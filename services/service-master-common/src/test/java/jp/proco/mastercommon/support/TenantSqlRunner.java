package jp.proco.mastercommon.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * テスト用: 任意 SQL を「テナントが束縛されたトランザクション」内で実行する。
 * jp.proco 配下の @Transactional のため TenantTransactionAspect が app.tenant を設定する。
 */
@Component
public class TenantSqlRunner {

    private final JdbcTemplate jdbc;

    public TenantSqlRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public int update(String sql, Object... args) {
        return jdbc.update(sql, args);
    }

    @Transactional(readOnly = true)
    public <T> T queryForObject(String sql, Class<T> type, Object... args) {
        return jdbc.queryForObject(sql, type, args);
    }
}
