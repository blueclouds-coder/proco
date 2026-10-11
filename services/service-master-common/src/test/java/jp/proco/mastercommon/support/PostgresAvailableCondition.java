package jp.proco.mastercommon.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * PostgreSQL（Docker または既存サーバー）が使えない環境ではテストを明示的にスキップする。
 * スキップは Surefire のレポートに "Skipped" として出る（静かに通過させない）。
 * CI では必ず Docker を用意し、このスキップが発生しないようにすること。
 */
public final class PostgresAvailableCondition implements ExecutionCondition {

    /** 既存 PostgreSQL の接続情報（環境変数で上書き可）。 */
    public record Local(String url, String user, String password) {
    }

    public static Local local() {
        return new Local(
                env("PROCO_TEST_DB_URL", "jdbc:postgresql://localhost:5432/proco"),
                env("PROCO_TEST_DB_USER", "proco"),
                env("PROCO_TEST_DB_PASSWORD", "proco"));
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            return ConditionEvaluationResult.enabled("Docker あり: Testcontainers を使用");
        }
        Local local = local();
        try (Connection ignored = DriverManager.getConnection(local.url(), local.user(), local.password())) {
            return ConditionEvaluationResult.enabled("Docker なし: 既存 PostgreSQL " + local.url() + " を使用");
        } catch (SQLException e) {
            return ConditionEvaluationResult.disabled(
                    "SKIPPED: Docker も PostgreSQL(" + local.url() + ") も利用できないため DB テストをスキップ");
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? def : v;
    }
}
