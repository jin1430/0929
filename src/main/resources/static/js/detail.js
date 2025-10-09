// detail.js : 카페 상세 전용 스크립트
(function () {
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

  // 탭 전환 (버튼 공통 위임)
  document.addEventListener('click', (e) => {
    const btn = e.target.closest('.tab-button');
    if (!btn) return;
    e.preventDefault();
    const tab = btn.getAttribute('data-tab');
    if (tab) showTab(tab);
  });

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
        <article class="bg-white rounded-xl shadow-lg overflow-hidden">
          <div class="bg-gradient-to-br from-green-100 to-green-200 h-48 flex items-center justify-center">
            <img src="${it.mainPhotoUrl || '/images/placeholder-cafe.jpg'}" alt="${escapeHtml(it.name || '')}" class="w-full h-full object-cover" onerror="this.onerror=null;this.src='/images/placeholder-cafe.jpg'">
          </div>
          <div class="p-6">
            <h3 class="font-bold text-gray-800 mb-1">${escapeHtml(it.name || '')}</h3>
            <p class="text-gray-500 text-sm mb-3">${escapeHtml(it.address || '')}</p>
            <div class="flex items-center justify-between text-sm">
              <div class="flex items-center gap-1"><span class="text-yellow-400">★</span><span class="text-gray-600">${String(it.avgRating ?? '').slice(0,4)}</span></div>
              <span class="text-green-600">점수 ${(Number(it.score) || 0).toFixed(2)}</span>
            </div>
          </div>
        </article>
      `).join('');
      section.classList.remove('hidden');
    } catch (e) {
      // 조용히 실패
      console.warn('[detail.js] similar fetch failed');
    }
  }

  function escapeHtml(s){
    return String(s || '').replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));
  }

  document.addEventListener('DOMContentLoaded', () => {
    // 초기 탭
    showTab('menu');

    // 추천 로드
    loadSimilar();

//    // 점주 모달(있는 경우)
//    const dlg = document.getElementById('ownerModal');
//    const openBtn = document.getElementById('ownerEditToggle');
//    const closeBtn = document.getElementById('ownerModalClose');
//    const cancelBtn = document.getElementById('cafeInfoCancelBtn');
//    if (openBtn && dlg?.showModal) openBtn.addEventListener('click', () => dlg.showModal());
//    if (closeBtn && dlg) closeBtn.addEventListener('click', () => dlg.close());
//    if (cancelBtn && dlg) cancelBtn.addEventListener('click', () => dlg.close());
//    if (dlg) dlg.addEventListener('cancel', e => e.preventDefault());
const ownerDialog = document.getElementById('ownerModal');
    const openBtn = document.getElementById('ownerEditToggle');
    const closeBtn = document.getElementById('ownerModalClose');
    const cancelBtn = document.getElementById('cafeInfoCancelBtn');

    if (openBtn && ownerDialog?.showModal) {
        openBtn.addEventListener('click', () => ownerDialog.showModal());
    }
    if (closeBtn && ownerDialog) {
        closeBtn.addEventListener('click', () => ownerDialog.close());
    }
    if (cancelBtn && ownerDialog) {
        cancelBtn.addEventListener('click', () => ownerDialog.close());
    }
    // 사용자가 ESC키로 닫는 기본 동작 방지 (필요시)
    if (ownerDialog) {
        ownerDialog.addEventListener('cancel', e => e.preventDefault());
    }

    // 모달 내부 탭 전환 기능 (이 부분이 추가되었습니다!)
        const ownerTabs = document.querySelectorAll('.owner-tab');
        const ownerPanels = document.querySelectorAll('.owner-tab-panel');
        ownerTabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const tabId = tab.dataset.tab;

                // 모든 탭 버튼 스타일 초기화
                ownerTabs.forEach(t => {
                    t.classList.remove('tab-active', 'font-semibold');
                    t.classList.add('border-transparent', 'text-gray-500');
                });
                // 클릭된 탭 버튼만 활성 스타일 적용
                tab.classList.add('tab-active', 'font-semibold');
                tab.classList.remove('border-transparent', 'text-gray-500');

                // 모든 탭 패널 숨기기
                ownerPanels.forEach(panel => {
                    panel.classList.add('hidden');
                });
                // 클릭된 탭에 해당하는 패널만 보여주기
                document.getElementById(`owner-tab-${tabId}`)?.classList.remove('hidden');
            });
        });
        // ==========================================================
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ 수정 끝 ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
        // ==========================================================
    // 리뷰 작성 모달
    try {
      const modal       = document.getElementById('reviewModal');
      const btnOpen     = document.getElementById('openReviewModal');
      const btnClose    = document.getElementById('closeReviewModal');
      const btnCancel   = document.getElementById('cancelReview');
      const form        = document.getElementById('reviewForm');
      const ratingWrap  = document.getElementById('ratingStars');
      const ratingInput = document.getElementById('ratingInput');
      const photoInput  = document.getElementById('photos');
      const preview     = document.getElementById('photoPreview');

      const open  = () => { modal?.classList.remove('hidden'); }
      const close = () => { modal?.classList.add('hidden'); }
      btnOpen?.addEventListener('click', open);
      btnClose?.addEventListener('click', close);
      btnCancel?.addEventListener('click', close);
      modal?.addEventListener('click', (e) => { if (e.target.classList.contains('cg-modal__backdrop')) close(); });

      if (ratingWrap && ratingInput) {
        const paint = (n) => { [...ratingWrap.children].forEach(el => { el.textContent = (+el.dataset.v <= n) ? '★' : '☆'; }); };
        paint(Number(ratingInput.value || 5));
        ratingWrap.addEventListener('click', (e) => {
          const v = Number(e.target?.dataset?.v || 0);
          if (v >= 1 && v <= 5) { ratingInput.value = String(v); paint(v); }
        });
      }

      if (photoInput && preview) {
        photoInput.addEventListener('change', () => {
          preview.innerHTML = '';
          const files = Array.from(photoInput.files || []).slice(0, 6);
          files.forEach(f => {
            const url = URL.createObjectURL(f);
            const img = document.createElement('img');
            img.src = url; img.className = 'w-full h-28 object-cover rounded-md';
            const wrap = document.createElement('div'); wrap.appendChild(img);
            preview.appendChild(wrap);
          });
        });
      }

      if (form) {
        form.addEventListener('submit', (e) => {
          const rating  = Number(form.querySelector('#ratingInput')?.value || 0);
          const content = (form.querySelector('textarea#reviewContent, textarea[name="reviewContent"]')?.value || '').trim();
          if (!(rating >= 1 && rating <= 5)) { e.preventDefault(); alert('평점을 선택해주세요.'); return; }
          if (content.length < 5) { e.preventDefault(); alert('리뷰 내용을 최소 5자 이상 입력해주세요.'); return; }
        });
      }
    } catch (err) {
      console.error('[detail.js] init error:', err);
    } finally {
      document.querySelectorAll('.fade-in').forEach(el => el.classList.add('is-visible','show'));
    }
  });

})();
