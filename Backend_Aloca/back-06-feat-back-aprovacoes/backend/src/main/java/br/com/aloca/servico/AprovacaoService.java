package br.com.aloca.servico;

import br.com.aloca.api.dto.AprovacaoDtos.*;
import br.com.aloca.api.dto.ReservaDtos.ReservaDetalhe;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.HistoricoReservaRepositorio;
import br.com.aloca.repositorio.ReservaRepositorio;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tela 08 · Aprovações: decisão do Responsável sobre recursos restritos (RF-07). */
@Service
public class AprovacaoService {

    private final ReservaRepositorio reservas;
    private final HistoricoReservaRepositorio historicos;
    private final ReservaService regras;
    private final ApplicationEventPublisher eventos;
    private final Mapeador mapeador;
    private final Clock relogio;

    public AprovacaoService(ReservaRepositorio reservas, HistoricoReservaRepositorio historicos, ReservaService regras,
                            ApplicationEventPublisher eventos, Mapeador mapeador, Clock relogio) {
        this.reservas = reservas;
        this.historicos = historicos;
        this.regras = regras;
        this.eventos = eventos;
        this.mapeador = mapeador;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Aprovacoes listar() {
        LocalDateTime inicioDoDia = LocalDate.now(relogio).atStartOfDay();
        List<Solicitacao> pendentes = reservas.findByStatusOrderByCriadaEm(StatusReserva.SOLICITADA).stream()
            .map(r -> {
                List<String> problemas = regras.problemas(r);
                return new Solicitacao(r.getCodigo(), r.getAtividade(), r.getSolicitante().getNome(), r.getCriadaEm(),
                    mapeador.recursosDaReserva(r), r.getData(), r.getInicio(), r.getFim(),
                    r.getProfessor() == null ? null : mapeador.professor(r.getProfessor()).rotulo(),
                    problemas.isEmpty(), problemas);
            })
            .toList();
        return new Aprovacoes(pendentes.size(),
            historicos.decisoesDesde(StatusReserva.APROVADA, inicioDoDia),
            historicos.decisoesDesde(StatusReserva.REJEITADA, inicioDoDia),
            pendentes);
    }

    @Transactional
    public ReservaDetalhe aprovar(String codigo, Usuario responsavel) {
        Reserva r = exigirPendente(codigo);
        regras.travarRecursos(r);
        List<String> problemas = regras.problemas(r);
        if (!problemas.isEmpty()) {
            throw RegraNegocioException.conflito("Não é possível aprovar: " + problemas.getFirst() + ".");
        }
        LocalDateTime agora = regras.agora();
        if (!r.fimEm().isAfter(agora)) {
            throw RegraNegocioException.conflito("O horário desta solicitação já passou. Rejeite-a com uma justificativa.");
        }
        r.mudarStatus(StatusReserva.APROVADA, responsavel.getNome(), responsavel, "Aprovada", "Alocação validada", agora);
        eventos.publishEvent(new ReservaEventos.Aprovada(r));
        regras.notificarSolicitante(r, "Sua solicitação " + r.getCodigo() + " foi aprovada.", agora);
        return mapeador.detalhe(r, false);
    }

    @Transactional
    public ReservaDetalhe rejeitar(String codigo, String justificativa, Usuario responsavel) {
        Reserva r = exigirPendente(codigo);
        LocalDateTime agora = regras.agora();
        r.setMotivoRejeicao(justificativa.trim());
        r.mudarStatus(StatusReserva.REJEITADA, responsavel.getNome(), responsavel, "Rejeitada", justificativa.trim(), agora);
        regras.notificarSolicitante(r, "Sua solicitação " + r.getCodigo() + " foi rejeitada: " + justificativa.trim(), agora);
        return mapeador.detalhe(r, false);
    }

    private Reserva exigirPendente(String codigo) {
        Reserva r = regras.buscar(codigo);
        if (r.getStatus() != StatusReserva.SOLICITADA) {
            throw RegraNegocioException.conflito("A solicitação " + codigo + " já foi decidida ("
                + r.getStatus().rotulo().toLowerCase() + ").");
        }
        return r;
    }
}
