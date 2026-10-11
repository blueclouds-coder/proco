package jp.proco.mastercommon.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 業務パラメータ（キー＋型付き値）。value は JSON テキストとして保持する。 */
public record BusinessParam(
        UUID id,
        UUID companyGroupId,
        String paramKey,
        ValueType valueType,
        String value,
        String description,
        OffsetDateTime updatedAt,
        String updatedBy,
        int version) {

    /** 値の型。DB の CHECK 制約と一致させる。 */
    public enum ValueType { STRING, INT, DECIMAL, BOOL, JSON }
}
