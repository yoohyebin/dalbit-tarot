/* 카드 뽑기 연출.
   무엇이 뽑히는지는 서버가 정한다. 여기서 고르는 건 몇 장을 뽑을지와 연출뿐이다. */

const STORAGE_KEY = 'dalbit.reading';

const DEAL_STEP_MS = 26;      // 카드 한 장씩 밀리는 간격
const DEAL_DURATION_MS = 550; // CSS transition 길이와 맞춘다

function start(reading) {
    const deck = document.getElementById('deck');
    const status = document.getElementById('deck-status');
    const guide = document.getElementById('deck-guide');
    const loading = document.getElementById('loading');
    const confirmButton = document.getElementById('confirm');

    const cards = Array.from(deck.querySelectorAll('.card-back'));
    const picked = new Set();
    let submitting = false;

    /* 부채꼴 배치. 가운데를 0도로 두고 좌우로 --spread 만큼 벌린다.
       각도는 CSS 변수로 넘겨서 hover·선택 시 transform 에 그대로 얹을 수 있게 한다. */
    function layOut() {
        const spread = parseFloat(getComputedStyle(deck).getPropertyValue('--spread'));
        const last = cards.length - 1;
        cards.forEach((card, index) => {
            const ratio = last === 0 ? 0.5 : index / last;
            card.style.setProperty('--angle', `${(ratio - 0.5) * spread}deg`);
            card.style.setProperty('--deal-delay', `${index * DEAL_STEP_MS}ms`);
            card.style.zIndex = String(index + 1);
        });
    }

    /* 포개진 상태에서 한 장씩 펼친다.
       transition 이 걸리려면 시작 상태가 먼저 확정돼야 한다. offsetHeight 를 읽어
       강제로 리플로우를 일으키는 방식은 동기라 확실하다. requestAnimationFrame 은
       탭이 백그라운드거나 렌더링이 밀리면 늦게 불려서 펼쳐지지 않는 경우가 있었다. */
    function deal() {
        layOut();
        deck.classList.add('dealing');
        void deck.offsetHeight;
        deck.classList.add('spread');
        setTimeout(() => deck.classList.remove('dealing'),
                DEAL_DURATION_MS + cards.length * DEAL_STEP_MS);
    }

    window.addEventListener('resize', layOut);

    function updateStatus() {
        const remaining = reading.cardCount - picked.size;
        const ready = remaining === 0;
        status.textContent = ready ? '다 골랐어요. 아래 버튼을 눌러 주세요' : `${remaining}장 더 고르세요`;
        confirmButton.disabled = !ready;
    }

    function reset(message) {
        loading.hidden = true;
        guide.hidden = false;
        confirmButton.hidden = false;
        submitting = false;
        picked.clear();
        cards.forEach((card) => {
            card.disabled = false;
            card.classList.remove('picked', 'dimmed');
        });
        updateStatus();
        status.textContent = message;
    }

    async function submit() {
        submitting = true;
        cards.forEach((card) => {
            card.disabled = true;
            if (!card.classList.contains('picked')) {
                card.classList.add('dimmed');
            }
        });
        guide.hidden = true;
        confirmButton.hidden = true;
        loading.hidden = false;

        try {
            const response = await fetch('/api/readings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    categoryCode: reading.categoryCode,
                    spreadType: reading.spreadType,
                    birthDate: reading.birthDate,
                    gender: reading.gender,
                }),
            });

            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new Error(error.message || '카드를 뽑지 못했습니다.');
            }

            const result = await response.json();
            sessionStorage.removeItem(STORAGE_KEY);
            location.href = `/reading/result/${result.shareToken}`;
        } catch (error) {
            reset(error.message);
        }
    }

    cards.forEach((card) => {
        card.addEventListener('click', () => {
            if (submitting) {
                return;
            }
            // 다시 누르면 선택을 푼다
            if (picked.has(card)) {
                picked.delete(card);
                card.classList.remove('picked');
                updateStatus();
                return;
            }
            if (picked.size >= reading.cardCount) {
                status.textContent = `${reading.cardCount}장까지만 고를 수 있어요`;
                return;
            }
            picked.add(card);
            card.classList.add('picked');
            updateStatus();
        });
    });

    confirmButton.addEventListener('click', () => {
        if (picked.size === reading.cardCount) {
            submit();
        }
    });

    deal();
    updateStatus();
}

/* location.replace() 는 스크립트 실행을 멈추지 않는다.
   이어서 진행하면 null 을 파싱하게 되므로 분기로 끊는다. */
const stored = sessionStorage.getItem(STORAGE_KEY);
if (stored) {
    start(JSON.parse(stored));
} else {
    location.replace('/reading/select');
}
