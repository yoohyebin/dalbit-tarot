package kr.dalbittarot.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 카카오톡 공유 설정.
 *
 * JavaScript 키는 브라우저가 SDK 를 초기화할 때 쓰는 키라 페이지 소스에 그대로 노출된다.
 * 비밀값이 아니라서 누가 가져가 쓰는 것은 막지 못하고, 카카오 앱에 등록한 도메인에서만 동작하는 것으로 막는다.
 */
@ConfigurationProperties(prefix = "dalbit.kakao")
public record KakaoProperties(String javascriptKey) {

    public boolean enabled() {
        return javascriptKey != null && !javascriptKey.isBlank();
    }
}
