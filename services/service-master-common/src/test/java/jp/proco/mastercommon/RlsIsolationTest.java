package jp.proco.mastercommon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.UUID;
import jp.proco.mastercommon.application.BusinessParamService;
import jp.proco.mastercommon.domain.BusinessParam;
import jp.proco.mastercommon.domain.BusinessParam.ValueType;
import jp.proco.mastercommon.infrastructure.tenant.TenantContext;
import jp.proco.mastercommon.support.PostgresTestSupport;
import jp.proco.mastercommon.support.TenantSqlRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RLS によるテナント分離の検証。アプリは非所有者ロール proco_app で接続する（スーパーユーザーは RLS を迂回するため）。
 */
@SpringBootTest
class RlsIsolationTest extends PostgresTestSupport {

    @Autowired
    BusinessParamService service;

    @Autowired
    TenantSqlRunner sql;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void otherTenantCannotSeeOrUpdateRows() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        String key = "rls.test." + UUID.randomUUID().toString().substring(0, 8);

        // テナント A で登録
        BusinessParam created = TenantContext.runAs(tenantA,
                () -> service.upsert(key, ValueType.INT, IntNode.valueOf(5), "A の値", "tester"));
        assertThat(created.companyGroupId()).isEqualTo(tenantA);

        // テナント B からは見えない
        TenantContext.runAs(tenantB, () ->
                assertThat(service.list()).extracting(BusinessParam::paramKey).doesNotContain(key));

        // テナント B は A の行を更新できない（RLS で 0 行更新）。company_group_id を直接書いても WITH CHECK で拒否される
        TenantContext.runAs(tenantB, () -> {
            int updated = sql.update("UPDATE business_params SET description = 'hacked' WHERE param_key = ?", key);
            assertThat(updated).isZero();
            assertThatThrownBy(() -> sql.update(
                    "INSERT INTO business_params(company_group_id, param_key, value_type, value) VALUES (?, ?, 'STRING', '\"x\"'::jsonb)",
                    tenantA, key + ".b"))
                    .rootCause().hasMessageContaining("row-level security");
        });

        // テナント A からは見える
        TenantContext.runAs(tenantA, () -> {
            assertThat(service.list()).extracting(BusinessParam::paramKey).contains(key);
            BusinessParam updated = service.upsert(key, ValueType.STRING, TextNode.valueOf("v2"), "更新", "tester");
            assertThat(updated.value()).isEqualTo("\"v2\"");
            assertThat(updated.version()).isEqualTo(1);
        });
    }

    @Test
    void withoutTenantNothingIsVisible() throws Exception {
        // app.tenant 未設定（トランザクション外・プール接続）では NULLIF(...) が NULL となり、どの行にも一致しない
        TenantContext.runAs(UUID.randomUUID(),
                () -> service.upsert("rls.novisible", ValueType.BOOL, BooleanNode.TRUE, null, "t"));
        Integer count = jdbc.queryForObject("SELECT count(*) FROM business_params", Integer.class);
        assertThat(count).isZero();
    }
}
