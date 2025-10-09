// src/main/java/com/example/GoCafe/controller/RecommendController.java
package com.example.GoCafe.controller;

import com.example.GoCafe.dto.CafeCardForm;
import com.example.GoCafe.dto.CafeRecommendDto;
import com.example.GoCafe.dto.QuestionsForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Member;
import com.example.GoCafe.service.CafeService;
import com.example.GoCafe.service.MemberService;
import com.example.GoCafe.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/recommend")
public class RecommendController {

    private final CafeService cafeService;
    private final RecommendService recommendService;
    private final MemberService memberService;

    // 성별에 따른 카페 추천
    @GetMapping("/gender")
    public String recommendByGender(
            @RequestParam(value = "region", required = false, defaultValue = "") String region,
            @RequestParam(value = "q",      required = false, defaultValue = "") String query,
            Authentication auth,
            Model model
    ) {
        // 로그인 유저 성별/ID
        String gender = null;
        Long meId = null;
        boolean isAdmin = (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN")));
        if (auth != null && auth.isAuthenticated()) {
            Member me = memberService.findByEmail(auth.getName());
            if (me != null) { meId = me.getId(); gender = me.getGender(); }
        }

        // 1) 후보 카페(가시성/검색/지역 필터)
        List<Cafe> candidates = cafeService.searchVisible(query, meId, isAdmin);
        if (!region.isBlank()) {
            candidates = candidates.stream()
                    .filter(c -> {
                        String addr = c.getAddress() != null ? c.getAddress() : "";
                        String nm = c.getName() != null ? c.getName() : "";
                        return addr.contains(region) || nm.contains(region);
                    })
                    .toList();
        }

        // 2) 성별 + UserNeeds × CSV 가중치
        List<CafeRecommendDto> recs = recommendService.recommendByGender(gender, meId, candidates);

        // 3) 카드 뷰 매핑
        List<CafeCardForm> cards = recs.stream()
                .map(r -> new CafeCardForm(
                        r.getCafeId(),
                        r.getName(),
                        r.getAddress(),
                        null,
                        null,
                        r.getViews(),
                        r.getPhotoUrl()
                ))
                .toList();

        model.addAttribute("isLoggedIn", (auth != null && auth.isAuthenticated()));
        model.addAttribute("initialRegion", region);
        model.addAttribute("initialSort",   "genderScore");
        model.addAttribute("initialQuery",  query);
        model.addAttribute("cafeCards",     cards);
        model.addAttribute("recommendedBy", "성별 기반의 카페 추천");
        model.addAttribute("hotSubtitle",   "당신의 성별과 취향(UserNeeds)을 반영해 골라봤어요!");

        return "cafes/find";
    }

    // 연령대 카페 추천
    @GetMapping("/age")
    public String recommendByAgePage(
            @RequestParam(value = "region", required = false, defaultValue = "") String region,
            @RequestParam(value = "q",      required = false, defaultValue = "") String query,
            Authentication auth,
            Model model
    ) {
        // 로그인 유저의 나이/ID
        Long meId = null;
        Long age  = null;
        boolean isAdmin = (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN")));
        if (auth != null && auth.isAuthenticated()) {
            Member me = memberService.findByEmail(auth.getName());
            if (me != null) { meId = me.getId(); age = me.getAge(); }
        }

        // 1) 후보 카페(가시성/검색/지역 필터)
        List<Cafe> candidates = cafeService.searchVisible(query, meId, isAdmin);
        if (!region.isBlank()) {
            candidates = candidates.stream()
                    .filter(c -> {
                        String addr = c.getAddress() != null ? c.getAddress() : "";
                        String nm = c.getName() != null ? c.getName() : "";
                        return addr.contains(region) || nm.contains(region);
                    })
                    .toList();
        }

        // 2) 연령 + UserNeeds × CSV 가중치
        List<CafeRecommendDto> recs = recommendService.recommendByAge(age, meId, candidates);

        // 3) 카드 뷰 매핑
        List<CafeCardForm> cards = recs.stream()
                .map(r -> new CafeCardForm(
                        r.getCafeId(),
                        r.getName(),
                        r.getAddress(),
                        null,
                        null,
                        r.getViews(),
                        r.getPhotoUrl()
                ))
                .toList();

        model.addAttribute("isLoggedIn", (auth != null && auth.isAuthenticated()));
        model.addAttribute("initialRegion", region);
        model.addAttribute("initialSort",   "ageScore");
        model.addAttribute("initialQuery",  query);
        model.addAttribute("cafeCards",     cards);
        model.addAttribute("recommendedBy", "연령대 기반의 카페 추천");
        model.addAttribute("hotSubtitle",   "당신의 연령대 취향과 UserNeeds를 반영해 골라봤어요!");

        return "cafes/find";
    }

    @GetMapping("/questions")
    public String recommengByQuestions() {
        return "cafes/recommendQuestions";
    }

    @PostMapping(value = "/questions/submit", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String submit(QuestionsForm form, RedirectAttributes ra) {
        ra.addFlashAttribute("questions", form);
        return "redirect:/recommend/questions/result?sort=recommend";
    }

    @GetMapping("/questions/result")
    public String questionsResult(@RequestParam(value = "region", required = false, defaultValue = "") String region,
                                  @RequestParam(value = "sort",   required = false, defaultValue = "latest") String sort,
                                  @RequestParam(value = "q",      required = false, defaultValue = "") String query,
                                  @RequestParam(value = "tags",   required = false) String tagsCsv,
                                  @ModelAttribute("questions") QuestionsForm questions,
                                  Authentication auth,
                                  Model model) {

        if (isBlank(questions.getPurpose())
                || isBlank(questions.getVibe())
                || isBlank(questions.getFactor())
                || isBlank(questions.getTime())
                || isBlank(questions.getArea())) {
            return "redirect:/cafes/ask";
        }

        boolean loggedIn = (auth != null && auth.isAuthenticated());
        boolean isAdmin  = (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN")));
        Long meId = null;
        if (loggedIn) {
            Member me = memberService.findByEmail(auth.getName());
            if (me != null) meId = me.getId();
        }

        model.addAttribute("isLoggedIn", loggedIn);
        model.addAttribute("initialRegion", region);
        model.addAttribute("initialSort",   sort);
        model.addAttribute("initialQuery",  query);

        List<Cafe> candidates = cafeService.searchVisible(query, meId, isAdmin);
        if (!region.isBlank()) {
            candidates = candidates.stream()
                    .filter(c -> {
                        String addr = c.getAddress() != null ? c.getAddress() : "";
                        String nm   = c.getName() != null ? c.getName() : "";
                        return addr.contains(region) || nm.contains(region);
                    })
                    .toList();
        }

        var recs = recommendService.recommendByQuestions(questions, candidates);

        var cards = recs.stream()
                .map(r -> new CafeCardForm(
                        r.getCafeId(),
                        r.getName(),
                        r.getAddress(),
                        null,
                        null,
                        r.getViews(),
                        r.getPhotoUrl()
                ))
                .toList();

        var pr = recommendService.resolveProfileByQuestions(questions);
        model.addAttribute("profileTitle",       pr.title);
        model.addAttribute("profileDescription", pr.description);
        model.addAttribute("profileWeights",     pr.weights);
        model.addAttribute("questionsProfile",   pr);

        model.addAttribute("cafeCards",     cards);
        model.addAttribute("recommendedBy", "질문 기반의 결과 추천");
        model.addAttribute("hotSubtitle",   "당신의 답변에 딱 맞는 카페를 골라봤어요!");
        model.addAttribute("myTitle",       pr.title);
        model.addAttribute("myDescription", pr.description);
        model.addAttribute("didAnswer", true);

        return "cafes/find";
    }

    private boolean isBlank(String s){ return s == null || s.isBlank(); }

    @GetMapping("/needs")
    public String recommendByBaseNeeds(
            @RequestParam(value = "region", required = false, defaultValue = "") String region,
            @RequestParam(value = "q",      required = false, defaultValue = "") String query,
            Authentication auth,
            Model model
    ) {
        Long meId = null;
        boolean isAdmin = (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN")));
        if (auth != null && auth.isAuthenticated()) {
            Member me = memberService.findByEmail(auth.getName());
            if (me != null) meId = me.getId();
        }

        // 후보 수집(+지역 필터)
        List<Cafe> candidates = cafeService.searchVisible(query, meId, isAdmin);
        if (!region.isBlank()) {
            candidates = candidates.stream()
                    .filter(c -> {
                        String addr = c.getAddress() != null ? c.getAddress() : "";
                        String nm = c.getName() != null ? c.getName() : "";
                        return addr.contains(region) || nm.contains(region);
                    })
                    .toList();
        }

        // ★ 변경: UserNeeds만으로 합산
        List<CafeRecommendDto> recs = recommendService.recommendByBaseNeeds(meId, candidates);

        // 카드 매핑
        List<CafeCardForm> cards = recs.stream()
                .map(r -> new CafeCardForm(
                        r.getCafeId(),
                        r.getName(),
                        r.getAddress(),
                        null,
                        null,
                        r.getViews(),
                        r.getPhotoUrl()
                ))
                .toList();

        model.addAttribute("isLoggedIn", (auth != null && auth.isAuthenticated()));
        model.addAttribute("initialRegion", region);
        model.addAttribute("initialSort",   "needsScore");
        model.addAttribute("initialQuery",  query);
        model.addAttribute("cafeCards",     cards);
        model.addAttribute("recommendedBy", "내 취향(UserNeeds) 기반 추천");
        model.addAttribute("hotSubtitle",   "내가 고른 태그의 가중치 합(예: 모던 0.9 + 콜드브루 1.2 = 2.1)으로 정렬했어요!");

        return "cafes/find";
    }
}
