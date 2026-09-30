/* 정보 입력 → sessionStorage 에 담아 뽑기 화면으로 넘긴다.
   생년월일을 쿼리 파라미터로 실으면 액세스 로그와 브라우저 방문 기록에 남는다. */

const STORAGE_KEY = 'dalbit.reading';
const MIN_YEAR = 1900;

const form = document.getElementById('input-form');
const categoryCode = document.getElementById('category-code').value;
const birthInput = document.getElementById('birth-date');
const birthError = document.getElementById('birth-date-error');

/* --- 생년월일 ---
   숫자만 받아 1998.03.15 모양으로 점을 넣어 준다.
   점은 다음 숫자가 있을 때만 붙여서, 지우다가 점에 걸려 커서가 멈추지 않게 한다. */

function formatBirth(digits) {
    if (digits.length <= 4) return digits;
    if (digits.length <= 6) return `${digits.slice(0, 4)}.${digits.slice(4)}`;
    return `${digits.slice(0, 4)}.${digits.slice(4, 6)}.${digits.slice(6, 8)}`;
}

birthInput.addEventListener('input', () => {
    const raw = birthInput.value;
    const caret = birthInput.selectionStart ?? raw.length;
    const digitsBeforeCaret = raw.slice(0, caret).replace(/\D/g, '').length;

    const digits = raw.replace(/\D/g, '').slice(0, 8);
    const formatted = formatBirth(digits);
    birthInput.value = formatted;

    // 가운데를 고쳐도 커서가 끝으로 튀지 않게, 커서 앞 숫자 개수만큼 뒤에 다시 놓는다
    let position = 0;
    for (let seen = 0; position < formatted.length && seen < digitsBeforeCaret; position++) {
        if (/\d/.test(formatted[position])) seen++;
    }
    birthInput.setSelectionRange(position, position);

    clearBirthError();
});

/** 비어 있으면 null, 올바르면 YYYY-MM-DD, 틀리면 오류 문구를 던진다 */
function parseBirth() {
    const digits = birthInput.value.replace(/\D/g, '');
    if (digits.length === 0) return null;
    if (digits.length !== 8) {
        throw new Error('생년월일은 숫자 8자리로 입력해 주세요. 예) 1998.03.15');
    }

    const year = Number(digits.slice(0, 4));
    const month = Number(digits.slice(4, 6));
    const day = Number(digits.slice(6, 8));

    // Date 는 2월 30일을 3월 2일로 넘겨 버리므로 되돌려 비교해 없는 날짜를 거른다
    const date = new Date(year, month - 1, day);
    const exists = date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
    if (!exists) {
        throw new Error('없는 날짜예요. 다시 확인해 주세요.');
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (year < MIN_YEAR || date >= today) {
        throw new Error('생년월일을 다시 확인해 주세요.');
    }

    return `${digits.slice(0, 4)}-${digits.slice(4, 6)}-${digits.slice(6, 8)}`;
}

function showBirthError(message) {
    birthError.textContent = message;
    birthError.hidden = false;
    birthInput.setAttribute('aria-invalid', 'true');
    birthInput.focus();
}

function clearBirthError() {
    birthError.hidden = true;
    birthInput.removeAttribute('aria-invalid');
}

/* --- 제출 --- */

function collect(birthDate) {
    const checked = form.querySelector('input[name="spreadType"]:checked');
    const gender = document.getElementById('gender').value;

    return {
        categoryCode,
        spreadType: checked.value,
        cardCount: Number(checked.dataset.cardCount),
        birthDate,
        gender: gender !== 'NONE' ? gender : null,
    };
}

form.addEventListener('submit', (event) => {
    event.preventDefault();

    let birthDate;
    try {
        birthDate = parseBirth();
    } catch (error) {
        showBirthError(error.message);
        return;
    }

    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(collect(birthDate)));
    location.href = '/reading/draw';
});
