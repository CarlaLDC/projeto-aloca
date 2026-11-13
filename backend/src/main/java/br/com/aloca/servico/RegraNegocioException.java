package br.com.aloca.servico;

import java.time.Duration;
import org.springframework.http.HttpStatus;

/** Erro de regra com mensagem segura para mostrar ao usuário (RF-11, RNF-03). */
public class RegraNegocioException extends RuntimeException {

    private final HttpStatus status;
    /** Quando preenchido, a resposta leva o cabeçalho Retry-After (ex.: login bloqueado). */
    private final Duration tentarApos;

    public RegraNegocioException(HttpStatus status, String mensagem) {
        this(status, mensagem, null);
    }

    public RegraNegocioException(HttpStatus status, String mensagem, Duration tentarApos) {
        super(mensagem);
        this.status = status;
        this.tentarApos = tentarApos;
    }

    public HttpStatus getStatus() { return status; }

    public Duration getTentarApos() { return tentarApos; }

    public static RegraNegocioException invalida(String mensagem) {
        return new RegraNegocioException(HttpStatus.BAD_REQUEST, mensagem);
    }

    public static RegraNegocioException naoEncontrada(String mensagem) {
        return new RegraNegocioException(HttpStatus.NOT_FOUND, mensagem);
    }

    public static RegraNegocioException proibida(String mensagem) {
        return new RegraNegocioException(HttpStatus.FORBIDDEN, mensagem);
    }

    public static RegraNegocioException conflito(String mensagem) {
        return new RegraNegocioException(HttpStatus.CONFLICT, mensagem);
    }

    /** 429: muitas tentativas; o cliente deve esperar {@code espera} antes de tentar de novo. */
    public static RegraNegocioException muitasTentativas(String mensagem, Duration espera) {
        return new RegraNegocioException(HttpStatus.TOO_MANY_REQUESTS, mensagem, espera);
    }
}
