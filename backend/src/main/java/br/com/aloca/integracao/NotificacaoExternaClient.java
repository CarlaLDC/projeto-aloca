package br.com.aloca.integracao;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Integração externa: envia as notificações do sistema para o serviço de e-mail da instituição (HTTP).
 *
 * <ul>
 *   <li>Desligada por padrão: sem {@code aloca.notificacao.url}, o envio é só simulado (RF-11 permite).</li>
 *   <li>O envio acontece depois que a transação é gravada, para a chamada HTTP não segurar as travas do banco.</li>
 *   <li>Falha ou demora do serviço externo nunca desfaz a reserva: o erro é registrado no log.</li>
 * </ul>
 * Nos testes, o serviço externo é substituído pelo WireMock.
 */
@Component
public class NotificacaoExternaClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoExternaClient.class);

    /** Corpo enviado ao serviço externo. */
    public record Envio(String para, String nome, String assunto, String mensagem, String codigoReserva) {}

    private final String url;
    private final RestClient http;

    public NotificacaoExternaClient(@Value("${aloca.notificacao.url:}") String url,
                                    @Value("${aloca.notificacao.token:}") String token,
                                    @Value("${aloca.notificacao.timeout:PT3S}") Duration timeout) {
        this.url = url == null ? "" : url.trim();
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(timeout);
        fabrica.setReadTimeout(timeout);
        RestClient.Builder builder = RestClient.builder().requestFactory(fabrica);
        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token.trim());
        }
        this.http = builder.build();
    }

    public boolean habilitado() {
        return !url.isEmpty();
    }

    /** Recebe as notificações criadas pelo sistema, já gravadas no banco. */
    @TransactionalEventListener(fallbackExecution = true)
    public void aoCriarNotificacao(NotificacaoCriada evento) {
        if (!habilitado()) {
            log.debug("Notificação externa desligada (simulação): {}", evento.mensagem());
            return;
        }
        enviar(new Envio(evento.email(), evento.nome(), assunto(evento), evento.mensagem(), evento.codigoReserva()));
    }

    /** @return true se o serviço externo aceitou a notificação (resposta 2xx). */
    public boolean enviar(Envio envio) {
        if (!habilitado()) return false;
        try {
            http.post().uri(url).contentType(MediaType.APPLICATION_JSON).body(envio).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.warn("Não foi possível enviar a notificação para {}: {}", envio.para(), e.getMessage());
            return false;
        }
    }

    private static String assunto(NotificacaoCriada e) {
        return e.codigoReserva() == null ? "Aloca" : "Aloca · reserva " + e.codigoReserva();
    }
}
