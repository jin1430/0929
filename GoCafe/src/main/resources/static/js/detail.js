// detail.js : 카페 상세 전용 스크립트
(function () {
  // ================== 공용: 탭 전환 ==================
  function showTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(c => c.classList.add('hidden'));
    document.getElementById(tabName + '-tab')?.classList.remove('hidden');

    document.querySelectorAll('.tab-button').forEach(btn => {
      btn.classList.remove('tab-active');
      btn.classList.add('border-transparent', 'text-gray-500');
    });
    const current = document.querySelector(`.tab-button[data-tab="${tabName}"]`);
    current?.classList.add('tab-active');
    current?.classList.remove('border-transparent', 'text-gray-500');
  }

  document.addEventListener('click', (e) => {
    const btn = e.target.closest('.tab-button');
    if (!btn) return;
    const tab = btn.getAttribute('data-tab');
    if (tab) showTab(tab);
  });

  // ================== 유사 카페 추천 로드 ==================
  async function loadSimilar() {
    const container = document.getElementById('similar-grid');
    const section = document.getElementById('similar-section');
    const cafeId = document.querySelector('main[data-cafe-id]')?.getAttribute('data-cafe-id');
    if (!cafeId || !container || !section) return;

    try {
      const res = await fetch(`/api/recommend/similar/${cafeId}?limit=6`, { headers: { Accept: 'application/json' } });
      if (!res.ok) throw new Error('failed');
      const items = await res.json();
      if (!Array.isArray(items) || items.length === 0) return;

      container.innerHTML = items.map(it => `
        <article class="bg-white rounded-xl shadow-lg overflow-hidden card-hover">
          <div class="bg-gradient-to-br from-green-100 to-green-200 h-48 flex items-center justify-center">
            <img src="${it.mainPhotoUrl || '/images/placeholder-cafe.jpg'}" alt="${it.name}" class="w-full h-full object-cover" onerror="this.onerror=null;this.src='/images/placeholder-cafe.jpg'">
          </div>
          <div class="p-6">
            <h3 class="font-bold text-gray-800 mb-1">${it.name}</h3>
            <p class="text-gray-500 text-sm mb-3">${it.address || ''}</p>
            <div class="flex items-center justify-between text-sm">
              <div class="flex items-center gap-1">
                <span class="text-yellow-400">★</span>
                <span class="text-gray-600">${(it.avgRating ?? '').toString().slice(0,4)}</span>
              </div>
              <span class="text-green-600">점수 ${(it.score ?? 0).toFixed(2)}</span>
            </div>
          </div>
        </article>
      `).join('');

      section.classList.remove('hidden');
    } catch { /* silent */ }
  }

  // 초기 탭 & 추천 로딩
  showTab('menu');
  loadSimilar();

  // ================== 점주 모달(있는 경우) ==================
  document.addEventListener('DOMContentLoaded', () => {
    const dlg = document.getElementById('ownerModal');
    const openBtn = document.getElementById('ownerEditToggle');
    const closeBtn = document.getElementById('ownerModalClose');
    const cancelBtn = document.getElementById('cafeInfoCancelBtn');

    if (openBtn && dlg?.showModal) openBtn.addEventListener('click', () => dlg.showModal());
    if (closeBtn && dlg) closeBtn.addEventListener('click', () => dlg.close());
    if (cancelBtn && dlg) cancelBtn.addEventListener('click', () => dlg.close());
    if (dlg) dlg.addEventListener('cancel', e => e.preventDefault());

    const tabBtns = document.querySelectorAll('.owner-tab');
    const tabPanels = document.querySelectorAll('.owner-tab-panel');
    tabBtns.forEach(btn => btn.addEventListener('click', () => {
      tabBtns.forEach(b => b.classList.remove('tab-active','border-b-2'));
      btn.classList.add('tab-active','border-b-2');
      const id = 'owner-tab-' + btn.dataset.tab;
      tabPanels.forEach(p => p.classList.add('hidden'));
      document.getElementById(id)?.classList.remove('hidden');
    }));

    const cafeInfoForm = document.getElementById('cafeInfoForm');
    if (cafeInfoForm) {
      cafeInfoForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const res = await fetch(cafeInfoForm.action, { method: 'POST', body: new FormData(cafeInfoForm) });
        if (res.ok) location.reload();
        else alert('정보 저장 실패');
      });
    }

    const menuAddForm = document.getElementById('menuAddForm');
    if (menuAddForm) {
      menuAddForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const res = await fetch(menuAddForm.action, { method: 'POST', body: new FormData(menuAddForm) });
        if (res.ok) location.reload();
        else alert('메뉴 추가 실패');
      });
    }
  });

  // ================== 리뷰 작성 모달 ==================
  document.addEventListener('DOMContentLoaded', () => {
    try {
      const modal     = document.getElementById('reviewModal');
      const btnOpen   = document.getElementById('openReviewModal');
      const btnClose  = document.getElementById('closeReviewModal');
      const btnCancel = document.getElementById('cancelReview');
      const form      = document.getElementById('reviewForm');
      const ratingWrap= document.getElementById('ratingStars');
      const ratingInput = document.getElementById('ratingInput');
      const photoInput  = document.getElementById('photos');
      const preview     = document.getElementById('photoPreview');

      const open = () => { if (modal) modal.classList.remove('hidden'); }
      const close = () => { if (modal) modal.classList.add('hidden'); }

      btnOpen && btnOpen.addEventListener('click', open);
      btnClose && btnClose.addEventListener('click', close);
      btnCancel && btnCancel.addEventListener('click', close);
      modal && modal.addEventListener('click', (e) => {
        if (e.target.classList.contains('cg-modal__backdrop')) close();
      });

      // 별점 선택
      if (ratingWrap && ratingInput) {
        const paint = (n) => {
          [...ratingWrap.children].forEach(el => {
            el.textContent = (Number(el.dataset.v) <= n) ? '★' : '☆';
          });
        };
        paint(Number(ratingInput.value || 5));
        ratingWrap.addEventListener('click', (e) => {
          const v = Number(e.target?.dataset?.v || 0);
          if (v >= 1 && v <= 5) {
            ratingInput.value = String(v);
            paint(v);
          }
        });
      }

      // 사진 미리보기 (최대 6장)
      if (photoInput && preview) {
        photoInput.addEventListener('change', () => {
          preview.innerHTML = '';
          const files = Array.from(photoInput.files || []).slice(0, 6);
          files.forEach(f => {
            const url = URL.createObjectURL(f);
            const img = document.createElement('img');
            img.src = url; img.className = 'w-full h-28 object-cover rounded-md';
            const wrap = document.createElement('div');
            wrap.appendChild(img);
            preview.appendChild(wrap);
          });
        });
      }

      // ====== 폼 검증 (★ 핵심 수정: content -> reviewContent) ======
      if (form) {
        form.addEventListener('submit', (e) => {
          const rating = Number(form.querySelector('#ratingInput')?.value || 0);

          // 폼 범위에서 reviewContent를 찾음 (중복 ID/템플릿 안전)
          const content = (form.querySelector('textarea#reviewContent, textarea[name="reviewContent"]')?.value || '').trim();

          if (!(rating >= 1 && rating <= 5)) {
            e.preventDefault(); alert('평점을 선택해주세요.'); return;
          }

          // ▶ 5자 이상 제한 유지한다면 아래 유지
          if (content.length < 5) {
            e.preventDefault(); alert('리뷰 내용을 최소 5자 이상 입력해주세요.'); return;
          }

          // ※ 최소 글자수 제한을 없애고 싶다면 위 if 블록을 주석 처리하세요.
        });
      }

    } catch (err) {
      console.error('[detail.js] init error:', err);
    } finally {
      document.querySelectorAll('.fade-in').forEach(el => el.classList.add('is-visible','show'));
    }
  });
})();
