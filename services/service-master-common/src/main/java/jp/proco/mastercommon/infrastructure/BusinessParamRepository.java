package jp.proco.mastercommon.infrastructure;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.table;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jp.proco.mastercommon.domain.BusinessParam;
import jp.proco.mastercommon.domain.BusinessParam.ValueType;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

/**
 * business_params テーブルへのアクセス。jOOQ の DSLContext を codegen なしで使う。
 * <p>
 * company_group_id は呼び出し側（Service）が TenantContext から渡す。WHERE 句にも必ず含めるが、
 * 真の分離は DB の RLS（app.tenant）が担う。
 */
@Repository
public class BusinessParamRepository {

    private static final Table<Record> T = table("business_params");
    private static final Field<UUID> ID = field("id", UUID.class);
    private static final Field<UUID> COMPANY_GROUP_ID = field("company_group_id", UUID.class);
    private static final Field<String> PARAM_KEY = field("param_key", String.class);
    private static final Field<String> VALUE_TYPE = field("value_type", String.class);
    private static final Field<JSONB> VALUE = field("value", JSONB.class);
    private static final Field<String> DESCRIPTION = field("description", String.class);
    private static final Field<String> CREATED_BY = field("created_by", String.class);
    private static final Field<OffsetDateTime> UPDATED_AT = field("updated_at", OffsetDateTime.class);
    private static final Field<String> UPDATED_BY = field("updated_by", String.class);
    private static final Field<Integer> VERSION = field("version", Integer.class);

    private final DSLContext dsl;

    public BusinessParamRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<BusinessParam> findAll(UUID companyGroupId) {
        return dsl.select(ID, COMPANY_GROUP_ID, PARAM_KEY, VALUE_TYPE, VALUE, DESCRIPTION, UPDATED_AT, UPDATED_BY, VERSION)
                .from(T)
                .where(COMPANY_GROUP_ID.eq(companyGroupId))
                .orderBy(PARAM_KEY)
                .fetch(BusinessParamRepository::toDomain);
    }

    public Optional<BusinessParam> findByKey(UUID companyGroupId, String paramKey) {
        return dsl.select(ID, COMPANY_GROUP_ID, PARAM_KEY, VALUE_TYPE, VALUE, DESCRIPTION, UPDATED_AT, UPDATED_BY, VERSION)
                .from(T)
                .where(COMPANY_GROUP_ID.eq(companyGroupId), PARAM_KEY.eq(paramKey))
                .fetchOptional(BusinessParamRepository::toDomain);
    }

    /** INSERT ... ON CONFLICT で upsert。更新時は version を +1 する。 */
    public BusinessParam upsert(UUID companyGroupId, String paramKey, ValueType valueType, String valueJson,
            String description, String actor) {
        JSONB jsonb = JSONB.jsonb(valueJson);
        dsl.insertInto(T)
                .set(COMPANY_GROUP_ID, companyGroupId)
                .set(PARAM_KEY, paramKey)
                .set(VALUE_TYPE, valueType.name())
                .set(VALUE, jsonb)
                .set(DESCRIPTION, description)
                .set(CREATED_BY, actor)
                .set(UPDATED_BY, actor)
                .onConflict(COMPANY_GROUP_ID, PARAM_KEY)
                .doUpdate()
                .set(VALUE_TYPE, valueType.name())
                .set(VALUE, jsonb)
                .set(DESCRIPTION, description)
                .set(UPDATED_AT, field("now()", OffsetDateTime.class))
                .set(UPDATED_BY, actor)
                .set(VERSION, VERSION.plus(inline(1)))
                .execute();
        return findByKey(companyGroupId, paramKey)
                .orElseThrow(() -> new IllegalStateException("upsert 直後に行が見つかりません: " + paramKey));
    }

    private static BusinessParam toDomain(Record r) {
        return new BusinessParam(
                r.get(ID),
                r.get(COMPANY_GROUP_ID),
                r.get(PARAM_KEY),
                ValueType.valueOf(r.get(VALUE_TYPE)),
                r.get(VALUE).data(),
                r.get(DESCRIPTION),
                r.get(UPDATED_AT),
                r.get(UPDATED_BY),
                r.get(VERSION));
    }
}
