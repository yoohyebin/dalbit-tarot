package kr.dalbittarot.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * /admin 아래만 잠그고 나머지는 전부 연다.
 *
 * 관리자 한 명뿐이라 DB 에 사용자 테이블을 두지 않고 메모리에 하나만 만든다.
 */
@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/admin/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .defaultSuccessUrl("/admin/feedback", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/"))
                // 공개 API 는 세션을 쓰지 않아 CSRF 토큰이 막아 줄 것이 없다.
                // 관리자 화면의 폼(로그인·로그아웃)에는 그대로 적용된다.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AdminProperties properties, PasswordEncoder encoder) {
        String password = properties.hasPassword() ? properties.password() : generateAndWarn();
        return new InMemoryUserDetailsManager(
                User.withUsername(properties.usernameOrDefault())
                        .password(encoder.encode(password))
                        .roles("ADMIN")
                        .build());
    }

    /** 비밀번호를 설정하지 않았을 때. 알려진 기본값을 두는 것보다 매번 바뀌는 편이 낫다. */
    private String generateAndWarn() {
        byte[] buffer = new byte[12];
        new SecureRandom().nextBytes(buffer);
        String generated = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
        log.warn("""
                dalbit.admin.password 가 비어 있어 이번 기동에만 쓸 비밀번호를 만들었다: {}
                재시작하면 바뀐다. 운영에서는 ADMIN_PASSWORD 환경변수를 설정할 것.""", generated);
        return generated;
    }
}
