(function () {
  const btn = document.getElementById('notiBtn');
  const dd  = document.getElementById('notiDropdown');
  const list= document.getElementById('notiList');
  const countEl = document.getElementById('notiCount');
  const readAllBtn = document.getElementById('notiReadAll');

  if (!btn || !dd) return; // 비로그인

  btn.addEventListener('click', () => {
    const opened = dd.classList.contains('is-open');
    dd.classList.toggle('is-open', !opened);
    if (!opened) fetchList();
  });

  // 외부 클릭 시 닫기
  document.addEventListener('click', (e) => {
    if (!dd.classList.contains('is-open')) return;
    const within = dd.contains(e.target) || btn.contains(e.target);
    if (!within) dd.classList.remove('is-open');
  });

  function fmt(ts) {
    try { return new Date(ts).toLocaleString(); } catch (e) { return ts; }
  }

  function render(items) {
    if (!Array.isArray(items) || items.length === 0) {
      list.innerHTML = '<li><div class="muted">알림이 없습니다.</div></li>';
      if (countEl) countEl.textContent = '0';
      return;
    }

    const unread = items.filter(it => it.read === false).length;
    if (countEl) countEl.textContent = String(unread);

    list.innerHTML = items.map(it => `
      <li>
        <div style="font-size:14px">${it.message || '(알림)'}</div>
        <div class="cg-dropdown__time">${fmt(it.createdAt)}</div>
        ${it.read ? '' : `<button data-id="${it.id}" class="markRead" type="button" style="margin-top:6px">읽음</button>`}
      </li>
    `).join('');

    list.querySelectorAll('.markRead').forEach(btn => {
      btn.addEventListener('click', () => {
        fetch('/api/notifications/' + btn.dataset.id + '/read', {
          method: 'POST',
          credentials: 'same-origin'
        }).then(fetchList);
      });
    });
  }

  function fetchList() {
    fetch('/api/notifications', { credentials: 'same-origin' })
      .then(r => r.json()).then(render).catch(() => { /* ignore */ });
  }

  function fetchCount() {
    fetch('/api/notifications/unread-count', { credentials: 'same-origin' })
      .then(r => r.text()).then(n => { if (countEl) countEl.textContent = n; }).catch(() => { /* ignore */ });
  }

  readAllBtn?.addEventListener('click', () => {
    fetch('/api/notifications/read-all', { method: 'POST', credentials: 'same-origin' })
      .then(fetchList);
  });

  // 초기/주기 갱신
  fetchCount();
  setInterval(fetchCount, 10000);
})();