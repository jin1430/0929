package com.example.GoCafe.service;

import com.example.GoCafe.client.HttpClient;
import com.example.GoCafe.dto.ReviewTagForm;
import com.example.GoCafe.entity.Menu;
import com.example.GoCafe.entity.MenuVector;
import com.example.GoCafe.entity.Review;
import com.example.GoCafe.entity.ReviewTag;
import com.example.GoCafe.event.ReviewChangedEvent;
import com.example.GoCafe.repository.MenuRepository;
import com.example.GoCafe.repository.MenuVectorRepository;
import com.example.GoCafe.repository.ReviewTagRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewTagService {

    private final ReviewTagRepository reviewTagRepository;
    private final MenuRepository menuRepository;
    private final MenuVectorRepository menuVectorRepository;
    private final ApplicationEventPublisher publisher;
    private final HttpClient httpClient;

    private static final String CATEGORY_MENU = "MENU";
    private static final int EXPECTED_DIM = 768;

    @Transactional(readOnly = true)
    public List<ReviewTag> findAll() {
        return reviewTagRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ReviewTag findById(Long id) {
        return reviewTagRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ReviewTag not found: " + id));
    }

    @Transactional
    public ReviewTag create(ReviewTag entity) {
        EntityIdUtil.setId(entity, null);
        return reviewTagRepository.save(entity);
    }

    @Transactional
    public ReviewTag update(Long id, ReviewTag entity) {
        if (!reviewTagRepository.existsById(id)) {
            throw new NotFoundException("ReviewTag not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return reviewTagRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!reviewTagRepository.existsById(id)) {
            throw new NotFoundException("ReviewTag not found: " + id);
        }
        reviewTagRepository.deleteById(id);
    }

    @Transactional
    public void upsertTags(Long reviewId, List<ReviewTag> tags) {
        // 기존 태그 정리 및 저장 로직
        // reviewTagRepo.deleteByReviewId(reviewId) 등 네가 쓰는 방식 유지
        reviewTagRepository.saveAll(tags);  // score 포함 저장
        Long cafeId = tags.isEmpty() ? null : tags.get(0).getReview().getCafe().getId();
        if (cafeId != null) publisher.publishEvent(new ReviewChangedEvent(cafeId));
    }

    @Transactional
    public void extractAndSaveMenuTags(Review review,
                                       Long cafeId) {
        if (review == null || review.getId() == null) {
            throw new NotFoundException("Review must be persisted before tagging.");
        }
        final String reviewContent = review.getContent();
        if (reviewContent == null || reviewContent.isBlank()) return;

        // ① cafeId 기준 menuMap 구성: { "메뉴명": [..768 floats..], ... }
        Map<String, List<Float>> menuMap = buildMenuMapForCafe(cafeId);

        // ② FastAPI 호출
        Map<String, Object> resp = httpClient.extractReviewTags(reviewContent, menuMap.isEmpty() ? null : menuMap, null);
        if (resp == null) return;

        Object itemsObj = resp.get("items");
        if (!(itemsObj instanceof List<?> items)) return;

        for (Object o : items) {
            if (!(o instanceof Map<?, ?> item)) continue;

            Object itemTextObj = item.get("itemText");
            if (!(itemTextObj instanceof String itemTextRaw)) continue;

            String tagCode = normalizeTag(itemTextRaw);
            if (tagCode.isBlank()) continue;

            boolean exists = reviewTagRepository.existsByReviewIdAndCode(review.getId(), tagCode);
            if (exists) continue;

            ReviewTag tag = new ReviewTag();
            tag.setReview(review);
            tag.setCategoryCode(CATEGORY_MENU);
            tag.setCode(tagCode);
            reviewTagRepository.save(tag);
        }
    }

    // cafeId의 메뉴들을 읽어 { 메뉴명 -> 임베딩 리스트 } 로 변환
    private Map<String, List<Float>> buildMenuMapForCafe(Long cafeId) {
        List<Menu> menus = menuRepository.findByCafeId(cafeId);
        if (menus.isEmpty()) return Collections.emptyMap();

        Map<Long, String> idToName = menus.stream()
                .collect(Collectors.toMap(Menu::getId, Menu::getName, (a, b) -> a, LinkedHashMap::new));

        List<Long> menuIds = new ArrayList<>(idToName.keySet());
        List<MenuVector> vectors = menuVectorRepository.findByMenuIdIn(menuIds);
        if (vectors.isEmpty()) return Collections.emptyMap();

        Map<String, List<Float>> menuMap = new LinkedHashMap<>();
        for (MenuVector mv : vectors) {
            String name = idToName.get(mv.getMenuId());
            if (name == null) continue;

            byte[] bytes = mv.getVector();
            if (bytes == null || bytes.length == 0) continue;

            List<Float> vec = bytesToFloatList(bytes);
            if (vec == null || vec.size() != EXPECTED_DIM) continue;

            // 동일 이름 충돌 시 첫 값 유지 (원하면 덮어쓰기 로직 변경 가능)
            menuMap.putIfAbsent(name, vec);
        }
        return menuMap;
    }

    // byte[] -> float 리스트 (리틀엔디안 32비트 float 가정)
    private List<Float> bytesToFloatList(byte[] bytes) {
        try {
            ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            int floatCount = bytes.length / 4;
            List<Float> out = new ArrayList<>(floatCount);
            for (int i = 0; i < floatCount; i++) out.add(bb.getFloat());
            return out;
        } catch (Throwable t) {
            return null; // 디코딩 실패 시 무시
        }
    }

    private String normalizeTag(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ");
    }

    @Transactional
    public void saveReviewTags(Review review, Collection<ReviewTagForm> pairs) {
        if (pairs == null || pairs.isEmpty()) return;

        for (ReviewTagForm p : pairs) {
            if (p == null) continue;
            String categoryCode = p.getCategoryCode();
            String tagCode      = p.getTagCode();
            if (categoryCode == null || categoryCode.isBlank() ||
                    tagCode == null || tagCode.isBlank()) {
                continue;
            }
            // 중복 방지(uk: review_id + tag_code)
            boolean exists = reviewTagRepository.existsByReviewIdAndCode(review.getId(), tagCode);
            if (!exists) {
                ReviewTag rt = new ReviewTag();
                rt.setReview(review);
                rt.setCategoryCode(categoryCode);
                rt.setCode(tagCode);
                reviewTagRepository.save(rt);
            }
        }
    }
}