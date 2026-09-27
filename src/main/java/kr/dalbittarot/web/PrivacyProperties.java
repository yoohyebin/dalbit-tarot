package kr.dalbittarot.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 개인정보처리방침에 표기할 운영자 정보.
 * 공개 페이지에 그대로 노출되므로 소스에 박지 않고 설정으로 뺀다.
 */
@ConfigurationProperties(prefix = "dalbit.privacy")
public record PrivacyProperties(String ownerName, String contactEmail) {

    public String ownerNameOrPlaceholder() {
        return blank(ownerName) ? "미기재" : ownerName;
    }

    public String contactEmailOrPlaceholder() {
        return blank(contactEmail) ? "미기재" : contactEmail;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
