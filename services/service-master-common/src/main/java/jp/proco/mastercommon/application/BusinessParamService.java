package jp.proco.mastercommon.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import jp.proco.mastercommon.domain.BusinessParam;
import jp.proco.mastercommon.domain.BusinessParam.ValueType;
import jp.proco.mastercommon.domain.ProcoException;
import jp.proco.mastercommon.infrastructure.BusinessParamRepository;
import jp.proco.mastercommon.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 業務パラメータのユースケース。テナントは必ず {@link TenantContext} から取得する（リクエストからは受け取らない）。
 */
@Service
public class BusinessParamService {

    private final BusinessParamRepository repository;

    public BusinessParamService(BusinessParamRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BusinessParam> list() {
        return repository.findAll(TenantContext.require());
    }

    @Transactional
    public BusinessParam upsert(String paramKey, ValueType valueType, JsonNode value, String description, String actor) {
        validateValue(valueType, value);
        UUID tenant = TenantContext.require();
        return repository.upsert(tenant, paramKey, valueType, value.toString(), description, actor);
    }

    /** value_type と JSON 値の型が一致していることを検査する。 */
    static void validateValue(ValueType type, JsonNode value) {
        boolean ok = switch (type) {
            case STRING -> value.isTextual();
            case INT -> value.isIntegralNumber();
            case DECIMAL -> value.isNumber();
            case BOOL -> value.isBoolean();
            case JSON -> !value.isNull();
        };
        if (!ok) {
            throw new ProcoException("PARAM_VALUE_TYPE_MISMATCH", "error.param.valueTypeMismatch",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
