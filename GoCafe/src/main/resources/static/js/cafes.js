// cafes.js | UTF-8
// 의존성: TailwindCSS (선택), window.mockData (데이터), window.showCafeDetail / window.toggleFavorite (있으면 사용)

/* 전역 초기화 함수
   - 페이지 로드/라우터 진입 시 initCafeSearch()만 호출하면 됨
   - 예) document.addEventListener('DOMContentLoaded', () => initCafeSearch());
*/
(function (global) {
  const state = {
    filters: { region: '', sort: 'latest', search: '', tags: [] }
  };

  const D = () => global.mockData || { cafes: [], cafeInfos: [], cafeTags: [], reviews: [], tagCodes: {} };

  // 안전 래퍼 (외부 글로벌 함수가 없으면 NO-OP)
  const onShowDetail = global.showCafeDetail || function (id) { console.warn('showCafeDetail가 정의되어 있지 않습니다.', id); };
  const onToggleFavorite = global.toggleFavorite || function (id) { console.warn('toggleFavorite가 정의되어 있지 않습니다.', id); };

  // ------- 데이터 헬퍼 -------
  function getCafeInfo(cafeId) {
    return (D().cafeInfos || []).find(info => info.cafe === String(cafeId));
  }
  function getCafeTags(cafeId) {
    const map = D().tagCodes || {};
    const raw = (D().cafeTags || []).filter(t => t.cafe === String(cafeId));
    return raw.map(t => map[t.code] || t.code);
  }
  function getCafeReviews(cafeId) {
    return (D().reviews || []).filter(r => r.cafe === String(cafeId));
  }
  function calculateCafeRating(cafeId) {
    const reviews = getCafeReviews(cafeId);
    if (!reviews.length) return '0.0';
    const avg = reviews.reduce((s, r) => s + (Number(r.taste) || 0), 0) / reviews.length;
    return (Math.round(avg * 10) / 10).toFixed(1);
  }
  function isCafeFavorite(cafeId, memberId = 1) {
    const favs = D().favorites || [];
    return favs.some(f => f.cafe === String(cafeId) && f.member === String(memberId));
  }

  // ------- 렌더링 -------
  function renderTagFilters() {
    const container = document.getElementById('tag-filters');
    if (!container) return;

    const allTags = Object.values(D().tagCodes || {});
    container.innerHTML = allTags.map(tag =>
      `<button type="button" onclick="toggleTagFilter('${tag.replace(/'/g, "\\'")}')"
               class="tag-chip px-3 py-2 rounded-full text-sm font-medium ${state.filters.tags.includes(tag) ? 'selected' : ''}"
               data-tag="${tag}">${tag}</button>`
    ).join('');
  }

  function applyCafeFilters() {
    const regionEl = document.getElementById('region-filter');
    const sortEl   = document.getElementById('sort-filter');
    const searchEl = document.getElementById('cafe-search');

    state.filters.region = regionEl ? regionEl.value : '';
    state.filters.sort   = sortEl ? sortEl.value : 'latest';
    state.filters.search = searchEl ? searchEl.value.trim() : '';

    renderCafeList();
  }

  function toggleTagFilter(tag) {
    const tags = state.filters.tags;
    const idx = tags.indexOf(tag);
    if (idx > -1) tags.splice(idx, 1);
    else tags.push(tag);

    // 버튼 토글 UI
    const btn = document.querySelector(`#tag-filters [data-tag="${CSS.escape(tag)}"]`);
    if (btn) btn.classList.toggle('selected');

    renderCafeList();
  }

  function renderCafeList() {
    const container = document.getElementById('cafe-list');
    if (!container) return;

    const { region, sort, search, tags } = state.filters;
    let cafes = (D().cafes || []).filter(c => c.cafeStatus === 'APPROVED');

    // 지역 필터 (주소 두 번째 토큰 가정)
    if (region) {
      cafes = cafes.filter(c => {
        const parts = (c.address || '').split(/\s+/);
        return parts[1] === region || c.address.includes(region);
      });
    }
    // 검색 (이름)
    if (search) {
      const key = search.toLowerCase();
      cafes = cafes.filter(c => (c.name || '').toLowerCase().includes(key));
    }
    // 태그 포함 여부 (OR)
    if (tags && tags.length) {
      cafes = cafes.filter(c => {
        const ct = getCafeTags(c.id);
        return tags.some(t => ct.includes(t));
      });
    }

    // 정렬
    if (sort === 'rating') {
      cafes.sort((a, b) => parseFloat(calculateCafeRating(b.id)) - parseFloat(calculateCafeRating(a.id)));
    } else if (sort === 'reviews') {
      cafes.sort((a, b) => getCafeReviews(b.id).length - getCafeReviews(a.id).length);
    } else if (sort === 'views') {
      cafes.sort((a, b) => (b.views || 0) - (a.views || 0));
    } else {
      // 최신순: creationDate 내림차순 → 없으면 id 내림차순
      cafes.sort((a, b) => {
        const ad = new Date(b.creationDate || 0) - new Date(a.creationDate || 0);
        if (ad !== 0) return ad;
        return (b.id || 0) - (a.id || 0);
      });
    }

    container.innerHTML = cafes.map(cafe => {
      const info    = getCafeInfo(cafe.id);
      const tagsArr = getCafeTags(cafe.id);
      const reviews = getCafeReviews(cafe.id);
      const rating  = calculateCafeRating(cafe.id);
      const fav     = isCafeFavorite(cafe.id);
      const regionText = (cafe.address || '').split(/\s+/)[1] || '';

      return `
      <div class="bg-white rounded-2xl overflow-hidden card-shadow">
        <div class="cafe-image h-48 text-white text-4xl">☕</div>
        <div class="p-6">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-xl font-bold text-gray-800">${escapeHtml(cafe.name)}</h3>
            <button onclick="(${onToggleFavorite.name || 'toggleFavorite'})(${cafe.id})"
                    class="text-2xl ${fav ? 'text-red-500' : 'text-gray-300'}">
              ${fav ? '❤️' : '🤍'}
            </button>
          </div>

          <div class="flex items-center mb-2">
            <span class="rating-stars mr-1">★</span>
            <span class="font-semibold text-gray-700 mr-2">${rating}</span>
            <span class="text-sm text-gray-500">(${reviews.length}개 리뷰)</span>
          </div>

          <p class="text-gray-600 mb-2">${escapeHtml(cafe.address || '')}</p>
          <p class="text-sm text-gray-500 mb-4">${escapeHtml(info?.info || '카페 정보')}</p>

          <div class="flex flex-wrap gap-1 mb-4">
            ${tagsArr.map(t => `<span class="tag-chip px-2 py-1 rounded-full text-xs">${escapeHtml(t)}</span>`).join('')}
          </div>

          <div class="flex items-center justify-between mb-4 text-sm text-gray-500">
            <span>⏰ ${info ? `${info.openTime}-${info.closeTime}` : '정보없음'}</span>
            <span>📞 ${escapeHtml(cafe.phoneNumber || '')}</span>
          </div>

          <button onclick="(${onShowDetail.name || 'showCafeDetail'})(${cafe.id})"
                  class="w-full btn-primary text-white py-3 rounded-2xl font-medium">
            자세히 보기
          </button>
        </div>
      </div>`;
    }).join('');
  }

  // HTML 이스케이프
  function escapeHtml(s) {
    return String(s || '').replace(/[&<>"']/g, m => ({
      '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
    }[m]));
  }

  // ------- 공개 API / 기존 코드 호환용 -------
  global.renderCafeList    = renderCafeList;
  global.renderTagFilters  = renderTagFilters;
  global.applyCafeFilters  = applyCafeFilters;
  global.toggleTagFilter   = toggleTagFilter;

  global.initCafeSearch = function initCafeSearch() {
    renderTagFilters();
    renderCafeList();
  };
})(window);
