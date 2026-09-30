package kr.dalbittarot.web;

import kr.dalbittarot.reading.dto.DrawnCardResponse;
import kr.dalbittarot.reading.dto.ReadingResponse;

import java.util.stream.Collectors;

/**
 * 공유 미리보기에 들어가는 값. 카카오톡 공유 메시지와 og 태그가 같은 값을 쓴다.
 *
 * 관심 분야와 카드 이름만 넣는다. 생년월일은 넣지 않고, 해석 본문도 넣지 않는다.
 * 링크를 받은 사람이 결과 페이지를 열어야 본문을 볼 수 있다.
 *
 * @param url      결과 페이지 절대 주소
 * @param imageUrl 미리보기 이미지 절대 주소. 카카오톡 공유가 JPG·PNG 만 안내하고 있어 WebP 카드 이미지를 쓰지 않는다
 */
public record SharePreview(
        String title,
        String description,
        String url,
        String imageUrl,
        int imageWidth,
        int imageHeight
) {

    /** tools/make_share_image.py 로 만든 이미지. 크기를 바꾸면 여기도 맞춘다 */
    static final String IMAGE_PATH = "/images/share.jpg";
    static final int IMAGE_WIDTH = 800;
    static final int IMAGE_HEIGHT = 400;

    static SharePreview of(ReadingResponse reading, String baseUrl) {
        String cardNames = reading.cards().stream()
                .map(DrawnCardResponse::nameKo)
                .collect(Collectors.joining(" · "));

        return new SharePreview(
                reading.categoryLabel() + " 타로 결과",
                cardNames + " 카드가 나왔어요",
                baseUrl + "/reading/result/" + reading.shareToken(),
                baseUrl + IMAGE_PATH,
                IMAGE_WIDTH,
                IMAGE_HEIGHT
        );
    }
}
