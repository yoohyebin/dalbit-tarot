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

/* 카드가 다 뒤집히는 시각(페이지를 연 뒤 초). main.css 의 카드 공개 애니메이션
   (0.35s + 장당 0.55s, 뒤집기 0.8s)과 맞춘다 */
const cardCount = document.querySelectorAll('.result-card').length;
const REVEAL_DONE_S = 0.35 + (cardCount - 1) * 0.55 + 0.8;

/* 해석을 문단으로 나눠 차례로 보이게 한다. 한 덩어리로 한 번에 뜨면 이야기라기보다 안내문처럼 보인다.
   문단 나누는 기준은 서버(ReadingViewController)와 같게 줄바꿈으로 한다.
   카드가 아직 뒤집히는 중에 해석이 도착하면(캐시 등) 다 뒤집힐 때까지 기다렸다 띄운다. */
function showInterpretation(text) {
    interpretation.replaceChildren(...text.split(/\n+/)
        .map((paragraph) => paragraph.trim())
        .filter(Boolean)
        .map((paragraph, index) => {
            const p = document.createElement('p');
            p.textContent = paragraph;
            p.style.setProperty('--p', String(index));
            return p;
        }));
    const remaining = Math.max(0, REVEAL_DONE_S - performance.now() / 1000);
    interpretation.style.setProperty('--delay', `${remaining.toFixed(2)}s`);
    interpretation.classList.remove('is-error');
    interpretation.hidden = false;
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

/* 미리보기 값은 서버가 만든다. og 태그와 같은 값을 써서 어느 경로로 공유해도 똑같이 보이게 한다 */
const share = {
    title: script.dataset.shareTitle || '달빛타로 결과',
    description: script.dataset.shareDescription || '',
    url: script.dataset.shareUrl || location.href,
    imageUrl: script.dataset.shareImage,
    imageWidth: Number(script.dataset.shareImageWidth),
    imageHeight: Number(script.dataset.shareImageHeight),
};
const kakaoKey = script.dataset.kakaoKey;

/* SDK 는 async 로 불러온다. 페이지를 열자마자 누르면 아직 없을 수 있어 누를 때 확인한다 */
function kakaoReady() {
    if (!kakaoKey || !window.Kakao) {
        return false;
    }
    if (!Kakao.isInitialized()) {
        Kakao.init(kakaoKey);
    }
    return Kakao.isInitialized();
}

function shareToKakao() {
    const link = { mobileWebUrl: share.url, webUrl: share.url };
    const result = Kakao.Share.sendDefault({
        objectType: 'feed',
        content: {
            title: share.title,
            description: share.description,
            imageUrl: share.imageUrl,
            imageWidth: share.imageWidth,
            imageHeight: share.imageHeight,
            link,
        },
        buttons: [{ title: '결과 보기', link }],
    });
    result?.catch?.((error) => console.warn('카카오톡 공유 실패', error));
}

shareButton.addEventListener('click', async () => {
    if (kakaoReady()) {
        try {
            shareToKakao();
            return;
        } catch (error) {
            // SDK 가 던지면 아래의 기존 공유 방식으로 넘어간다
            console.warn('카카오톡 공유 실패', error);
        }
    }

    const { title, url } = share;

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

    await copyLink(shareButton);
});

/* --- 링크 복사 ---
   navigator.clipboard 는 HTTPS 에서만 열린다. HTTP 로 운영하는 동안에는 예전 방식
   (textarea 를 선택해 execCommand('copy'))으로 복사한다. 이 방식은 HTTP 에서도 된다. */

async function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
        return;
    }
    const area = document.createElement('textarea');
    area.value = text;
    area.setAttribute('readonly', '');   // 휴대폰에서 키보드가 올라오지 않게
    area.style.position = 'fixed';
    area.style.top = '0';
    area.style.opacity = '0';
    document.body.appendChild(area);
    area.select();
    area.setSelectionRange(0, text.length);   // iOS 는 select() 만으로 선택되지 않는다
    const copied = document.execCommand('copy');
    area.remove();
    if (!copied) {
        throw new Error('복사하지 못했습니다');
    }
}

async function copyLink(button) {
    try {
        await copyText(share.url);
        const original = button.textContent;
        button.textContent = '복사했어요';
        button.disabled = true;
        setTimeout(() => {
            button.textContent = original;
            button.disabled = false;
        }, 2_000);
    } catch {
        prompt('아래 주소를 복사해 공유하세요', share.url);
    }
}

document.getElementById('copy-link').addEventListener('click', (event) => copyLink(event.currentTarget));
