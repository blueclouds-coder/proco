package jp.proco.mastercommon.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL 実物を使うテストの基底クラス。
 * <ol>
 *   <li>Docker があれば Testcontainers（postgres:17）を起動する</li>
 *   <li>なければ {@code PROCO_TEST_DB_URL}（既定 jdbc:postgresql://localhost:5432/proco, proco/proco。
 *       docker compose の PostgreSQL）に接続し、テスト用 DB {@code proco_test} を作って使う</li>
 *   <li>どちらも使えなければ {@link PostgresAvailableCondition} がテストを「スキップ」として報告する（失敗ではない）</li>
 * </ol>
 * スーパーユーザーは RLS を迂回するため、Flyway はスーパーユーザーで実行し、
 * アプリの DataSource は非所有者ロール {@code proco_app} で接続する（本番と同じ構成）。
 */
@ExtendWith(PostgresAvailableCondition.class)
@ActiveProfiles("test")
public abstract class PostgresTestSupport {

    static final String APP_ROLE = "proco_app";
    static final String APP_PASSWORD = "proco_app";

    private static final PostgreSQLContainer<?> CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17")).withDatabaseName("proco").withUsername("proco").withPassword("proco");

    /** 管理者（Flyway 実行用）接続情報。 */
    private static String adminUrl;
    private static String adminUser;
    private static String adminPassword;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        resolveAdminConnection();
        migrateAndCreateAppRole();
        registry.add("spring.datasource.url", () -> adminUrl);
        registry.add("spring.datasource.username", () -> APP_ROLE);
        registry.add("spring.datasource.password", () -> APP_PASSWORD);
        // マイグレーションはここで実行済み。アプリ側（非所有者ロール）では走らせない
        registry.add("spring.flyway.enabled", () -> "false");
    }

    private static void resolveAdminConnection() {
        if (adminUrl != null) {
            return;
        }
        if (DockerClientFactory.instance().isDockerAvailable()) {
            CONTAINER.start();
            adminUrl = CONTAINER.getJdbcUrl();
            adminUser = CONTAINER.getUsername();
            adminPassword = CONTAINER.getPassword();
            return;
        }
        PostgresAvailableCondition.Local local = PostgresAvailableCondition.local();
        adminUser = local.user();
        adminPassword = local.password();
        // 既存 DB を汚さないよう、テスト専用 DB を作って使う
        try (Connection c = DriverManager.getConnection(local.url(), adminUser, adminPassword);
                Statement st = c.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS proco_test");
            st.execute("CREATE DATABASE proco_test");
        } catch (SQLException e) {
            throw new IllegalStateException("テスト用 DB を作成できません: " + local.url(), e);
        }
        adminUrl = local.url().replaceAll("/[^/?]+(\\?.*)?$", "/proco_test$1");
    }

    private static void migrateAndCreateAppRole() {
        Flyway.configure()
                .dataSource(adminUrl, adminUser, adminPassword)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        try (Connection c = DriverManager.getConnection(adminUrl, adminUser, adminPassword);
                Statement st = c.createStatement()) {
            st.execute("""
                    DO $$ BEGIN
                      IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '%s') THEN
                        CREATE ROLE %s LOGIN PASSWORD '%s';
                      END IF;
                    END $$""".formatted(APP_ROLE, APP_ROLE, APP_PASSWORD));
            st.execute("GRANT USAGE ON SCHEMA public TO " + APP_ROLE);
            st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO " + APP_ROLE);
        } catch (SQLException e) {
            throw new IllegalStateException("アプリ用ロールの作成に失敗しました", e);
        }
    }
}
