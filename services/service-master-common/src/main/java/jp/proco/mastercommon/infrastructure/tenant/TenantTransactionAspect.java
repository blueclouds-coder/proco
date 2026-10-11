package jp.proco.mastercommon.infrastructure.tenant;

import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code jp.proco..} 配下の {@code @Transactional} メソッドの先頭で
 * {@code SET LOCAL app.tenant = '<uuid>'} を実行し、RLS ポリシーにテナントを渡す。
 * <p>
 * Spring の TransactionInterceptor（既定 order = LOWEST_PRECEDENCE）より先に入るため、
 * トランザクションが未開始ならここで {@link TransactionTemplate} により開始する。
 * 既に開始済み（ネスト呼び出し）の場合は同じトランザクション上で再度 SET LOCAL するだけ（冪等）。
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantTransactionAspect {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public TenantTransactionAspect(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Around("within(jp.proco..*) && ("
            + "@annotation(org.springframework.transaction.annotation.Transactional) || "
            + "@within(org.springframework.transaction.annotation.Transactional))")
    public Object bindTenant(ProceedingJoinPoint pjp) throws Throwable {
        UUID tenant = TenantContext.require();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            setLocalTenant(tenant);
            return pjp.proceed();
        }
        // トランザクション未開始: ここで開始し、SET LOCAL が同一トランザクション内に収まるようにする
        return transactionTemplate.execute(status -> {
            setLocalTenant(tenant);
            try {
                return pjp.proceed();
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new TenantTransactionException(t);
            }
        });
    }

    private void setLocalTenant(UUID tenant) {
        // set_config は値をバインド可能（SET LOCAL はリテラルのみのため）。第3引数 true = トランザクション限定
        jdbcTemplate.queryForObject("SELECT set_config('app.tenant', ?, true)", String.class, tenant.toString());
    }

    /** チェック例外を TransactionTemplate 越しに運ぶためのラッパ。 */
    static final class TenantTransactionException extends RuntimeException {
        TenantTransactionException(Throwable cause) {
            super(cause);
        }
    }
}
