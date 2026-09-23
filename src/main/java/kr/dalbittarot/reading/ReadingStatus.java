package kr.dalbittarot.reading;

public enum ReadingStatus {

    /** 카드만 뽑힌 상태 */
    DRAWN,
    /** LLM 해석 요청 중 */
    INTERPRETING,
    /** 해석까지 완료 */
    COMPLETED,
    /** 해석 실패 (타임아웃·오류) */
    FAILED
}
