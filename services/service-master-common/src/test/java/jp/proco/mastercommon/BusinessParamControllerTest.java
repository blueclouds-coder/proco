package jp.proco.mastercommon;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import jp.proco.mastercommon.infrastructure.tenant.TenantContextFilter;
import jp.proco.mastercommon.support.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** dev モード（X-Proco-Tenant ヘッダ）での API 結合テスト。 */
@SpringBootTest
@AutoConfigureMockMvc
class BusinessParamControllerTest extends PostgresTestSupport {

    @Autowired
    MockMvc mvc;

    @Test
    void putThenGetReturnsValueForTenant() throws Exception {
        String tenant = UUID.randomUUID().toString();
        String other = UUID.randomUUID().toString();

        mvc.perform(put("/api/v1/config/params/quote.max_suppliers")
                        .header(TenantContextFilter.HEADER_TENANT, tenant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valueType":"INT","value":10,"description":"見積依頼先上限"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("quote.max_suppliers"))
                .andExpect(jsonPath("$.value").value(10))
                .andExpect(jsonPath("$.version").value(0));

        mvc.perform(get("/api/v1/config/params").header(TenantContextFilter.HEADER_TENANT, tenant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].key", hasItem("quote.max_suppliers")))
                .andExpect(jsonPath("$[?(@.key=='quote.max_suppliers')].value", hasItem(10)));

        // 別テナントには見えない
        mvc.perform(get("/api/v1/config/params").header(TenantContextFilter.HEADER_TENANT, other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void missingTenantIs400Problem() throws Exception {
        mvc.perform(get("/api/v1/config/params"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_MISSING"));
    }

    @Test
    void typeMismatchIs400Problem() throws Exception {
        mvc.perform(put("/api/v1/config/params/some.flag")
                        .header(TenantContextFilter.HEADER_TENANT, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valueType":"BOOL","value":"yes"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAM_VALUE_TYPE_MISMATCH"))
                .andExpect(jsonPath("$.messageKey").value("error.param.valueTypeMismatch"));
    }
}
