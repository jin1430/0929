// /static/js/main.js  (혹은 resources/static/js/main.js)
(function () {
  // 페이지 이동: 서버 라우팅에 맞춰 링크로 이동
  function showPage(name) {
    switch (name) {
      case 'home':
        window.location.href = '/';
        break;
      case 'cafes':
        window.location.href = '/cafes';
        break;
      case 'reviews':
        window.location.href = '/reviews';
        break;
      case 'missions':
        window.location.href = '/missions';
        break;
      default:
        window.location.href = '/';
    }
  }

  // 검색: 메인 히어로 검색 인풋 값을 /cafes?q= 로 전달
  function searchCafes() {
    const input = document.querySelector('.cg-search .cg-input');
    const q = (input && input.value) ? input.value.trim() : '';
    const url = q ? `/cafes?q=${encodeURIComponent(q)}` : '/cafes';
    window.location.href = url;
  }

  // 간단 토스트 유틸 (필요 시 서버 플래시 메시지와 함께 사용)
  function showToast(title, message) {
    let toast = document.getElementById('toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'toast';
      toast.style.position = 'fixed';
      toast.style.top = '20px';
      toast.style.right = '20px';
      toast.style.background = '#fff';
      toast.style.borderLeft = '4px solid #3aa17e';
      toast.style.padding = '16px 20px';
      toast.style.borderRadius = '8px';
      toast.style.boxShadow = '0 10px 15px -3px rgba(0,0,0,.1)';
      toast.style.transform = 'translateX(400px)';
      toast.style.transition = 'transform .3s ease';
      toast.style.zIndex = '1001';
      toast.innerHTML = `
        <div style="display:flex;align-items:center;">
          <div style="margin-right:12px;color:#14b8a6;">✓</div>
          <div>
            <div id="toast-title" style="font-weight:600;color:#111827">알림</div>
            <div id="toast-message" style="font-size:12px;color:#4b5563">메시지</div>
          </div>
        </div>
      `;
      document.body.appendChild(toast);
    }
    toast.querySelector('#toast-title').textContent = title || '알림';
    toast.querySelector('#toast-message').textContent = message || '';
    requestAnimationFrame(() => { toast.style.transform = 'translateX(0)'; });
    setTimeout(() => { toast.style.transform = 'translateX(400px)'; }, 3000);
  }

  // 전역에 노출 (템플릿의 onclick 훅과 연결)
  window.showPage = showPage;
  window.searchCafes = searchCafes;
  window.showToast = showToast;

  // 초기 포커스/접근성(선택)
  document.addEventListener('DOMContentLoaded', () => {
    const input = document.querySelector('.cg-search .cg-input');
    if (input) input.setAttribute('aria-label', '카페 검색');
  });
})();
