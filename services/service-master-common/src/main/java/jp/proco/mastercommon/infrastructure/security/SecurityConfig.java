package jp.proco.mastercommon.infrastructure.security;

import jp.proco.mastercommon.infrastructure.tenant.TenantContextFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * セキュリティ設定。
 * <ul>
 *   <li>{@code proco.security.mode=jwt}（既定）: OIDC Resource Server。JWT の cgid でテナント解決</li>
 *   <li>{@code proco.security.mode=dev}: 認証なし。X-Proco-Tenant ヘッダでテナント解決（ローカル専用）</li>
 * </ul>
 * いずれも {@link TenantContextFilter} を認可フィルタの後ろに置き、認証済み principal を参照できるようにする。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/actuator/health", "/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    /**
     * TenantContextFilter は @Component のため Boot がサーブレットフィルタとしても自動登録する。
     * それだと認証前に実行され JWT を参照できないので、自動登録を無効化しセキュリティチェーン内でのみ動かす。
     */
    @Bean
    FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(TenantContextFilter filter) {
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** jwt モードでは起動時に OIDC ディスカバリを行い、設定不備を早期に検知する。 */
    @Bean
    @ConditionalOnProperty(name = "proco.security.mode", havingValue = "jwt", matchIfMissing = true)
    JwtDecoder jwtDecoder(@Value("${proco.security.issuer-uri}") String issuerUri) {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    @ConditionalOnProperty(name = "proco.security.mode", havingValue = "jwt", matchIfMissing = true)
    SecurityFilterChain jwtFilterChain(HttpSecurity http, TenantContextFilter tenantContextFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))
            .addFilterAfter(tenantContextFilter, AuthorizationFilter.class);
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "proco.security.mode", havingValue = "dev")
    SecurityFilterChain devFilterChain(HttpSecurity http, TenantContextFilter tenantContextFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterAfter(tenantContextFilter, AuthorizationFilter.class);
        return http.build();
    }
}
