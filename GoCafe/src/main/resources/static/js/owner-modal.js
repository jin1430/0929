(function(){
  // JWT 헬퍼(있으면 교체 가능)
  const AUTH = (() => {
    const token = () => localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken') || '';
    const withJwt = (init={}) => {
      const t = token();
      init.headers = Object.assign({ 'Accept':'application/json' }, init.headers || {}, t ? { 'Authorization':'Bearer '+t } : {});
      init.credentials = 'same-origin';
      return init;
    };
    return { withJwt };
  })();

  const modal   = document.getElementById('ownerModal');
  const openBtn = document.getElementById('ownerEditToggle'); // 상세 페이지의 버튼 id
  const cafeId  = document.querySelector('main[max-w-7xl], main.cg-container, main[data-cafe-id]')?.getAttribute('data-cafe-id')
                  || document.querySelector('main')?.dataset?.cafeId;

  if (!modal) return;

  function open(){ modal.classList.remove('hidden'); document.body.classList.add('overflow-hidden'); loadAll(); }
  function close(){ modal.classList.add('hidden'); document.body.classList.remove('overflow-hidden'); }

  openBtn?.addEventListener('click', (e)=>{ e.preventDefault(); open(); });
  modal.querySelectorAll('[data-close-modal]').forEach(el=>el.addEventListener('click', close));
  modal.addEventListener('click', e=>{ if(e.target.classList.contains('bg-black/50')) close(); });
  document.addEventListener('keydown', e=>{ if(e.key==='Escape' && !modal.classList.contains('hidden')) close(); });

  // ===== A. CafeInfo =====
  async function fetchCafeInfo(){
    const r = await fetch(`/api/cafe-infos/by-cafe/${cafeId}`, AUTH.withJwt());
    if (r.status === 404) return null;
    return r.ok ? r.json() : null;
  }
  async function loadCafeInfo(){
    const f = document.getElementById('cafeInfoFormModal'); if (!f) return;
    const data = await fetchCafeInfo();
    document.getElementById('cafeInfoIdModal').value = data?.id || '';
    f.openTime.value  = data?.openTime  || '';
    f.closeTime.value = data?.closeTime || '';
    f.holiday.value   = data?.holiday   || '';
    f.notice.value    = data?.notice    || '';
    f.info.value      = data?.info      || '';
  }
  async function saveCafeInfo(){
    const f = document.getElementById('cafeInfoFormModal'); if (!f) return false;
    const payload = {
      cafe: { id: Number(cafeId) },
      openTime: f.openTime.value, closeTime: f.closeTime.value,
      holiday: f.holiday.value,   notice: f.notice.value, info: f.info.value
    };
    const id = document.getElementById('cafeInfoIdModal').value;
    const url = id ? `/api/cafe-infos/${id}` : `/api/cafe-infos/upsert/${cafeId}`;
    const method = id ? 'PUT' : 'POST';
    const res = await fetch(url, AUTH.withJwt({ method, headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload) }));
    return res.ok;
  }
  document.getElementById('cafeInfoSaveBtnModal')?.addEventListener('click', async ()=>{
    const ok = await saveCafeInfo();
    alert(ok ? '저장했습니다.' : '저장 실패');
  });

  // ===== B. Photos =====
  function photoItemTpl(p){ return `
    <li class="py-3 px-1 flex items-center gap-3" data-id="${p.id}">
      <img src="${p.url}" class="w-20 h-20 object-cover rounded-lg border" onerror="this.style.display='none'">
      <div class="flex-1">
        <div class="font-medium">${p.originalName || '사진'}</div>
        ${p.main ? '<span class="inline-block text-xs px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 border border-emerald-200">대표</span>' : ''}
      </div>
      <div class="flex gap-2">
        ${p.main ? '' : '<button type="button" class="px-3 py-1 rounded border" data-act="main">대표설정</button>'}
        <button type="button" class="px-3 py-1 rounded border" data-act="del">삭제</button>
      </div>
    </li>`; }
  async function fetchPhotos(){ const r = await fetch(`/api/cafes/${cafeId}/photos`, AUTH.withJwt()); return r.ok ? r.json() : []; }
  async function loadPhotos(){
    const list = document.getElementById('cafePhotoListModal'); if (!list) return;
    const items = await fetchPhotos();
    list.innerHTML = items.map(photoItemTpl).join('');
    list.querySelectorAll('button[data-act]').forEach(btn=>{
      const id = btn.closest('[data-id]')?.dataset.id;
      if (btn.dataset.act==='del'){
        btn.addEventListener('click', async ()=>{
          if (!confirm('삭제할까요?')) return;
          const r = await fetch(`/api/cafes/photos/${id}`, AUTH.withJwt({ method:'DELETE' }));
          if (r.ok) loadPhotos(); else alert('삭제 실패');
        });
      } else if (btn.dataset.act==='main'){
        btn.addEventListener('click', async ()=>{
          const r = await fetch(`/api/cafes/photos/${id}/main`, AUTH.withJwt({ method:'PATCH' }));
          if (r.ok) loadPhotos(); else alert('변경 실패');
        });
      }
    });
  }
  document.getElementById('photoUploadFormModal')?.addEventListener('submit', async (e)=>{
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const r = await fetch(`/api/cafes/${cafeId}/photos`, AUTH.withJwt({ method:'POST', body: fd }));
    if (r.ok){ e.currentTarget.reset(); loadPhotos(); } else alert('업로드 실패');
  });

  // ===== C. Menus =====
  function menuItemTpl(m){ return `
    <li class="py-3 px-1 flex items-center gap-3" data-id="${m.id}">
      <img src="${m.photo || ''}" class="w-16 h-16 object-cover rounded-lg border" onerror="this.style.display='none'">
      <div class="flex-1">
        <div class="font-semibold">${m.name} <small class="text-gray-500">${m.price ?? ''}원</small></div>
        <div class="text-sm text-gray-500">${m.isNew ? 'New' : ''} ${m.isRecommended ? '· 추천' : ''}</div>
      </div>
      <div class="flex gap-2">
        <button type="button" class="px-3 py-1 rounded border" data-act="edit">수정</button>
        <button type="button" class="px-3 py-1 rounded border" data-act="del">삭제</button>
        <button type="button" class="px-3 py-1 rounded border" data-act="photo">사진</button>
      </div>
    </li>`; }
  async function fetchMenus(){ const r = await fetch(`/api/menus/by-cafe/${cafeId}`, AUTH.withJwt()); return r.ok ? r.json() : []; }
  async function loadMenus(){
    const list = document.getElementById('ownerMenuListModal'); if (!list) return;
    const items = await fetchMenus();
    list.innerHTML = items.map(menuItemTpl).join('');
    list.querySelectorAll('button[data-act]').forEach(btn=>{
      const id = btn.closest('[data-id]')?.dataset.id;
      if (btn.dataset.act==='del'){
        btn.addEventListener('click', async ()=>{
          if (!confirm('삭제할까요?')) return;
          const r = await fetch('/api/menus/'+id, AUTH.withJwt({ method:'DELETE' }));
          if (r.ok) loadMenus(); else alert('삭제 실패');
        });
      } else if (btn.dataset.act==='edit'){
        btn.addEventListener('click', async ()=>{
          const name = prompt('메뉴명?'); if (name===null) return;
          const price = Number(prompt('가격(원)?')); if (Number.isNaN(price)) return alert('가격을 숫자로 입력하세요.');
          const isNew = confirm('신규로 표시할까요?');
          const isRecommended = confirm('추천으로 표시할까요?');
          const payload = { name, price, isNew, isRecommended };
          const r = await fetch('/api/menus/'+id, AUTH.withJwt({ method:'PUT', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload) }));
          if (r.ok) loadMenus(); else alert('수정 실패');
        });
      } else if (btn.dataset.act==='photo'){
        btn.addEventListener('click', async ()=>{
          const input = document.createElement('input'); input.type='file'; input.accept='image/*';
          input.onchange = async ()=>{
            const fd = new FormData(); fd.append('file', input.files[0]);
            const r = await fetch('/api/menus/'+id+'/photo', AUTH.withJwt({ method:'POST', body: fd }));
            if (r.ok) loadMenus(); else alert('업로드 실패');
          };
          input.click();
        });
      }
    });
  }
  document.getElementById('menuCreateFormModal')?.addEventListener('submit', async (e)=>{
    e.preventDefault();
    const f = e.currentTarget;
    if (!f.menuPhoto.files[0]) return alert('메뉴 사진은 필수입니다.');
    const fd = new FormData();
    fd.append('cafeId', Number(cafeId));
    fd.append('name', f.name.value.trim());
    fd.append('price', Number(f.price.value));
    fd.append('isNew', f.isNew.checked);
    fd.append('isRecommended', f.isRecommended.checked);
    fd.append('file', f.menuPhoto.files[0]);
    const r = await fetch('/api/menus', AUTH.withJwt({ method:'POST', body: fd }));
    if (!r.ok) return alert('등록 실패');
    f.reset(); loadMenus();
  });

  // ===== 합쳐서 로드 =====
  async function loadAll(){ await Promise.all([loadCafeInfo(), loadPhotos(), loadMenus()]); }

  // 관리자 저장 버튼(카페 인포만 저장, 다른 섹션은 각각 즉시 처리)
  document.getElementById('ownerSaveAllBtn')?.addEventListener('click', async ()=>{
    const ok = await saveCafeInfo();
    alert(ok ? '저장했습니다.' : '저장 실패');
    if (ok) location.reload();
  });
})();
