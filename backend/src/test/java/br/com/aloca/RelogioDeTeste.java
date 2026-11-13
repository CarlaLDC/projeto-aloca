package br.com.aloca;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Relógio que o teste pode adiantar, para simular "a reserva já começou" sem esperar o horário real.
 * Começa no horário atual. Use com {@code @Import(RelogioDeTeste.Config.class)}.
 */
public class RelogioDeTeste extends Clock {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private volatile Instant agora = Instant.now();

    /** Faz a aplicação "estar" na data e hora informadas (horário de Brasília). */
    public void ajustar(LocalDateTime dataHora) {
        this.agora = dataHora.atZone(FUSO).toInstant();
    }

    /** Volta para o horário real. */
    public void reiniciar() {
        this.agora = Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return FUSO;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return Clock.fixed(agora, zona);
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste();
        }
    }
}
