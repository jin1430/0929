/* =========================================
   CafeGo – Global UI & Auth (CSP-safe)
   ========================================= */
(() => {
  "use strict";
  if (window.__cafegoInit) return;          // 중복 로드 방지
  window.__cafegoInit = true;

  /* ---------- Token helpers ---------- */
  function setToken(token){
    if(!token){ localStorage.removeItem('cafego_token'); return; }
    localStorage.setItem('cafego_token', token);
  }
  function getToken(){ return localStorage.getItem('cafego_token') || null; }
  function getAuthHeaders(){ const t=getToken(); return t?{Authorization:`Bearer ${t}`}:{ }; }
  /* ---------- 즐겨찾기 토글 (JWT) ---------- */
  (() => {
    const btn = document.getElementById('favBtn');
    if (!btn) return;

    const cafeId  = btn.dataset.cafeId;
    const countEl = document.getElementById('favCount');

    function setUI(favorited, count) {
      btn.dataset.favorited = favorited ? 'true' : 'false';
      btn.textContent = favorited ? '즐겨찾기 해제' : '즐겨찾기';
      if (typeof count === 'number' && countEl) countEl.textContent = `즐겨찾기 ${count}`;
    }

    btn.addEventListener('click', async (e) => {
      e.preventDefault();

      btn.disabled = true;
      try {
        const res = await fetch(`/api/favorites/${cafeId}/favorite`, {
          method: 'POST',
          headers: { 'Accept': 'application/json', ...getAuthHeaders() }
        });
        if (res.status === 401 || res.status === 403) {
        sessionStorage.setItem('returnTo', location.href);
         alert('로그인이 필요합니다.');
         location.href = '/login';
         return;
        }
        // { favorited: true/false, favoriteCount: number }
        const data = await res.json();
        setUI(!!(data.favorited ?? data.favorite ?? data.isFavorited),
              typeof data.favoriteCount === 'number' ? data.favoriteCount : undefined);

      } catch (err) {
        alert(err.message || '네트워크 오류');
      } finally {
        btn.disabled = false;
      }
    });
  })();


  /* ---------- Header/Login state ---------- */
  const loginFormHeader = document.getElementById('loginFormHeader');
  const logoutBtnHeader = document.getElementById('logoutBtnHeader');
  const meEmailHeader   = document.getElementById('meEmailHeader');
  const authOut         = document.querySelector('.cg-auth-out');
  const authIn          = document.querySelector('.cg-auth-in');

  // (본문에 과거 폼이 남아있어도 안전하게)
  const loginFormBody   = document.getElementById('loginForm');
  const logoutBtnBody   = document.getElementById('logoutBtn');
  const meBoxBody       = document.getElementById('meBox');
  const meEmailBody     = document.getElementById('meEmail');

  async function fetchMe(){
    try{
      const res = await fetch('/api/auth/me', { headers:{ ...getAuthHeaders() }});
      if(res.ok){
        const me = await res.json();
        if(meEmailHeader) meEmailHeader.textContent = me.memberEmail || '';
        if(authOut) authOut.style.display='none';
        if(authIn)  authIn.hidden=false;
        if(meBoxBody) meBoxBody.hidden=false;
        if(meEmailBody) meEmailBody.textContent = me.memberEmail || '';
      }else{
        if(authOut) authOut.style.display='';
        if(authIn)  authIn.hidden=true;
        if(meBoxBody) meBoxBody.hidden=true;
      }
    }catch(_){
      if(authOut) authOut.style.display='';
      if(authIn)  authIn.hidden=true;
      if(meBoxBody) meBoxBody.hidden=true;
    }
  }

  /* ---------- Login (header/body) ---------- */
  async function handleLogin(form){
    const fd = new FormData(form);
    const payload = { memberEmail: fd.get('memberEmail'), memberPassword: fd.get('memberPassword') };
    const res = await fetch('/api/auth/login', {
      method:'POST', headers:{ 'Content-Type':'application/json' }, body:JSON.stringify(payload)
    });
    if(res.ok){
      const data = await res.json(); // {tokenType:"Bearer", token:"..."}
      setToken(data.token);
      await fetchMe();
      const next = sessionStorage.getItem('returnTo')
      || new URLSearchParams(location.search).get('next')
      || '/';
    }else{
      const err = await res.json().catch(()=>({message:'로그인 실패'}));
      alert(err.message || '로그인 실패');
    }
  }
  if(loginFormHeader){ loginFormHeader.addEventListener('submit', e=>{ e.preventDefault(); handleLogin(loginFormHeader); }); }
  if(loginFormBody){   loginFormBody.addEventListener('submit',   e=>{ e.preventDefault(); handleLogin(loginFormBody);   }); }

  /* ---------- Logout (폼/버튼 모두 지원) ---------- */
  async function doLogout(e){
    if(e){ e.preventDefault(); }
    try{ await fetch('/api/auth/logout', { method:'POST', headers:{...getAuthHeaders()} }); }catch(_){}
    setToken(null);
    location.href = '/';
  }
  const logoutForm = document.getElementById('logoutForm');
  if (logoutForm) logoutForm.addEventListener('submit', doLogout);
  if (logoutBtnHeader) logoutBtnHeader.addEventListener('click', doLogout);
  if (logoutBtnBody)   logoutBtnBody.addEventListener('click',   doLogout);

// 칩 클릭 → 검색 이동 (모달 제외)
document.addEventListener('click', (e) => {
  const chip = e.target.closest('.cg-chip[data-tag], .cg-chip[data-category]');
  if (!chip) return;                         // 조건 칩이 아니면 무시
  if (chip.closest('#reviewModal')) return;  // 모달 내부 칩은 무시

  const { tag, category } = chip.dataset;
  const url = new URL('/search', location.origin);
  if (tag)      url.searchParams.set('tag', tag);
  if (category) url.searchParams.set('category', category);
  location.href = url.toString();
});


  /* ---------- data-confirm 공통 확인 ---------- */
  document.addEventListener('click', (e) => {
    const el = e.target.closest('[data-confirm]');
    if (el) {
      const msg = el.getAttribute('data-confirm') || '진행할까요?';
      if (!window.confirm(msg)) {
        e.preventDefault();
        e.stopPropagation();
      }
    }
  });

  /* ---------- 이미지 onerror 대체 ---------- */
  const hideOnError = (img) => {
    img.style.display = 'none';
    const wrap = img.parentElement;
    if (wrap) wrap.classList.add('cg-noimg');
  };
  document.querySelectorAll('img.cg-card__img, img.cg-feed__photo').forEach(img => {
    img.addEventListener('error', () => hideOnError(img));
  });

  /* ========== 리뷰 모달 ========== */
  document.addEventListener('DOMContentLoaded', () => {
    const modal    = document.getElementById('reviewModal');
    if (!modal) return;

    const dialog   = modal.querySelector('.cg-modal__dialog');
    const sheet    = modal.querySelector('.cg-modal__sheet');
    const backdrop = modal.querySelector('.cg-modal__backdrop');
    const form     = sheet && sheet.tagName === 'FORM' ? sheet : modal.querySelector('form');
    const cafeIdInp= document.getElementById('modalCafeId');
    const mainEl   = document.querySelector('main.cg-container');
    const body     = document.body;

    let lastFocus = null;
    let lastClickedCafeId = '';

    const cafeIdFromMain = () => mainEl?.getAttribute('data-cafe-id') || '';
    const cafeIdFromPath = () => (location.pathname.match(/\/cafes\/(\d+)/)||[])[1] || '';
    function ensureCafeId(clickedId) {
      if (!cafeIdInp) return;
      if (cafeIdInp.value && cafeIdInp.value.trim() !== '') return;
      cafeIdInp.value = (clickedId||'').trim() || cafeIdFromMain() || cafeIdFromPath();
    }

    function openModal(clickedId) {
      lastClickedCafeId = clickedId || lastClickedCafeId || '';
      ensureCafeId(lastClickedCafeId);
      lastFocus = document.activeElement;
      modal.classList.add('is-open');
      modal.setAttribute('aria-hidden','false');
      body.classList.add('cg-modal-open');

      const first = modal.querySelector('[autofocus], textarea, input, select, button, a[href]');
      if (first) try { first.focus({preventScroll:true}); } catch(_) {}
    }

    function closeModal() {
      modal.classList.remove('is-open');
      modal.setAttribute('aria-hidden','true');
      body.classList.remove('cg-modal-open');

      if (location.hash) {
        try { history.replaceState(null, document.title, location.pathname + location.search); } catch(_) {}
      }
      if (lastFocus && document.contains(lastFocus)) {
        try { lastFocus.focus({preventScroll:true}); } catch(_) {}
      }
    }

    // 열기 (위임)
    document.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-review-modal]');
      if (!btn) return;
      e.preventDefault();
      openModal(btn.getAttribute('data-cafe-id') || '');
    });

    // 닫기 — 백드롭/버튼
    if (backdrop) {
      backdrop.addEventListener('click', (e) => { e.preventDefault(); closeModal(); });
    }
    modal.querySelectorAll('[data-close-modal]').forEach(el => {
      el.addEventListener('click', (e) => { e.preventDefault(); closeModal(); });
    });

    // 보강: dialog 빈 영역 직접 클릭도 닫기 (pointer-events 차단했지만 혹시 모를 클릭 대비)
    if (dialog) {
      const onDialogEmpty = (e) => {
        if (e.target === dialog) { e.preventDefault(); e.stopPropagation(); closeModal(); }
      };
      dialog.addEventListener('mousedown', onDialogEmpty, true);
      dialog.addEventListener('click',     onDialogEmpty, true);
    }

    // ESC 닫기
    window.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && modal.classList.contains('is-open')) {
        e.preventDefault();
        closeModal();
      }
    }, true);

    // ?review=open 쿼리
    if (new URLSearchParams(location.search).get('review') === 'open') openModal();

    // 제출 가드
    if (form) {
      form.addEventListener('submit', (e) => {
        ensureCafeId(lastClickedCafeId);
        if (!cafeIdInp || !cafeIdInp.value) {
          e.preventDefault();
          alert('카페 정보가 유실되어 리뷰를 저장할 수 없어요. 새로고침 후 다시 시도해주세요.');
          return;
        }
        const fileInput = document.getElementById('photoInput');
        if (fileInput?.files) {
          if (fileInput.files.length > 5) {
            e.preventDefault();
            alert('사진은 최대 5장까지 업로드할 수 있어요.');
            return;
          }
          for (const f of fileInput.files) {
            if (f.size > 10 * 1024 * 1024) {
              e.preventDefault();
              alert('사진 용량은 파일당 10MB 이하로 업로드해주세요.');
              return;
            }
          }
        }
      });
    }
  });
  /* ---------- Auth 페이지 폼 (login/signup 템플릿) ---------- */
  // 기본은 서버사이드(MVC) 네이티브 제출을 사용.
  // AJAX가 꼭 필요하면 <form data-ajax="true"> 로 명시적으로 opt-in 하세요.

  const loginPageForm  = document.getElementById('loginPageForm');
  const signupPageForm = document.getElementById('signupPageForm');

  // (옵션) 로그인 폼을 AJAX로 처리하고 싶을 때만 data-ajax="true" 사용
  if (loginPageForm && loginPageForm.dataset.ajax === 'true') {
    loginPageForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const fd = new FormData(loginPageForm);

      // CSRF 토큰이 hidden input에 있다면 자동 포함됨(FormData)
      const res = await fetch('/login', {
        method: 'POST',
        body: new URLSearchParams(fd) // x-www-form-urlencoded로 전송
      });

      if (res.redirected) location.href = res.url; else location.reload();
    });
  }

  // (옵션) 회원가입 폼도 필요할 때만 AJAX. 기본은 네이티브 제출 권장.
  if (signupPageForm && signupPageForm.dataset.ajax === 'true') {
    signupPageForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const fd = new FormData(signupPageForm);

      const pw  = fd.get('password');
      const pw2 = fd.get('passwordConfirm');
      if (pw !== pw2) { alert('비밀번호가 일치하지 않습니다.'); return; }

      const res = await fetch('/signup', {
        method: 'POST',
        body: new URLSearchParams(fd) // 필드명: email, password, passwordConfirm, nickname, age, gender
      });

      if (res.redirected) { location.href = res.url; }
      else { location.reload(); } // 실패 시 서버가 에러 포함한 뷰를 다시 렌더링
    });
  }


  /* ---------- 초기화 ---------- */
  fetchMe();
})();
//intro
document.addEventListener('DOMContentLoaded', function() {
  // 샘플 DB (로컬 데모)
  const cafeDB = [
    {name:"달빛라떼", tags:["감성","라떼 맛집","인테리어"], desc:"감성적인 인테리어와 진한 라떼."},
    {name:"모모브런치", tags:["브런치","넓은 좌석","주차 가능"], desc:"브런치 플레이팅이 예쁜 카페."},
    {name:"코드카페", tags:["콘센트 있음","와이파이 빠름","작업 가능"], desc:"노트북 작업하기 좋은 카페."},
    {name:"테라스하우스", tags:["테라스","데이트 코스","디저트"], desc:"테라스에서 쉬어가기 좋은 곳."}
  ];

  function computeTags(answers){
    const mapping = {
      "감성":["감성","인테리어","라떼 맛집"],
      "활기":["모던","음악있음","테이블 多"],
      "작업":["콘센트 있음","와이파이 빠름","작업 가능"],
      "라떼":["라떼 맛집","핸드드립"],
      "브런치":["브런치","브런치 세트"],
      "디저트":["디저트","케이크"]
    };
    const scores = {};
    answers.forEach(a => (mapping[a]||[]).forEach(tag => scores[tag] = (scores[tag]||0)+1));
    return Object.entries(scores).sort((a,b)=>b[1]-a[1]).map(x=>x[0]);
  }

  function recommendLocal(tags){
    return cafeDB
      .map(c=>({...c, score: c.tags.filter(t=>tags.includes(t)).length}))
      .filter(c=>c.score>0)
      .sort((a,b)=>b.score-a.score);
  }

  function renderSampleCards(list){
    const wrap = document.getElementById('sample-cards');
    wrap.innerHTML = '';
    if(list.length===0){
      wrap.innerHTML = '<div class="card">추천 결과가 없습니다. 다른 선택으로 시도해보세요.</div>';
      return;
    }
    list.forEach(r=>{
      const el = document.createElement('div');
      el.className = 'card';
      el.innerHTML = `
        <strong>${r.name}</strong>
        <div style="color:#6b7c74;font-size:13px">${r.desc}</div>
        <div style="margin-top:8px">${r.tags.map(t=>`<span style="display:inline-block;background:#f3fff7;color:#2e8f6e;padding:4px 8px;border-radius:8px;margin-right:6px;font-size:12px">${t}</span>`).join('')}</div>
      `;
      wrap.appendChild(el);
    });
  }

  // 초기 샘플 렌더
  renderSampleCards(cafeDB.slice(0,3));

  // 데모 폼
  const demoBtn = document.getElementById('demo-btn');
  const demoClear = document.getElementById('demo-clear');
  const demoResult = document.getElementById('demo-result');

  demoBtn && demoBtn.addEventListener('click', async (e)=>{
    e.preventDefault();
    const q1 = document.getElementById('q1').value;
    const q2 = document.getElementById('q2').value;
    const q3 = document.getElementById('q3').value;
    const tags = computeTags([q1,q2,q3]);

    // ① 로컬 데모 (기본)
    let recs = recommendLocal(tags);

    // ② 서버 API

