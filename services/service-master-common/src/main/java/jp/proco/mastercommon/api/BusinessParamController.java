package jp.proco.mastercommon.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import jp.proco.mastercommon.application.BusinessParamService;
import jp.proco.mastercommon.domain.BusinessParam;
import jp.proco.mastercommon.domain.BusinessParam.ValueType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 業務パラメータ API。契約は services/service-master-common/openapi.yaml を正とする。 */
@RestController
@Validated
@RequestMapping("/api/v1/config/params")
public class BusinessParamController {

    private final BusinessParamService service;
    private final ObjectMapper objectMapper;

    public BusinessParamController(BusinessParamService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<BusinessParamResponse> list() {
        return service.list().stream().map(this::toResponse).toList();
    }

    @PutMapping("/{key}")
    public BusinessParamResponse upsert(
            @PathVariable("key") @Pattern(regexp = "[a-z0-9_.]{1,100}") String key,
            @Valid @RequestBody BusinessParamRequest request,
            Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "dev";
        BusinessParam saved = service.upsert(key, request.valueType(), request.value(), request.description(), actor);
        return toResponse(saved);
    }

    private BusinessParamResponse toResponse(BusinessParam p) {
        try {
            JsonNode value = objectMapper.readTree(p.value());
            return new BusinessParamResponse(p.id(), p.paramKey(), p.valueType(), value, p.description(),
                    p.updatedAt(), p.updatedBy(), p.version());
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("DB の jsonb が不正です: " + p.paramKey(), e);
        }
    }

    /** PUT ボディ。 */
    public record BusinessParamRequest(
            @NotNull ValueType valueType,
            @NotNull JsonNode value,
            @Size(max = 500) String description) {
    }

    /** レスポンス。company_group_id は返さない（テナントは呼び出し側が知っている）。 */
    public record BusinessParamResponse(
            UUID id,
            String key,
            ValueType valueType,
            JsonNode value,
            String description,
            OffsetDateTime updatedAt,
            String updatedBy,
            int version) {
    }
}
