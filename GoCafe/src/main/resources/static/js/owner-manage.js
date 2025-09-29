document.addEventListener('DOMContentLoaded', () => {
    const deleteBtn = document.getElementById('deleteCafeBtn');

    if (deleteBtn) {
        // URL에서 카페 ID 추출
        const pathParts = window.location.pathname.split('/');
        const cafeId = pathParts[pathParts.indexOf('cafes') + 1];

        deleteBtn.addEventListener('click', async () => {
            if (!confirm('정말 이 카페를 삭제하시겠습니까?')) return;

            try {
                // JWT 토큰 로컬 스토리지에서 가져오기 (이미 로그인 시스템에 맞춰 변경 가능)
                const token = localStorage.getItem('token');

                const response = await fetch(`/api/cafes/${cafeId}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (response.status === 204) {
                    alert('카페가 삭제되었습니다.');
                    window.location.href = '/';
                } else {
                    alert('삭제 실패. 다시 시도해 주세요.');
                }
            } catch (err) {
                console.error(err);
                alert('서버 오류가 발생했습니다.');
            }
        });
    }
});
