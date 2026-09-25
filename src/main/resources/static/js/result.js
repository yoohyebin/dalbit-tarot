/* 결과 화면.
   카드는 서버가 렌더링해 이미 보인다. 해석만 비동기로 받아 채운다.
   LLM 응답이 10초 안팎 걸려 서버 렌더링으로 기다리면 흰 화면이 뜬다. */

const script = document.currentScript;
const shareToken = script.dataset.shareToken;
const needsInterpretation = script.dataset.needsInterpretation === 'true';

const REQUEST_TIMEOUT_MS = 60_000;

/* 기다리는 동안 문구를 바꿔 준다. 같은 문장이 30초 넘게 멈춰 있으면 멈춘 것처럼 보인다 */
const WAITING_MESSAGES = [
    { after: 0, text: '카드를 읽고 있어요…' },
    { after: 6_000, text: '카드의 흐름을 살펴보고 있어요…' },
    { after: 15_000, text: '조금만 더 기다려 주세요. 해석을 다듬고 있어요…' },
];

const loading = document.getElementById('loading');
const interpretation = document.getElementById('interpretation');
const retryButton = document.getElementById('retry');
const shareButton = document.getElementById('share');

/* --- 해석 요청 --- */

let messageTimers = [];

function startWaitingMessages() {
    const message = document.getElementById('loading-message');
    messageTimers = WAITING_MESSAGES.map(({ after, text }) =>
        setTimeout(() => { message.textContent = text; }, after));
}

function stopWaitingMessages() {
    messageTimers.forEach(clearTimeout);
    messageTimers = [];
}

function showInterpretation(text) {
    interpretation.textContent = text;
    interpretation.classList.remove('is-error');
    interpretation.hidden = false;
    interpretation.classList.add('fade-in');
}

function showError(message) {
    interpretation.textContent = message;
    interpretation.classList.add('is-error');
    interpretation.hidden = false;
    retryButton.hidden = false;
}

async function requestInterpretation() {
    loading.hidden = false;
    retryButton.hidden = true;
    interpretation.hidden = true;
    startWaitingMessages();

    try {
        const response = await fetch(`/api/readings/${shareToken}/interpretation`, {
            method: 'POST',
            signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || '해석을 가져오지 못했습니다.');
        }

        const result = await response.json();
        showInterpretation(result.interpretation);
    } catch (error) {
        const timedOut = error.name === 'TimeoutError' || error.name === 'AbortError';
        showError(timedOut
            ? '응답이 너무 오래 걸렸어요. 잠시 후 다시 시도해 주세요.'
            : error.message);
    } finally {
        stopWaitingMessages();
        loading.hidden = true;
    }
}

if (needsInterpretation) {
    requestInterpretation();
}

retryButton?.addEventListener('click', requestInterpretation);

/* --- 공유 --- */

shareButton.addEventListener('click', async () => {
    const url = location.href;
    const title = '달빛타로 결과';

    if (navigator.share) {
        try {
            await navigator.share({ title, url });
            return;
        } catch (error) {
            if (error.name === 'AbortError') {
                return; // 사용자가 공유 시트를 닫은 것
            }
        }
    }

    try {
        await navigator.clipboard.writeText(url);
        const original = shareButton.textContent;
        shareButton.textContent = '링크를 복사했어요';
        setTimeout(() => { shareButton.textContent = original; }, 2_000);
    } catch {
        prompt('아래 주소를 복사해 공유하세요', url);
    }
});
