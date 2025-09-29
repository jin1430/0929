// src/main/resources/static/js/login.js

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('login-form');
    const loginBtn = document.getElementById('login-btn');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const errorMessageDiv = document.getElementById('error-message');

    const handleLogin = async () => {
        const email = emailInput.value;
        const password = passwordInput.value;

        // 간단한 유효성 검사
        if (!email || !password) {
            showError('이메일과 비밀번호를 모두 입력해주세요.');
            return;
        }

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email, password: password })
            });

            const data = await response.json();

            if (response.ok) {
                // ✅ [핵심] 로그인 성공 시, 'token'이라는 key로 토큰을 localStorage에 저장
                localStorage.setItem('token', data.token);

                alert('로그인되었습니다.');
                window.location.href = '/'; // 로그인 성공 후 메인 페이지로 이동
            } else {
                // 서버에서 보낸 에러 메시지를 표시
                showError(data.message || '아이디 또는 비밀번호를 확인해주세요.');
            }
        } catch (error) {
            console.error('Login Error:', error);
            showError('로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
        }
    };

    // 에러 메시지를 화면에 표시하는 함수
    const showError = (message) => {
        errorMessageDiv.textContent = message;
        errorMessageDiv.style.display = 'block';
    };

    // 버튼 클릭 시 로그인 함수 실행
    loginBtn.addEventListener('click', handleLogin);

    // Enter 키로도 로그인 되도록 설정
    loginForm.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault(); // Form의 기본 Enter 동작(submit)을 막음
            handleLogin();
        }
    });
});