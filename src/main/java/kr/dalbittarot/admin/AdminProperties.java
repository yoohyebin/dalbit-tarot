package kr.dalbittarot.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 계정. 값은 환경변수로만 받는다.
 *
 * @param username 로그인 아이디
 * @param password 비워 두면 기동 때 무작위로 만들어 로그에 찍는다.
 *                 기본값을 소스에 두면 저장소를 공개하는 순간 그대로 뚫린다.
 */
@ConfigurationProperties(prefix = "dalbit.admin")
public record AdminProperties(String username, String password) {

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }

    public String usernameOrDefault() {
        return username == null || username.isBlank() ? "admin" : username;
    }
}
