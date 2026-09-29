/* 의견 보내기. 성공하면 폼을 감추고 안내로 바꾼다. */

const form = document.getElementById('feedback-form');
const nickname = document.getElementById('nickname');
const content = document.getElementById('content');
const counter = document.getElementById('counter');
const status = document.getElementById('status');
const submitButton = document.getElementById('submit');
const done = document.getElementById('done');

content.addEventListener('input', () => {
    counter.textContent = String(content.value.length);
});

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    if (!content.value.trim()) {
        status.textContent = '내용을 입력해 주세요.';
        return;
    }

    submitButton.disabled = true;
    status.textContent = '보내는 중…';

    try {
        const response = await fetch('/api/feedback', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                nickname: nickname.value.trim() || null,
                content: content.value,
            }),
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            const field = error.fields && error.fields[0];
            throw new Error(field ? field.reason : (error.message || '보내지 못했습니다.'));
        }

        form.hidden = true;
        done.hidden = false;
    } catch (error) {
        status.textContent = error.message;
        submitButton.disabled = false;
    }
});
