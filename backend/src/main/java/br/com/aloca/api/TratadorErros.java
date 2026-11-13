package br.com.aloca.api;

import br.com.aloca.servico.RegraNegocioException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converte erros em {"mensagem": "..."} com texto claro, sem expor detalhes internos (RF-11, RNF-03).
 */
@RestControllerAdvice
public class TratadorErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorErros.class);

    public record Erro(String mensagem, Map<String, String> campos) {
        Erro(String mensagem) { this(mensagem, null); }
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<Erro> regra(RegraNegocioException e) {
        ResponseEntity.BodyBuilder resposta = ResponseEntity.status(e.getStatus());
        if (e.getTentarApos() != null) {
            resposta.header(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, e.getTentarApos().toSeconds())));
        }
        return resposta.body(new Erro(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Erro> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        String mensagem = campos.isEmpty() ? "Dados inválidos." : campos.values().iterator().next();
        return ResponseEntity.badRequest().body(new Erro(mensagem, campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Erro> formato(Exception e) {
        return ResponseEntity.badRequest().body(new Erro("Algum campo está em formato inválido. Confira datas e horários."));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ResponseEntity<Erro> acesso(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Erro("Seu perfil não tem permissão para esta ação."));
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    ResponseEntity<Erro> concorrencia(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new Erro("Outra pessoa está alterando este recurso agora. Tente novamente em instantes."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Erro> naoEncontrado(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Erro("Endereço não encontrado."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Erro> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new Erro("Não foi possível concluir a operação. Tente novamente."));
    }
}
