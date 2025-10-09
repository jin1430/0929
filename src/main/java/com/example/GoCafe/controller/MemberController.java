package com.example.GoCafe.controller;

import com.example.GoCafe.dto.MemberForm;
import com.example.GoCafe.dto.MyReviewItem;
import com.example.GoCafe.dto.NeedSelection;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.entity.Review;
import com.example.GoCafe.repository.CafePhotoRepository;
import com.example.GoCafe.repository.UserNeedsRepository;
import com.example.GoCafe.service.FavoriteService;
import com.example.GoCafe.service.MemberService;
import com.example.GoCafe.service.ReviewService;
import com.example.GoCafe.service.UserNeedsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;
    private final CafePhotoRepository cafePhotoRepository;
    private final UserNeedsService userNeedsService;

    /* =========================
     *        MY PAGE
     * ========================= */
    @GetMapping("/me")
    public String myPage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                         Model model) {
        if (principal == null) return "redirect:/login";

        Member member = memberService.findByEmail(principal.getUsername());
        if (member == null) return "redirect:/login";

        MemberForm view = toForm(member);

        // 공통 헤더/레이아웃에서 쓰는 값 + PRO 여부
        populateCommonUserModel(model, member);

        // 진행도(없어도 항상 키 존재하게 세팅)
        Map<String, Object> proProgress = buildProProgress(member.getId(), isPro(member));
        model.addAttribute("proProgress", proProgress);

        // 본문 모델
        model.addAttribute("member", view);
        model.addAttribute("memberPhoto", member.getPhoto() == null ? "" : member.getPhoto());

        // 좌측 네비 활성화
        model.addAttribute("nav_me", true);

        return "member/mypage";
    }

    /* =========================
     *     PROFILE EDIT (GET)
     * ========================= */
    @GetMapping("/edit")
    public String edit(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                       Model model) {
        if (principal == null) return "redirect:/login";

        Member member = memberService.findByEmail(principal.getUsername());
        if (member == null) return "redirect:/login";

        MemberForm view = toForm(member);

        // 공통 키
        populateCommonUserModel(model, member);

        // 사진 파생 값
        String photoName = member.getPhoto();
        boolean hasPhoto = photoName != null && !photoName.isBlank();
        model.addAttribute("photoName", hasPhoto ? photoName : "");
        model.addAttribute("photoUrl", hasPhoto ? "/images/profile/" + photoName : "");
        model.addAttribute("hasPhoto", hasPhoto);

        // 본문 모델
        model.addAttribute("member", view);

        // 네비
        model.addAttribute("nav_edit", true);

        return "member/edit";
    }

    /* =========================
     *     PROFILE EDIT (POST)
     * ========================= */
    @PostMapping("/edit")
    public String editDo(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                         @RequestParam(value = "member_nickname", required = false) String nickname,
                         @RequestParam(value = "member_age", required = false) Long age,
                         @RequestParam(value = "member_gender", required = false) String gender,
                         @RequestParam(value = "member_photo", required = false) String photo,
                         @RequestParam(value = "current_password", required = false) String currentPassword,
                         @RequestParam(value = "new_password", required = false) String newPassword,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        boolean pwChanged = memberService.updateSelf(
                me.getId(), nickname, age, gender, photo, currentPassword, newPassword
        );

        if (pwChanged) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            new SecurityContextLogoutHandler().logout(request, response, auth);
            return "redirect:/login?password=changed";
        }
        return "redirect:/member/me?update=success";
    }

    /* =========================
     *       WITHDRAW (GET)
     * ========================= */
    @GetMapping("/withdraw")
    public String withdrawPage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                               Model model) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        // 공통 키
        populateCommonUserModel(model, me);

        // 점표기 회피용 단일 키
        model.addAttribute("email", me.getEmail());
        model.addAttribute("nickname", me.getNickname());

        // 네비
        model.addAttribute("nav_withdraw", true);

        return "member/withdraw";
    }

    /* =========================
     *       WITHDRAW (POST)
     * ========================= */
    @PostMapping("/withdraw")
    public String withdrawDo(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        memberService.withdrawSelf(me.getId());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, auth);

        return "redirect:/?withdraw=success";
    }

    /* =========================
     *        MY REVIEWS
     * ========================= */
    // src/main/java/com/example/GoCafe/controller/MemberController.java

    @GetMapping("/reviews")
    public String myReviews(@AuthenticationPrincipal User principal,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            Model model) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        Page<MyReviewItem> p = reviewService.findMyReviews(me.getId(), PageRequest.of(page, 10));

        // 공통 키
        populateCommonUserModel(model, me);

        // === 뷰 모델로 변환 (필드명 보정 + 포맷) ===
        List<Map<String, Object>> items = new ArrayList<>();
        for (MyReviewItem it : p.getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();

            // cafeId
            m.put("cafeId", invokeAny(it, Long.class, "getCafeId", "cafeId"));

            // cafeName
            Object cafeName = invokeAny(it, Object.class, "getCafeName", "cafeName");
            m.put("cafeName", cafeName != null ? cafeName.toString() : "(알 수 없음)");

            // content
            Object content = invokeAny(it, Object.class, "getContent", "getReviewContent", "content", "reviewContent", "getText");
            m.put("content", content != null ? content.toString() : "");

            // createdAt (문자열로 포맷)
            Object created = invokeAny(it, Object.class, "getCreatedAt", "createdAt", "getWrittenAt", "writtenAt", "getCreatedDate");
            m.put("createdAt", formatDateTime(created)); // "yyyy.MM.dd HH:mm"

            // good / bad (없으면 0)
            Number good = (Number) invokeAny(it, Number.class, "getGood", "good");
            Number bad  = (Number) invokeAny(it, Number.class, "getBad",  "bad");
            m.put("good", good == null ? 0 : good.intValue());
            m.put("bad",  bad  == null ? 0 : bad.intValue());

            items.add(m);
        }

        model.addAttribute("items", items);
        model.addAttribute("hasContent", !items.isEmpty());
        model.addAttribute("totalElements", p.getTotalElements());

        // 페이지네이션
        int current = p.getNumber();
        int totalPages = Math.max(p.getTotalPages(), 1);
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);

        List<Map<String, Object>> pages = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("index", i);
            m.put("display", i + 1);
            m.put("active", i == current);
            pages.add(m);
        }

        model.addAttribute("pages", pages);
        model.addAttribute("hasPrev", p.hasPrevious());
        model.addAttribute("hasNext", p.hasNext());
        model.addAttribute("prevPage", Math.max(0, current - 1));
        model.addAttribute("nextPage", Math.min(totalPages - 1, current + 1));

        // 좌측 네비 활성화
        model.addAttribute("nav_reviews", true);

        return "member/reviews";
    }

    /* ===== 아래 헬퍼 두 개를 같은 클래스 안에 추가 ===== */

    private static Object invokeAny(Object target, Class<?> expectType, String... methods) {
        for (String m : methods) {
            try {
                var mm = target.getClass().getMethod(m.replaceFirst("^get", "get"));
                Object v = mm.invoke(target);
                if (v != null && (expectType == null || expectType.isInstance(v) || expectType == Object.class)) return v;
            } catch (Exception ignored) {}
            try {
                var f = target.getClass().getDeclaredField(m);
                f.setAccessible(true);
                Object v = f.get(target);
                if (v != null && (expectType == null || expectType.isInstance(v) || expectType == Object.class)) return v;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String formatDateTime(Object v) {
        if (v == null) return "";
        try {
            // LocalDateTime
            if (v instanceof java.time.LocalDateTime ldt) {
                return ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
            }
            // java.util.Date
            if (v instanceof java.util.Date d) {
                return new java.text.SimpleDateFormat("yyyy.MM.dd HH:mm").format(d);
            }
            // 문자열이면 그대로
            return v.toString();
        } catch (Exception e) {
            return v.toString();
        }
    }

    /* =========================
     *         HELPERS
     * ========================= */

    private MemberForm toForm(Member member) {
        MemberForm view = new MemberForm();
        view.setId(member.getId());
        view.setEmail(member.getEmail());
        view.setNickname(member.getNickname());
        view.setAge(member.getAge());
        view.setGender(member.getGender());
        view.setRoleKind(String.valueOf(member.getRoleKind()));
        view.setCreatedAt(member.getCreatedAt());
        view.setPhoto(member.getPhoto());
        view.setTokenVersion(member.getTokenVersion());
        return view;
    }

    /** 헤더/레이아웃에서 공통으로 쓰는 키들을 항상 채워준다. */
    private void populateCommonUserModel(Model model, Member member) {
        boolean isPro = isPro(member);
        String photoName = member.getPhoto();
        boolean hasPhoto = photoName != null && !photoName.isBlank();

        model.addAttribute("isLoggedIn", true);
        model.addAttribute("memberNickname", member.getNickname());   // ✅ 헤더가 참조
        model.addAttribute("currentUserNickname", member.getNickname());
        model.addAttribute("isPro", isPro);

        // 사진 관련 키(헤더/페이지 어디서든 사용 가능)
        model.addAttribute("hasPhoto", hasPhoto);
        model.addAttribute("photoName", hasPhoto ? photoName : "");
        model.addAttribute("photoUrl", hasPhoto ? "/images/profile/" + photoName : "");
        model.addAttribute("memberPhoto", hasPhoto ? photoName : "");
    }

    private boolean isPro(Member m) {
        return m != null && "PRO".equalsIgnoreCase(String.valueOf(m.getRoleKind()));
    }

    /**
     * PRO가 아니더라도 템플릿에서 항상 사용할 수 있도록
     * total/good/percent 키를 포함한 맵을 돌려줍니다.
     */
    private Map<String, Object> buildProProgress(Long memberId, boolean isPro) {
        final int TOTAL_TARGET = 10;
        final int GOOD_TARGET  = 6;

        long totalReviews = 0L;
        long goodReviews  = 0L;

        try {
            Page<MyReviewItem> p = reviewService.findMyReviews(memberId, PageRequest.of(0, 1));
            totalReviews = (p != null) ? p.getTotalElements() : 0L;
        } catch (Throwable ignore) {}

        try {
            List<Review> all = reviewService.findAll();
            goodReviews = all.stream()
                    .filter(r -> r.getMember() != null && Objects.equals(r.getMember().getId(), memberId))
                    .filter(r -> "GOOD".equalsIgnoreCase(String.valueOf(r.getSentiment())))
                    .count();
        } catch (Throwable ignore) {}

        int pctTotal = (int) Math.min(100, Math.round((totalReviews * 100.0) / TOTAL_TARGET));
        int pctGood  = (int) Math.min(100, Math.round((goodReviews  * 100.0) / GOOD_TARGET));
        int percent  = Math.min(pctTotal, pctGood);
        if (isPro) percent = 100;

        Map<String, Object> map = new HashMap<>();
        map.put("totalReviews", totalReviews);
        map.put("goodReviews",  goodReviews);
        map.put("percent",      percent);
        return map;
    }
    /* =========================
     *      MY FAVORITES (GET)
     * ========================= */
    @GetMapping("/favorites")
    public String favoritesPage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "12") int size,
                                Model model) {
        if (principal == null) return "redirect:/login";

        // 로그인 사용자 조회
        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        // 헤더/레이아웃 공통 키
        populateCommonUserModel(model, me);

        // 사용자의 즐겨찾기 카페 페이징 조회
        Page<com.example.GoCafe.entity.Cafe> favPage =
                favoriteService.listMyFavorites(me.getId(), PageRequest.of(page, size));

        var cafes = favPage.getContent();

        // (1) 카페별 대표 사진을 한 번에 모아서 조회
        List<Long> cafeIds = cafes.stream().map(c -> c.getId()).toList();

        Map<Long, String> thumbByCafeId = new HashMap<>();
        if (!cafeIds.isEmpty()) {
            var photos = cafePhotoRepository.findByCafe_IdIn(cafeIds); // List<CafePhoto>

            // isMain=true 우선 등록
            photos.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsMain()))
                    .forEach(p -> thumbByCafeId.put(p.getCafe().getId(), p.getUrl()));

            // 대표가 비어있는 카페는 첫 사진으로 보충
            photos.forEach(p -> thumbByCafeId.putIfAbsent(p.getCafe().getId(), p.getUrl()));
        }

        // (2) Mustache에서 쓰는 키(id, name, address, photo)로 매핑
        List<Map<String, Object>> favorites = new ArrayList<>();
        for (var cafe : cafes) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", cafe.getId());
            m.put("name", cafe.getName());           // 엔티티 필드명에 맞게
            m.put("address", cafe.getAddress());     // 엔티티 필드명에 맞게
            m.put("photo", thumbByCafeId.getOrDefault(cafe.getId(), "")); // 없으면 빈 문자열
            favorites.add(m);
        }

        // (3) 모델 바인딩
        model.addAttribute("favorites", favorites);
        model.addAttribute("hasFavorites", !favorites.isEmpty()); // 필요시 템플릿에서 사용

        // (4) 페이지네이션 키
        int current = favPage.getNumber();
        int totalPages = Math.max(favPage.getTotalPages(), 1);
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);

        List<Map<String, Object>> pages = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("index", i);
            m.put("display", i + 1);
            m.put("active", i == current);
            pages.add(m);
        }
        model.addAttribute("pages", pages);
        model.addAttribute("hasPrev", favPage.hasPrevious());
        model.addAttribute("hasNext", favPage.hasNext());
        model.addAttribute("prevPage", Math.max(0, current - 1));
        model.addAttribute("nextPage", Math.min(totalPages - 1, current + 1));
        model.addAttribute("pageSize", size);

        // 좌측 네비 활성화
        model.addAttribute("nav_favorites", true);

        // 뷰
        return "member/favorites";
    }

    @GetMapping("/needs")
    public String needsPage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                            Model model) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        // ✅ 헤더/레이아웃 공통 키
        populateCommonUserModel(model, me);

        // ✅ 사이드 내비 활성화(템플릿 키와 일치)
        model.addAttribute("nav_prefs", true);

        // ✅ 저장된 니즈 로드해서 바인딩
        //    UserNeedsService에 listSelections(memberId) 같은 조회 메서드가 있다고 가정
        List<NeedSelection> selections = userNeedsService.listSelections(me.getId());
        boolean has = selections != null && !selections.isEmpty();
        model.addAttribute("hasProfile", has);

        if (has) {
            // 상단 요약영역(자유롭게 문구 조정 가능)
            model.addAttribute("myTitle", "내가 선택한 니즈");
            model.addAttribute("myDescription", "선택한 태그를 기반으로 추천 정확도를 높였어요.");


            // selections: List<NeedSelection>
            List<Map<String, Object>> needs = selections.stream()
                    .map(s -> {
                        Map<String, Object> m = new java.util.LinkedHashMap<>();
                        m.put("category", s.getCategoryCode()); // "VIBE", "DRINK" ...
                        m.put("tag",      s.getTagName());      // "아늑함", "루프탑" ...
                        m.put("weight",   s.getWeight());       // 1.0
                        return m;
                    })
                    .toList();

            model.addAttribute("needs", needs);
        }

        return "member/needs";  // => templates/member/needs.mustache
    }

    /* =========================
     *  NEEDS SELECT (GET) - 페이지 이동
     * ========================= */
    @GetMapping({"/select/needs", "/select/needs/"})
    public String needsSelectPage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                                  Model model) {
        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        // 공통 헤더용 최소한만
        populateCommonUserModel(model, me);
        model.addAttribute("nav_prefs", true);

        // 카테고리/태그 바인딩 없음 (페이지에서 자체 구현)
        return "member/selectNeeds"; // templates/member/needs-select.mustache
    }

    @PostMapping("/select/needs")
    public String saveNeedsSelections(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam(value = "tags", required = false) List<String> tags,
            Model model) {

        if (principal == null) return "redirect:/login";

        Member me = memberService.findByEmail(principal.getUsername());
        if (me == null) return "redirect:/login";

        populateCommonUserModel(model, me);

        Map<String, NeedSelection> dedup = new LinkedHashMap<>();
        if (tags != null) {
            for (String raw : tags) {
                if (raw == null || raw.isBlank()) continue;
                String[] parts = raw.split("\\|", 2);
                if (parts.length < 2) continue;

                String code = parts[0].trim();
                String name = parts[1].trim();
                if (code.isEmpty() || name.isEmpty()) continue;

                String key = (code + "|" + name).toUpperCase();
                dedup.put(key, new NeedSelection(code, name, 1.0));
            }
        }

        if (dedup.isEmpty()) {
            userNeedsService.clearForMember(me.getId());
        } else {
            userNeedsService.replaceSelections(me.getId(), new ArrayList<>(dedup.values()));
        }
        return "redirect:/member/needs?save=success";
    }

}
