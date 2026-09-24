package kr.dalbittarot.reading;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 결과 공유용 토큰 생성기.
 *
 * 회원이 없어 이 토큰만 알면 결과를 볼 수 있으므로 추측 가능한 값을 쓰면 안 된다.
 * 128비트를 URL-safe Base64 로 인코딩해 22자를 만든다 (reading_session.share_token CHAR(22)).
 */
@Component
public class ShareTokenGenerator {

    private static final int BYTE_LENGTH = 16;

    private final SecureRandom random = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public String generate() {
        byte[] buffer = new byte[BYTE_LENGTH];
        random.nextBytes(buffer);
        return encoder.encodeToString(buffer);
    }
}
