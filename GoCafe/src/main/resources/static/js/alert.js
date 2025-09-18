document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.cg-alert[role="alert"]').forEach(el=>{
    el.setAttribute('tabindex','-1');
    try{ el.focus({preventScroll:false}); }catch(_){}
    setTimeout(()=>{ el.style.transition='opacity .4s'; el.style.opacity='0'; setTimeout(()=>el.remove(),400); }, 4000);
  });
});
