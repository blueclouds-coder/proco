package jp.proco.mastercommon.infrastructure.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * リクエストからテナントを解決して {@link TenantContext} に設定するフィルタ。
 * <ul>
 *   <li>通常: 認証済み JWT の {@code cgid} クレーム</li>
 *   <li>ローカル開発のみ（{@code proco.security.dev-header-tenant=true}）: {@code X-Proco-Tenant} ヘッダ</li>
 * </ul>
 * {@code /api/**} でテナントが解決できなければ 400 を返す。
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String CLAIM_CGID = "cgid";
    public static final String HEADER_TENANT = "X-Proco-Tenant";

    private final boolean devHeaderTenant;

    public TenantContextFilter(@Value("${proco.security.dev-header-tenant:false}") boolean devHeaderTenant) {
        this.devHeaderTenant = devHeaderTenant;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<UUID> tenant = fromJwt().or(() -> fromHeader(request));
        if (tenant.isEmpty() && request.getRequestURI().startsWith("/api/")) {
            writeProblem(response);
            return;
        }
        try {
            tenant.ifPresent(TenantContext::set);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Optional<UUID> fromJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        return parse(jwt.getClaimAsString(CLAIM_CGID));
    }

    private Optional<UUID> fromHeader(HttpServletRequest request) {
        if (!devHeaderTenant) {
            return Optional.empty();
        }
        return parse(request.getHeader(HEADER_TENANT));
    }

    private static Optional<UUID> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** RFC 9457 形式で 400 を返す（ControllerAdvice より手前で止まるため自前で書く）。 */
    private static void writeProblem(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"type":"about:blank","title":"Bad Request","status":400,\
                "code":"TENANT_MISSING","messageKey":"error.tenant.missing"}""");
    }
}
