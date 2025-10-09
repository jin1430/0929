package com.example.GoCafe.controller;

import com.example.GoCafe.domain.CafeStatus;
import com.example.GoCafe.dto.CafeCardForm;
import com.example.GoCafe.dto.CafeForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafePhoto;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.repository.CafeTagRepository;
import com.example.GoCafe.repository.ReviewTagRepository;
import com.example.GoCafe.service.*;
import com.example.GoCafe.support.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cafes")
public class CafeController {

    private final CafeService cafeService;
    private final MemberService memberService;
    private final ReviewService reviewService;
    private final CafeStatsService cafeStatsService;
    private final ReviewPhotoService reviewPhotoService;
    private final FavoriteService favoriteService;
    private final CafePhotoService cafePhotoService;
    private final CafeInfoService cafeInfoService;
    private final MenuService menuService;
    private final ReviewTagRepository reviewTagRepository;
    private final CafeTagRepository cafeTagRepository;

    /** 신규 등록 폼 */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/new")
    public String createCafeForm() {
        return "cafes/create";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String createCafe(Authentication auth,
                             @Valid @ModelAttribute("form") CafeForm form,
                             @RequestParam(value = "cafePhotoFile", required = false) MultipartFile cafePhotoFile,
                             @RequestParam(value = "bizDocFile", required = false) MultipartFile bizDocFile,
                             RedirectAttributes ra) {

        if (auth == null || !auth.isAuthenticated()) {
            ra.addFlashAttribute("msg", "로그인이 필요한 서비스입니다.");
            return "redirect:/login";
        }

        String email = auth.getName();
        Member me = memberService.findByEmail(email);
        if (me == null) {
            ra.addFlashAttribute("msg", "로그인 정보를 찾을 수 없습니다.");
            return "redirect:/login";
        }

        try {
            Long cafeId = cafeService.createCafe(me.getId(), form, cafePhotoFile, bizDocFile);
            ra.addFlashAttribute("msg", "카페가 등록되었습니다. (승인 대기 중)");
            return "redirect:/cafes/" + cafeId;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/cafes/new";
        }
    }

    /** 카페 상세 */
    @GetMapping("/{cafeId}")
    public String viewCafe(@PathVariable Long cafeId,
                           Authentication auth,
                           Model model,
                           RedirectAttributes ra) {

        // 0) 카페 존재 확인
        Cafe cafe = cafeService.findById(cafeId);
        if (cafe == null) throw new NotFoundException("카페가 없습니다.");

        // 1) 대표 사진
        CafePhoto mainPhoto = cafePhotoService.getMainPhoto(cafeId).orElse(null);

        // 2) 로그인 사용자 정보/권한
        String email = (auth != null ? auth.getName() : null);
        Long meId = null;
        boolean isAdmin = false;
        if (auth != null) {
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        if (email != null) {
            Member me = memberService.findByEmail(email);
            meId = (me != null ? me.getId() : null);
        }
        boolean isOwner = (meId != null && cafe.getOwner() != null
                && meId.equals(cafe.getOwner().getId()));

        // 3) 승인 전 접근 제한
        if (cafe.getCafeStatus() != CafeStatus.APPROVED && !(isOwner || isAdmin)) {
            if (cafe.getCafeStatus() == CafeStatus.PENDING) {
                ra.addFlashAttribute("flashInfo", "카페가 현재 검토 중입니다. 승인 후 열람할 수 있어요. (보통 수 분 소요)");
                return "redirect:/";
            } else {
                throw new NotFoundException("승인되지 않은 카페입니다.");
            }
        }

        // 4) 기본 모델
        model.addAttribute("cafe", cafe);
        model.addAttribute("mainPhoto", mainPhoto);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLoggedIn", auth != null && auth.isAuthenticated());

        // 4-1) 등록일 포맷 (템플릿에서 {{creationDateFmt}})
        try {
            var createdAt = cafe.getClass().getMethod("getCreationDate").invoke(cafe);
            if (createdAt instanceof LocalDate) {
                model.addAttribute("creationDateFmt",
                        ((LocalDate) createdAt).format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
            } else if (createdAt instanceof LocalDateTime) {
                model.addAttribute("creationDateFmt",
                        ((LocalDateTime) createdAt).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
            }
        } catch (ReflectiveOperationException ignored) {}

        // 5) CafeInfo 주입
        var cafeInfoOpt = cafeInfoService.findByCafeId(cafeId);
        if (cafeInfoOpt.isPresent()) {
            var ci = cafeInfoOpt.get();
            model.addAttribute("info", true);
            model.addAttribute("cafeOpenTime",  ci.getOpenTime());
            model.addAttribute("cafeCloseTime", ci.getCloseTime());
            model.addAttribute("cafeHoliday",   ci.getHoliday());
            model.addAttribute("cafeNotice",    ci.getNotice());
            model.addAttribute("cafeInfo",      ci.getInfo());
        } else {
            model.addAttribute("info", false);
        }

        // 6) 사진 갤러리
        var photoList = cafePhotoService.list(cafeId).stream()
                .map(p -> new LinkedHashMap<String, Object>() {{
                    put("cafePhotoUrl", p.getUrl());
                    put("limit4", false);
                }})
                .toList();
        for (int i = 0; i < Math.min(4, photoList.size()); i++) {
            photoList.get(i).put("limit4", true);
        }
        model.addAttribute("cafePhotos", photoList);

        // 7) 메뉴
        var menus = menuService.findByCafeId(cafeId);
        var nf = NumberFormat.getNumberInstance(Locale.KOREA);
        var menuVm = menus.stream().map(m -> {
            String photo = null;
            try {
                var mm = m.getClass().getMethod("getPhotoUrl");
                Object v = mm.invoke(m);
                if (v != null) photo = String.valueOf(v);
            } catch (ReflectiveOperationException ignored) {}
            if (photo == null) {
                try {
                    var mm = m.getClass().getMethod("getPhoto");
                    Object v = mm.invoke(m);
                    if (v != null) photo = String.valueOf(v);
                } catch (ReflectiveOperationException ignored) {}
            }

            String priceText;
            try {
                var pm = m.getClass().getMethod("getPrice");
                Object pv = pm.invoke(m);
                long price = (pv instanceof Number) ? ((Number) pv).longValue() : Long.parseLong(String.valueOf(pv));
                priceText = nf.format(price) + "원";
            } catch (Exception e) {
                priceText = "";
            }

            String name;
            try {
                var nm = m.getClass().getMethod("getName");
                Object nv = nm.invoke(m);
                name = (nv != null ? String.valueOf(nv) : "");
            } catch (Exception e) {
                name = "";
            }

            var map = new LinkedHashMap<String, Object>();
            map.put("menuName",  name);
            map.put("menuPrice", priceText);
            map.put("menuPhoto", (photo != null && !photo.isBlank()) ? photo : "/images/placeholder-cafe.jpg");
            return map;
        }).toList();
        model.addAttribute("menus", menuVm);

        // 8) 리뷰 목록 + 사진
        var reviews = reviewService.findByCafeIdWithMember(cafeId);
        var reviewVm = new ArrayList<Map<String, Object>>();
        for (var r : reviews) {
            var photos = reviewPhotoService.findByReviewIdOrderBySortIndexAsc(r.getId());
            String memberName = (r.getMember() != null ? r.getMember().getNickname() : "익명");
            String createdDate = "";
            try {
                var dt = r.getCreatedAt();
                if (dt != null) {
                    createdDate = (dt instanceof LocalDateTime)
                            ? ((LocalDateTime) dt).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                            : dt.toString();
                }
            } catch (Exception ignored) {}

            // 별점 문자열 계산
            String ratingStars = "";
            try {
                Number val = null;
                try { val = (Number) r.getClass().getMethod("getRating").invoke(r); } catch (Exception ignored) {}
                if (val == null) { try { val = (Number) r.getClass().getMethod("getTaste").invoke(r); } catch (Exception ignored) {} }
                if (val == null) { try { val = (Number) r.getClass().getMethod("getScore").invoke(r); } catch (Exception ignored) {} }
                if (val == null) { try { val = (Number) r.getClass().getMethod("getStars").invoke(r); } catch (Exception ignored) {} }
                if (val != null) {
                    int full = Math.max(0, Math.min(5, (int) Math.round(val.doubleValue())));
                    ratingStars = "★★★★★".substring(0, full);
                }
            } catch (Exception ignored) {}

            var map = new LinkedHashMap<String, Object>();
            map.put("id", r.getId());
            map.put("nickname", memberName);
            map.put("createdAt", createdDate);
            map.put("content", r.getContent());
            map.put("good", r.getGood());
            map.put("bad", r.getBad());
            map.put("ratingStars", ratingStars);
            map.put("photos", photos.stream().map(p -> Map.of("url", p.getUrl())).toList());
            reviewVm.add(map);
        }
        model.addAttribute("reviews", reviewVm);
        model.addAttribute("reviewsCount", reviewVm.size());

        // 9) 좋아요/아쉬워요 집계
        var stats = cafeStatsService.buildStats(cafeId, 12);
        model.addAttribute("cafeGood", stats.get("good"));
        model.addAttribute("cafeBad",  stats.get("bad"));

        // 10) 즐겨찾기
        boolean isFavorited = (email != null) && favoriteService.isFavoritedByEmail(email, cafeId);
        long favoriteCount = favoriteService.countFavoriteForCafe(cafeId);
        model.addAttribute("isFavorited", isFavorited);
        model.addAttribute("favoriteCount", favoriteCount);

        var rows = cafeTagRepository.findByCafeIdOrderByScoreDesc(cafeId);
        if (rows == null || rows.isEmpty()) {
            rows = cafeTagRepository.findTop4ByCafeIdOrderByScoreDesc(cafeId);
        }
        var cafeTags = rows.stream()
                .limit(4) // ✅ 실시간 정보 카드에 넣을 태그 개수를 4개로 제한
                .map(cafeTag -> Map.of(
                        "tagCode", cafeTag.getTagCode(),
                        "count",   (cafeTag.getRankNo() != null ? cafeTag.getRankNo().intValue()
                                : (int)Math.round(cafeTag.getScore() != null ? cafeTag.getScore() : 0.0))
                ))
                .toList();
        model.addAttribute("cafeTags", cafeTags);

        return "cafes/detail";
    }


    // ----- CafeInfo 저장 -----
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{cafeId}/info")
    public String upsertCafeInfo(@PathVariable Long cafeId,
                                 Authentication auth,
                                 @ModelAttribute com.example.GoCafe.dto.CafeInfoForm form,
                                 RedirectAttributes ra) {
        Cafe cafe = cafeService.findById(cafeId);

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isOwner = cafe.getOwner()!=null && auth.getName()!=null &&
                auth.getName().equalsIgnoreCase(cafe.getOwner().getEmail());
        if (!(isAdmin || isOwner)) {
            ra.addFlashAttribute("error", "점주 또는 관리자만 수정할 수 있습니다.");
            return "redirect:/cafes/" + cafeId;
        }

        var entity = form.toEntity();
        entity.setCafe(cafe);
        cafeInfoService.upsertByCafeId(cafeId, entity);

        ra.addFlashAttribute("msg", "영업 정보가 저장되었습니다.");
        return "redirect:/cafes/" + cafeId + "#info";
    }

    // ----- 목록 페이지 -----
    @GetMapping({"/find"})
    public String listCafesPage(@RequestParam(value = "region", required = false, defaultValue = "") String region,
                                @RequestParam(value = "sort",   required = false, defaultValue = "latest") String sort,
                                @RequestParam(value = "q",      required = false, defaultValue = "") String query,
                                @RequestParam(value = "tags",   required = false) String tagsCsv,
                                Authentication auth,
                                Model model) {

        // ================== [ 디버깅 로그 1: 입력값 확인 ] ==================
        System.out.println("\n[디버깅 1] === 요청 수신 ===");
        System.out.println("  > 검색어(q): " + query);
        System.out.println("  > 지역(region): " + region);

        model.addAttribute("isLoggedIn", (auth != null && auth.isAuthenticated()));
        model.addAttribute("initialRegion", region);
        model.addAttribute("initialSort",   sort);
        model.addAttribute("initialQuery",  query);

        Long meId = null;
        if (auth != null && auth.isAuthenticated()) {
            com.example.GoCafe.entity.Member me = memberService.findByEmail(auth.getName());
            if (me != null) meId = me.getId();
        }
        boolean isAdmin = (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN")));

        java.util.List<com.example.GoCafe.entity.Cafe> preload = cafeService.searchVisible(query, meId, isAdmin);
        // ================== [ 디버깅 로그 2: 초기 데이터 확인 ] ==================
        System.out.println("[디버깅 2] DB에서 가져온 초기 카페 개수: " + preload.size() + "개");


        if (!query.isBlank()) {
            preload = preload.stream()
                    .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(query.toLowerCase()))
                    .toList();
        }
        // ================== [ 디버깅 로그 3: 이름으로 필터링 후 확인 ] ==================
        System.out.println("[디버깅 3] '" + query + "' 이름으로 필터링 후 개수: " + preload.size() + "개");


        if (!region.isBlank()) {
            preload = preload.stream()
                    .filter(c -> {
                        String addr = c.getAddress() != null ? c.getAddress() : "";
                        String nm = c.getName() != null ? c.getName() : "";
                        return addr.contains(region) || nm.contains(region);
                    })
                    .toList();
        }
        // ================== [ 디버깅 로그 4: 지역으로 필터링 후 확인 ] ==================
        System.out.println("[디버깅 4] '" + region + "' 지역으로 필터링 후 개수: " + preload.size() + "개");


        if ("popular".equalsIgnoreCase(sort)) {
            preload = preload.stream()
                    .sorted(Comparator.comparing(com.example.GoCafe.entity.Cafe::getViews, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } else {
            preload = preload.stream()
                    .sorted(Comparator.comparing(com.example.GoCafe.entity.Cafe::getId, Comparator.reverseOrder()))
                    .toList();
        }

        if (!preload.isEmpty()) {
            Set<Long> cafeIds = preload.stream().map(com.example.GoCafe.entity.Cafe::getId).collect(Collectors.toSet());

            // 대표 먼저, 그 다음 sortIndex → 한 카페당 맨 앞 것이 대표가 되도록 가져오는 메서드여야 함
            List<com.example.GoCafe.entity.CafePhoto> mainPhotos =
                    cafePhotoService.findForCafeIdsOrderByMainThenSort(cafeIds);

            Map<Long, String> photoMap = mainPhotos.stream()
                    .collect(Collectors.toMap(
                            p -> p.getCafe().getId(),
                            com.example.GoCafe.entity.CafePhoto::getUrl,
                            (first, second) -> first   // 동일 cafe_id면 첫 번째(대표) 유지
                    ));

            List<CafeCardForm> initialCafeCards = preload.stream()
                    .map(c -> {
                        String url = photoMap.get(c.getId());
                        String safeUrl = cafePhotoService.normalizeUrl(url); // ← 수정
                        return new CafeCardForm(
                                c.getId(), c.getName(), c.getAddress(),
                                c.getPhoneNumber(), c.getBusinessCode(),
                                c.getViews(),
                                safeUrl
                        );
                    })
                    .toList();


            model.addAttribute("cafeCards", initialCafeCards);
        } else {
            model.addAttribute("cafeCards", java.util.Collections.emptyList());
        }
        // ================== [ 디버깅 로그 5: 최종 결과 확인 ] ==================
        System.out.println("[디버깅 5] 최종적으로 화면에 전달되는 카페 개수: " + ((java.util.List)model.getAttribute("cafeCards")).size() + "개");
        System.out.println("==================================================\n");


        java.util.List<String> initialTags = java.util.Collections.emptyList();
        if (tagsCsv != null && !tagsCsv.isBlank()) {
            initialTags = java.util.Arrays.stream(tagsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        model.addAttribute("initialTags", initialTags);

        return "cafes/find";
    }

}