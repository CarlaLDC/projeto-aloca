package br.com.aloca.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** A cada minuto, move reservas aprovadas para "Em uso" e "Concluída" conforme o horário. */
@Component
public class CicloReservaAgendador {

    private static final Logger log = LoggerFactory.getLogger(CicloReservaAgendador.class);

    private final ReservaService reservas;

    public CicloReservaAgendador(ReservaService reservas) {
        this.reservas = reservas;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() {
        executar();
    }

    @Scheduled(fixedDelayString = "${aloca.ciclo.intervalo:PT1M}", initialDelayString = "${aloca.ciclo.intervalo:PT1M}")
    public void executar() {
        try {
            int alteradas = reservas.atualizarCiclo();
            if (alteradas > 0) log.info("Ciclo de reservas: {} reserva(s) atualizada(s)", alteradas);
        } catch (RuntimeException e) {
            log.warn("Falha ao atualizar o ciclo das reservas", e);
        }
    }
}
