/* 카드 뽑기 연출.
   무엇이 뽑히는지는 서버가 정한다. 여기서 고르는 건 몇 장을 뽑을지와 연출뿐이다. */

const STORAGE_KEY = 'dalbit.reading';

function start(reading) {
    const deck = document.getElementById('deck');
    const status = document.getElementById('deck-status');
    const guide = document.getElementById('deck-guide');
    const loading = document.getElementById('loading');

    const cards = Array.from(deck.querySelectorAll('.card-back'));
    const picked = new Set();
    let submitting = false;

    function updateStatus() {
        const remaining = reading.cardCount - picked.size;
        status.textContent = remaining > 0 ? `${remaining}장 더 고르세요` : '카드를 펼칩니다…';
    }

    function reset(message) {
        loading.hidden = true;
        guide.hidden = false;
        status.textContent = message;
        submitting = false;
        picked.clear();
        cards.forEach((card) => {
            card.disabled = false;
            card.classList.remove('picked', 'dimmed');
        });
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
            if (submitting || picked.has(card)) {
                return;
            }
            picked.add(card);
            card.classList.add('picked');
            updateStatus();

            if (picked.size === reading.cardCount) {
                // 마지막 카드가 올라오는 게 보이도록 조금 기다린다
                setTimeout(submit, 450);
            }
        });
    });

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
