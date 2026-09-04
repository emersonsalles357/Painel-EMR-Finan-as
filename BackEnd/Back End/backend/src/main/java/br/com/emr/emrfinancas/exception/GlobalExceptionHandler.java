package br.com.emr.emrfinancas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarNaoEncontrado(RecursoNaoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro(exception.getMessage(), HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> tratarRegraNegocio(RegraNegocioException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro(exception.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<Map<String, Object>> tratarEmailJaCadastrado(EmailJaCadastradoException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro(exception.getMessage(), HttpStatus.CONFLICT));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarJsonInvalido(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(erro("Requisicao invalida", HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> tratarFalhaDeAutenticacao(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(erro("Credenciais invalidas.", HttpStatus.UNAUTHORIZED));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException exception) {
        Map<String, Object> body = erro("Erro de validacao", HttpStatus.BAD_REQUEST);
        Map<String, String> campos = new HashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            campos.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        body.put("campos", campos);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarErroGeral(Exception exception) {
        LOGGER.error("Erro interno nao tratado", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(erro("Ocorreu um erro interno inesperado", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private Map<String, Object> erro(String mensagem, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", mensagem);
        return body;
    }
}
