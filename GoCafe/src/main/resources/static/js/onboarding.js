// /static/js/onboarding.js
(function () {
  /** ===============================
   * 온보딩 자동 노출 정책
   * -------------------------------
   * AUTO_OPEN_POLICY:
   *  - "respectDone": 완료자( localStorage.onboardingDone === 'true' )는 자동 오픈 안 함 (기본)
   *  - "always": 페이지 진입 시 항상 자동 오픈
   *  - "oncePerVisit": 이번 방문에서 한 번만 자동 오픈(세션 기준), 완료자는 제외
   * 강제 오픈: URL에 ?onboard=1 이면 정책/완료 여부 무시하고 즉시 오픈
   * =============================== */
  const AUTO_OPEN_POLICY = "respectDone"; // 필요 시 "always" 또는 "oncePerVisit" 로 변경

  // 상태
  let currentOnboardingStep = 1;
  let onboardingAnswers = {};

  // DOM
  const modal   = document.getElementById('onboardingModal');
  const openBtn = document.getElementById('openOnboarding'); // 있을 수도, 없을 수도

  // 유틸
  const hasModal = !!modal;
  function isDone() {
    return localStorage.getItem('onboardingDone') === 'true';
  }
  function setVisitShown() {
    sessionStorage.setItem('onboardingShownThisVisit', '1');
  }
  function wasVisitShown() {
    return sessionStorage.getItem('onboardingShownThisVisit') === '1';
  }

  // =======================
  // 자동 오픈 트리거
  // =======================
  (function autoOpenOnLoad() {
    if (!hasModal) return;

    const params = new URLSearchParams(location.search);
    const forceParam = params.get('onboard') === '1';

    if (forceParam) {
      // 강제 오픈(완료자도 무시)
      setTimeout(() => openOnboarding(), 0);
      return;
    }

    // 정책별 동작
    if (AUTO_OPEN_POLICY === 'always') {
      setTimeout(() => openOnboarding(), 0);
      return;
    }

    if (AUTO_OPEN_POLICY === 'oncePerVisit') {
      if (!wasVisitShown() && !isDone()) {
        setVisitShown();
        setTimeout(() => openOnboarding(), 0);
      }
      return;
    }

    // default: "respectDone" (완료자는 자동 오픈 X)
    if (!isDone()) {
      setTimeout(() => openOnboarding(), 0);
    }
  })();

  // =======================
  // (선택) 버튼 트리거
  // 헤더 등에 id="openOnboarding" 있으면 클릭으로도 열 수 있음
  // =======================
  if (openBtn) {
    openBtn.addEventListener('click', function (e) {
      // 링크 이동은 그대로 유지하고, 강제 오픈 파라미터를 붙이고 싶다면 주석 해제
      // e.preventDefault();
      // const u = new URL(openBtn.href, location.origin);
      // u.searchParams.set('onboard', '1');
      // location.href = u.toString();

      // 현재 페이지에서 즉시 오픈시키고 싶다면 아래 한 줄 사용
      e.preventDefault();
      openOnboarding();
    });
  }

  // =======================
  // 모달 열기/닫기
  // =======================
  function openOnboarding() {
    if (!hasModal) return;

    // 초기화
    currentOnboardingStep = 1;
    onboardingAnswers = {};

    // 스텝 표시
    document.querySelectorAll('.onboarding-step').forEach(s => s.classList.remove('active'));
    document.getElementById('step-1')?.classList.add('active');

    updateStepIndicator();
    updateNavigationButtons();

    modal.classList.add('active');
    modal.setAttribute('aria-hidden', 'false');
  }

  function closeModal() {
    if (!hasModal) return;
    modal.classList.remove('active');
    modal.setAttribute('aria-hidden', 'true');
  }

  // =======================
  // 스텝 이동
  // =======================
  window.nextStep = function () {
    const currentStep = document.getElementById(`step-${currentOnboardingStep}`);
    const selectedOption = currentStep?.querySelector('.onboarding-option.selected');
    if (!selectedOption) return;

    onboardingAnswers[`step${currentOnboardingStep}`] = selectedOption.dataset.value;

    currentStep.classList.remove('active');
    currentOnboardingStep++;

    if (currentOnboardingStep <= 5) {
      document.getElementById(`step-${currentOnboardingStep}`)?.classList.add('active');
      updateStepIndicator();
      updateNavigationButtons();
    } else {
      analyzeUserPreferences();
      document.getElementById('step-result')?.classList.add('active');
      updateNavigationButtons();
    }
  };

  window.prevStep = function () {
    if (currentOnboardingStep <= 1) return;
    document.getElementById(`step-${currentOnboardingStep}`)?.classList.remove('active');
    currentOnboardingStep--;
    document.getElementById(`step-${currentOnboardingStep}`)?.classList.add('active');
    updateStepIndicator();
    updateNavigationButtons();
  };

  // =======================
  // UI 업데이트
  // =======================
  function updateStepIndicator() {
    const indicators = document.querySelectorAll('.step-indicator');
    indicators.forEach((el, idx) => {
      if (idx < currentOnboardingStep) el.classList.add('active');
      else el.classList.remove('active');
    });
    const counter = document.getElementById('step-counter');
    if (counter) counter.textContent = `${Math.min(currentOnboardingStep, 5)} / 5`;
  }

  function updateNavigationButtons() {
    const prevBtn = document.getElementById('prev-btn');
    const nextBtn = document.getElementById('next-btn');
    const completeBtn = document.getElementById('complete-btn');
    const skipBtn = document.getElementById('skip-btn');

    if (currentOnboardingStep > 1 && currentOnboardingStep <= 5) prevBtn?.classList.remove('hidden');
    else prevBtn?.classList.add('hidden');

    if (currentOnboardingStep <= 5) {
      nextBtn?.classList.remove('hidden');
      completeBtn?.classList.add('hidden');
      skipBtn?.classList.remove('hidden');
      const currentStep = document.getElementById(`step-${currentOnboardingStep}`);
      const selectedOption = currentStep?.querySelector('.onboarding-option.selected');
      if (selectedOption) {
        if (nextBtn) {
          nextBtn.disabled = false;
          nextBtn.classList.remove('bg-gray-300', 'text-gray-500', 'cursor-not-allowed');
          nextBtn.classList.add('btn-primary', 'text-white');
        }
      } else {
        if (nextBtn) {
          nextBtn.disabled = true;
          nextBtn.classList.add('bg-gray-300', 'text-gray-500', 'cursor-not-allowed');
          nextBtn.classList.remove('btn-primary', 'text-white');
        }
      }
    } else {
      nextBtn?.classList.add('hidden');
      completeBtn?.classList.remove('hidden');
      skipBtn?.classList.add('hidden');
    }
  }

  // =======================
  // 옵션 선택
  // =======================
  document.addEventListener('click', function (e) {
    const opt = e.target.closest('.onboarding-option');
    if (!opt) return;
    const step = opt.closest('.onboarding-step');
    step?.querySelectorAll('.onboarding-option').forEach(o => o.classList.remove('selected'));
    opt.classList.add('selected');
    updateNavigationButtons();
  });

  // 배경 클릭/ESC 닫기
  document.addEventListener('click', function (e) {
    if (e.target === modal) closeModal();
  });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && modal?.classList.contains('active')) closeModal();
  });

  // =======================
  // 분석/결과
  // =======================
  function analyzeUserPreferences() {
    const { step1, step2, step3 } = onboardingAnswers;
    let userType = '힐링 추구자';
    let description = '혼자만의 여유로운 시간을 즐기며, 편안한 분위기를 선호하는 타입입니다.';
    let recommendedTags = ['조용한', '뷰맛집', '디저트', '아늑한'];

    if (step1 === 'study') {
      userType = '카페 스터디족';
      description = '조용하고 집중할 수 있는 환경을 선호하며, 커피 품질을 중시하는 타입입니다.';
      recommendedTags = ['조용한', '스터디', 'WiFi', '넓은'];
    } else if (step1 === 'date') {
      userType = '로맨틱 데이터';
      description = '분위기 좋고 예쁜 카페를 선호하며, 특별한 순간을 만들어가는 타입입니다.';
      recommendedTags = ['데이트', '뷰맛집', '인스타', '디저트'];
    } else if (step1 === 'friends') {
      userType = '소셜 카페러';
      description = '친구들과 함께 즐거운 시간을 보내는 활발한 타입입니다.';
      recommendedTags = ['넓은', '디저트', '브런치', '인스타'];
    }

    if (step2 === 'modern') recommendedTags.push('모던한');
    else if (step2 === 'vintage') recommendedTags.push('빈티지');
    else if (step2 === 'nature') recommendedTags.push('자연친화');

    if (step3 === 'coffee') recommendedTags.push('스페셜티');
    else if (step3 === 'dessert') recommendedTags.push('디저트');
    else if (step3 === 'atmosphere') recommendedTags.push('인스타');

    // 결과 바인딩
    document.getElementById('user-type-title').textContent = `당신은 "${userType}"이에요!`;
    document.getElementById('user-type-description').textContent = description;
    const tags = document.getElementById('recommended-tags');
    tags.innerHTML = `
      <div class="text-sm font-medium text-gray-700 mb-3">추천 카페 태그</div>
      <div class="flex flex-wrap gap-2 justify-center">
        ${recommendedTags.slice(0, 6).map(t => `<span class="tag-chip selected px-3 py-2 rounded-full text-sm font-medium">${t}</span>`).join('')}
      </div>`;

    // 로컬 저장
    localStorage.setItem('userPreferences', JSON.stringify({
      userType, description, recommendedTags, answers: onboardingAnswers
    }));
  }

  // =======================
  // 완료/스킵
  // =======================
  window.completeOnboarding = async function () {
    const prefs = JSON.parse(localStorage.getItem('userPreferences') || '{}');
    try {
      await fetch('/api/member/preferences', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include', // 쿠키(JWT) 사용하는 경우
        body: JSON.stringify(prefs)
      });
    } catch (_) { /* 실패해도 UX 계속 */ }

    localStorage.setItem('onboardingDone', 'true');
    closeModal();

    // 추천 태그를 쿼리로 넘겨서 지도/리스트 초기 필터에 활용
    const qs = new URLSearchParams();
    if (prefs?.recommendedTags?.length) {
      qs.set('tags', prefs.recommendedTags.slice(0, 6).join(','));
    }
    location.href = `/index/map?${qs.toString()}`;
  };

  window.skipOnboarding = function () {
    closeModal();
    // 스킵 시에는 완료로 간주하지 않음 → 기본 카페 페이지로
    location.href = '/cafes';
  };
})();
