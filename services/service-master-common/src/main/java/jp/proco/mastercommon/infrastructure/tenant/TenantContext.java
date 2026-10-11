package jp.proco.mastercommon.infrastructure.tenant;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * リクエスト処理中のテナント（company_group_id）を保持する ThreadLocal ホルダ。
 * 値は {@link TenantContextFilter} が設定し、{@link TenantTransactionAspect} が
 * {@code SET LOCAL app.tenant} に使う。業務コードはリクエストから直接テナントを取らず、必ずここから取得する。
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    /** 現在のテナント。未設定なら empty。 */
    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /** 現在のテナント。未設定なら例外（バッチ等で設定し忘れた場合に早期検知する）。 */
    public static UUID require() {
        UUID id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("テナントコンテキストが設定されていません");
        }
        return id;
    }

    public static void set(UUID companyGroupId) {
        CURRENT.set(companyGroupId);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** 指定テナントで処理を実行し、終了後に元の値へ戻す（テスト・バッチ用）。 */
    public static <T> T runAs(UUID companyGroupId, Callable<T> action) throws Exception {
        UUID previous = CURRENT.get();
        CURRENT.set(companyGroupId);
        try {
            return action.call();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /** 戻り値のない版。 */
    public static void runAs(UUID companyGroupId, Runnable action) {
        try {
            runAs(companyGroupId, () -> {
                action.run();
                return null;
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
