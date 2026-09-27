/* 정보 입력 → sessionStorage 에 담아 뽑기 화면으로 넘긴다.
   생년월일을 쿼리 파라미터로 실으면 액세스 로그와 브라우저 방문 기록에 남는다. */

const STORAGE_KEY = 'dalbit.reading';

const form = document.getElementById('input-form');
const categoryCode = document.getElementById('category-code').value;

function collect(includeOptional) {
    const checked = form.querySelector('input[name="spreadType"]:checked');
    const gender = document.getElementById('gender').value;
    const birthDate = document.getElementById('birth-date').value;

    return {
        categoryCode,
        spreadType: checked.value,
        cardCount: Number(checked.dataset.cardCount),
        birthDate: includeOptional && birthDate ? birthDate : null,
        gender: includeOptional && gender !== 'NONE' ? gender : null,
    };
}

function go(includeOptional) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(collect(includeOptional)));
    location.href = '/reading/draw';
}

form.addEventListener('submit', (event) => {
    event.preventDefault();
    go(true);
});

document.getElementById('skip').addEventListener('click', () => go(false));
