// cafes.js | UTF-8
// 서버렌더만 써도 안전하게 no-op 되도록 방어코드 포함
(function (global) {
  const state = { filters: { region: '', sort: 'latest', search: '', tags: [] } };

  const D = () => global.mockData || { cafes: [], cafeInfos: [], cafeTags: [], reviews: [], tagCodes: {}, favorites: [] };

  // 외부 훅(선택)
  const onShowDetail     = global.showCafeDetail   || function(){};
  const onToggleFavorite = global.toggleFavorite   || function(){};

  function getCafeInfo(cafeId)   { return (D().cafeInfos || []).find(i => String(i.cafe) === String(cafeId)); }
  function getCafeTags(cafeId)   {
    const map = D().tagCodes || {};
    return (D().cafeTags || [])
      .filter(t => String(t.cafe) === String(cafeId))
      .map(t => map[t.code] || t.code);
  }
  function getCafeReviews(cafeId){ return (D().reviews || []).filter(r => String(r.cafe) === String(cafeId)); }
  function calculateCafeRating(cafeId){
    const r = getCafeReviews(cafeId);
    if (!r.length) return '0.0';
    const sum = r.reduce((s, v) => s + (Number(v.taste) || 0), 0);
    const avg = sum / r.length;
    return (Math.round(avg * 10) / 10).toFixed(1);
  }
  function isCafeFavorite(cafeId, memberId = 1){
    return (D().favorites || []).some(f => String(f.cafe) === String(cafeId) && String(f.member) === String(memberId));
  }

  function renderTagFilters(){
    const container = document.getElementById('tag-filters');
    if (!container) return;

    const allTags = Object.values(D().tagCodes || {});
    container.innerHTML = allTags.map(tag =>
      `<button type="button" class="tag-chip px-3 py-2 rounded-full text-sm font-medium ${state.filters.tags.includes(tag) ? 'selected' : ''}" data-tag="${escapeHtml(tag)}">${escapeHtml(tag)}</button>`
    ).join('');

    // 이벤트 바인딩 중복 방지
    if (!container.dataset.bound) {
      container.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-tag]');
        if (!btn) return;
        toggleTagFilter(btn.dataset.tag);
      });
      container.dataset.bound = '1';
    }
  }

  function applyCafeFilters(){
    const regionEl = document.getElementById('regionSelect');
    const sortEl   = document.getElementById('sortSelect');
    const searchEl = document.getElementById('cafe-search');

    state.filters.region = regionEl ? regionEl.value : '';
    state.filters.sort   = sortEl ? sortEl.value : 'latest';
    state.filters.search = searchEl ? searchEl.value.trim() : '';

    renderCafeList();
  }

  function toggleTagFilter(tag){
    const tags = state.filters.tags;
    const i = tags.indexOf(tag);
    if (i >= 0) tags.splice(i,1); else tags.push(tag);

    const el = document.querySelector(`#tag-filters [data-tag="${CSS.escape(tag)}"]`);
    el?.classList.toggle('selected');

    renderCafeList();
  }

  function renderCafeList(){
    const container = document.getElementById('cafe-list');
    if (!container) return;

    const { region, sort, search, tags } = state.filters;
    let cafes = (D().cafes || []).filter(c => c.cafeStatus === 'APPROVED');

    if (region) {
      cafes = cafes.filter(c => (c.address || '').includes(region));
    }
    if (search) {
      const k = search.toLowerCase();
      cafes = cafes.filter(c => (c.name || '').toLowerCase().includes(k));
    }
    if (tags?.length) {
      cafes = cafes.filter(c => {
        const ct = getCafeTags(c.id);
        return tags.some(t => ct.includes(t));
      });
    }

    if (sort === 'views' || sort === 'popular') {
      cafes.sort((a,b) => (b.views || 0) - (a.views || 0));
    } else {
      cafes.sort((a,b) => {
        const d = new Date(b.creationDate || 0) - new Date(a.creationDate || 0);
        return d !== 0 ? d : (Number(b.id) || 0) - (Number(a.id) || 0);
      });
    }

    container.innerHTML = cafes.map(cafe => {
      const info    = getCafeInfo(cafe.id);
      const tagsArr = getCafeTags(cafe.id);
      const reviews = getCafeReviews(cafe.id);
      const rating  = calculateCafeRating(cafe.id);
      const fav     = isCafeFavorite(cafe.id);

      return `
      <div class="bg-white rounded-2xl overflow-hidden shadow hover:shadow-md transition">
        <div class="h-48 bg-gray-100 flex items-center justify-center text-4xl">☕</div>
        <div class="p-6">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-xl font-bold text-gray-800">${escapeHtml(cafe.name)}</h3>
            <button type="button" class="text-2xl ${fav ? 'text-red-500' : 'text-gray-300'}" aria-label="즐겨찾기">${fav ? '❤️' : '🤍'}</button>
          </div>
          <div class="flex items-center mb-2">
            <span class="text-yellow-400 mr-1">★</span>
            <span class="font-semibold text-gray-700 mr-2">${rating}</span>
            <span class="text-sm text-gray-500">(${reviews.length}개 리뷰)</span>
          </div>
          <p class="text-gray-600 mb-2">${escapeHtml(cafe.address || '')}</p>
          <p class="text-sm text-gray-500 mb-4">${escapeHtml((info && (info.info || info.cafeInfo)) || '카페 정보')}</p>
          <div class="flex flex-wrap gap-1 mb-4">
            ${tagsArr.map(t => `<span class="bg-green-100 text-green-700 px-2 py-1 rounded-full text-xs">${escapeHtml(t)}</span>`).join('')}
          </div>
          <div class="flex items-center justify-between mb-4 text-sm text-gray-500">
            <span>⏰ ${info ? `${info.openTime || info.cafeOpenTime || '정보없음'}-${info.closeTime || info.cafeCloseTime || ''}` : '정보없음'}</span>
            <span>📞 ${escapeHtml(cafe.phoneNumber || '')}</span>
          </div>
          <a href="/cafes/${cafe.id}" class="w-full inline-block text-center bg-green-600 text-white py-3 rounded-2xl font-medium">자세히 보기</a>
        </div>
      </div>`;
    }).join('');
  }

  function escapeHtml(s){
    return String(s || '').replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));
  }

  // 공개
  global.renderCafeList   = renderCafeList;
  global.renderTagFilters = renderTagFilters;
  global.applyCafeFilters = applyCafeFilters;
  global.toggleTagFilter  = toggleTagFilter;

  // ================== [ 해결 코드 ] ==================
  // 초기 페이지 로딩 시 서버가 그려준 내용을 지우지 않도록
  // renderCafeList() 호출을 제거합니다.
  global.initCafeSearch = function initCafeSearch(){
    renderTagFilters();
    // renderCafeList(); // <--- 이 부분이 서버가 렌더링한 결과를 지워버리므로 주석 처리 또는 삭제

    // 필터 입력 변화 시에만 목록을 다시 그리도록 이벤트 리스너는 그대로 둡니다.
    document.getElementById('regionSelect')?.addEventListener('change', applyCafeFilters);
    document.getElementById('sortSelect')?.addEventListener('change', applyCafeFilters);
    document.getElementById('cafe-search')?.addEventListener('input',  applyCafeFilters);
  };

})(window);