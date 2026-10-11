package jp.proco.mastercommon.domain;

import org.springframework.http.HttpStatus;

/**
 * Proco 共通の業務例外。API では RFC 9457 Problem Details（code, messageKey 付き）に変換される。
 * messageKey は i18n リソースのキー（文言は UI 側で解決する）。
 */
public class ProcoException extends RuntimeException {

    private final String code;
    private final String messageKey;
    private final HttpStatus status;

    public ProcoException(String code, String messageKey, HttpStatus status) {
        super(code + " (" + messageKey + ")");
        this.code = code;
        this.messageKey = messageKey;
        this.status = status;
    }

    public ProcoException(String code, String messageKey) {
        this(code, messageKey, HttpStatus.BAD_REQUEST);
    }

    public String code() {
        return code;
    }

    public String messageKey() {
        return messageKey;
    }

    public HttpStatus status() {
        return status;
    }
}
