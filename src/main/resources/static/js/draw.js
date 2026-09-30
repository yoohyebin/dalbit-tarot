/* 카드 뽑기 연출.
   무엇이 뽑히는지는 서버가 정한다. 여기서 고르는 건 몇 장을 뽑을지와 연출뿐이다. */

const STORAGE_KEY = 'dalbit.reading';

const DEAL_STEP_MS = 26;      // 카드 한 장씩 밀리는 간격
const DEAL_DURATION_MS = 550; // CSS transition 길이와 맞춘다
const PICK_DURATION_MS = 160; // 고르고 푸는 전환 길이. CSS 와 맞춘다

function start(reading) {
    const deck = document.getElementById('deck');
    const status = document.getElementById('deck-status');
    const guide = document.getElementById('deck-guide');
    const loading = document.getElementById('loading');
    const confirmButton = document.getElementById('confirm');

    const cards = Array.from(deck.querySelectorAll('.card-back'));
    const picked = new Set();
    let submitting = false;

    /* 원형 배치. 맨 위를 0도로 두고 양쪽으로 --spread 만큼 두른다.
       각도는 CSS 변수로 넘겨서 hover·선택 시 transform 에 그대로 얹을 수 있게 한다. */
    function layOut() {
        const spread = parseFloat(getComputedStyle(deck).getPropertyValue('--spread'));
        const last = cards.length - 1;
        cards.forEach((card, index) => {
            const ratio = last === 0 ? 0.5 : index / last;
            card.style.setProperty('--angle', `${(ratio - 0.5) * spread}deg`);
            card.style.setProperty('--deal-delay', `${index * DEAL_STEP_MS}ms`);
            /* 인라인 z-index 로 넣으면 스타일시트의 .picked 규칙을 이겨서
               고른 카드가 이웃에 가린 채로 올라간다. 변수로 넘겨 CSS 가 덮을 수 있게 한다 */
            card.style.setProperty('--z', String(index + 1));
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
        release();
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
            /* replace 로 넘긴다. 뽑기는 이미 끝났으므로 결과에서 뒤로 가면
               뽑기 화면이 아니라 정보 입력 화면으로 돌아가야 한다 */
            location.replace(`/reading/result/${result.shareToken}`);
        } catch (error) {
            reset(error.message);
        }
    }

    function toggle(card) {
        if (submitting) {
            return;
        }
        // 다시 누르면 선택을 푼다
        if (picked.has(card)) {
            picked.delete(card);
            card.classList.remove('picked');
            card.classList.add('returning');
            setTimeout(() => card.classList.remove('returning'), PICK_DURATION_MS);
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
    }

    /* 누른 자리의 카드를 원 모양으로 직접 계산한다.
       브라우저에 맡기면(click 대상) 두 가지가 어긋난다.
       - 휴대폰 브라우저는 손가락 면적 안에 버튼이 여러 개면 "가장 그럴듯한" 버튼으로 탭을 옮긴다.
         카드가 겹쳐 있어서 옆 카드로 옮겨지는 일이 생긴다
       - 미리 보여 주려고 들어 올린 카드가 이웃을 덮어서, 밀어도 그 카드에서 벗어나지 않는다
       pointer 이벤트 좌표는 보정 전 원래 좌표라, 이것으로 "화면에 보이는 카드"를 계산한다. */
    const toDeg = (rad) => rad * 180 / Math.PI;

    function geometry() {
        const style = getComputedStyle(deck);
        const sample = cards[0];
        const rect = deck.getBoundingClientRect();
        // 회전 중심은 카드 위 끝에서 transform-origin 세로값만큼 아래다. 브라우저가 px 로 풀어 준 값을 쓴다
        const pivotOffset = parseFloat(getComputedStyle(sample).transformOrigin.split(' ')[1]);
        // offset* 는 회전 전 자리라 기준으로 쓸 수 있다
        return {
            spread: parseFloat(style.getPropertyValue('--spread')),
            pickLift: parseFloat(style.getPropertyValue('--pick-lift')),
            width: sample.offsetWidth,
            height: sample.offsetHeight,
            pivotOffset,
            pivotX: rect.left + rect.width / 2,
            pivotY: rect.top + sample.offsetTop + pivotOffset,
        };
    }

    function cardAt(clientX, clientY) {
        const g = geometry();
        const dx = clientX - g.pivotX;
        const dy = g.pivotY - clientY;
        const radius = Math.hypot(dx, dy);
        const angle = toDeg(Math.atan2(dx, dy));   // 위가 0도, 오른쪽이 +. -180 ~ 180
        const last = cards.length - 1;
        const step = g.spread / last;
        const angleOf = (index) => (index / last - 0.5) * g.spread;
        const inner = g.pivotOffset - g.height;    // 원 중심에서 카드 아래 끝까지
        const outer = g.pivotOffset;               // 원 중심에서 카드 위 끝까지

        // 골라서 들어 올린 카드는 이웃을 통째로 덮고 있으니 먼저 본다
        for (const card of picked) {
            const turn = (angle - angleOf(cards.indexOf(card))) * Math.PI / 180;
            const along = radius * Math.cos(turn);   // 카드 축 방향 거리
            const side = radius * Math.sin(turn);    // 축에서 옆으로 벗어난 거리
            const bottom = inner + g.pickLift;
            if (Math.abs(side) <= g.width / 2 && along >= bottom && along <= bottom + g.height) {
                return card;
            }
        }

        if (radius < inner - 12 || radius > outer + 12) {
            return null;
        }

        /* 카드마다 보이는 곳은 왼쪽 끝부터 한 칸 폭이다. 나머지는 오른쪽(시계 방향) 카드에 덮여 있다.
           edge 는 카드 반폭이 이 거리에서 차지하는 각도, 즉 축에서 왼쪽 끝까지의 각도다 */
        const r = Math.min(Math.max(radius, inner), outer);
        const edge = toDeg(Math.asin(Math.min(1, g.width / 2 / r)));
        const indexAt = (a) => {
            if (a < -g.spread / 2 - edge || a > g.spread / 2 + edge) {
                return -1;
            }
            return Math.min(last, Math.max(0, Math.floor((a + edge + g.spread / 2) / step)));
        };

        /* 원 아래쪽 틈 근처에서는 첫 카드와 마지막 카드가 ±180도 경계를 사이에 두고 겹친다.
           같은 자리를 360도 돌린 각도로도 보고, 더 위에 놓인(번호가 큰) 카드를 고른다 */
        const index = Math.max(indexAt(angle), indexAt(angle - 360), indexAt(angle + 360));
        return index < 0 ? null : cards[index];
    }

    /* 누른 채로 원을 따라 밀면 손가락 아래 카드가 살짝 올라와 미리 보이고, 떼면 그 카드를 고른다.
       한 장이 차지하는 폭이 손가락보다 좁아서, 떼기 전에 어느 카드인지 확인할 수 있게 한다. */
    let aimed = null;
    let activePointer = null;

    function aim(card) {
        if (aimed === card) {
            return;
        }
        aimed?.classList.remove('aimed');
        aimed = card;
        aimed?.classList.add('aimed');
    }

    function release() {
        activePointer = null;
        deck.classList.remove('aiming');
        aim(null);
    }

    deck.addEventListener('pointerdown', (event) => {
        if (submitting || !event.isPrimary || event.button > 0) {
            return;
        }
        activePointer = event.pointerId;
        if (event.pointerType !== 'mouse') {
            deck.classList.add('aiming');
        }
        aim(cardAt(event.clientX, event.clientY));
    });

    deck.addEventListener('pointermove', (event) => {
        if (submitting) {
            return;
        }
        // 마우스는 누르지 않아도 가리키는 카드를 밝힌다. 터치는 누른 동안에만 움직임이 온다
        if (event.pointerType === 'mouse' || event.pointerId === activePointer) {
            aim(cardAt(event.clientX, event.clientY));
        }
    });

    deck.addEventListener('pointerup', (event) => {
        if (event.pointerId !== activePointer) {
            return;
        }
        const card = cardAt(event.clientX, event.clientY);
        const isMouse = event.pointerType === 'mouse';
        activePointer = null;
        deck.classList.remove('aiming');
        if (!isMouse) {
            aim(null);
        }
        if (card) {
            toggle(card);
        }
    });

    // 세로로 쓸어 스크롤로 넘어가면 브라우저가 취소를 보낸다. 이때는 고르지 않는다
    deck.addEventListener('pointercancel', release);
    deck.addEventListener('pointerleave', (event) => {
        if (event.pointerType === 'mouse') {
            aim(null);
        }
    });

    /* 키보드(Tab → Enter/Space)로 고르는 경우. 포인터로 누른 클릭은 위에서 처리했으므로
       detail 이 0 인 키보드 클릭만 받는다 */
    cards.forEach((card) => {
        card.addEventListener('click', (event) => {
            if (event.detail === 0) {
                toggle(card);
            }
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

/* 뒤로·앞으로 이동으로 돌아온 경우.
   브라우저가 DOM 을 그대로 복원해서(bfcache) 제출 중이던 로딩 화면이 그대로 남는다.
   이때는 스크립트도 다시 실행되지 않으므로 pageshow 에서 직접 처리한다. */
window.addEventListener('pageshow', (event) => {
    if (!event.persisted) {
        return;
    }
    if (sessionStorage.getItem(STORAGE_KEY)) {
        location.reload();   // 아직 뽑지 않았다면 처음부터 다시 고르게 한다
    } else {
        location.replace('/reading/select');
    }
});

/* location.replace() 는 스크립트 실행을 멈추지 않는다.
   이어서 진행하면 null 을 파싱하게 되므로 분기로 끊는다. */
const stored = sessionStorage.getItem(STORAGE_KEY);
if (stored) {
    start(JSON.parse(stored));
} else {
    location.replace('/reading/select');
}
