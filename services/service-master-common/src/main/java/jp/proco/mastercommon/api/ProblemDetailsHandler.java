package jp.proco.mastercommon.api;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import jp.proco.mastercommon.domain.ProcoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 例外を RFC 9457 Problem Details に変換する。拡張フィールドとして code / messageKey を必ず付ける。
 * 内部エラーの詳細（スタックトレース・SQL）はレスポンスに含めない。
 */
@RestControllerAdvice
public class ProblemDetailsHandler {

    @ExceptionHandler(ProcoException.class)
    public ProblemDetail handleProco(ProcoException e) {
        return problem(e.status(), e.code(), e.messageKey());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "error.validation");
        List<String> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        pd.setProperty("errors", fields);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(ConstraintViolationException e) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "error.validation");
        pd.setProperty("errors", e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage()).toList());
        return pd;
    }

    private static ProblemDetail problem(HttpStatus status, String code, String messageKey) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code);
        pd.setProperty("messageKey", messageKey);
        return pd;
    }
}
