package br.com.aloca.servico;

import org.springframework.http.HttpStatus;

/** Erro de regra com mensagem segura para mostrar ao usuário (RF-11, RNF-03). */
public class RegraNegocioException extends RuntimeException {

    private final HttpStatus status;

    public RegraNegocioException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

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
}
