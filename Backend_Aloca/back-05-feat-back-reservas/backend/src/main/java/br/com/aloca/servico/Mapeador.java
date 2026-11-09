package br.com.aloca.servico;

import br.com.aloca.api.dto.ReservaDtos.*;
import br.com.aloca.dominio.*;
import org.springframework.stereotype.Component;

/** Converte entidades em DTOs da API. */
@Component
public class Mapeador {

    public OpcaoRecurso recurso(Recurso r) {
        String rotulo = r.getTipo().espaco()
            ? r.getNome() + " · " + r.getCapacidade() + " lugares"
            : r.getNome() + " · " + r.getCapacidade() + (r.getCapacidade() == 1 ? " unidade" : " unidades");
        return new OpcaoRecurso(r.getId(), r.getNome(), r.getTipo().name(), r.getTipo().rotulo(),
            r.getLocalizacao(), r.getCapacidade(), r.isRestrito(), rotulo);
    }

    public OpcaoProfessor professor(Professor p) {
        if (p == null) return null;
        return new OpcaoProfessor(p.getId(), p.getNome(), p.getCompetencia(), p.getNome() + " · " + p.getCompetencia());
    }

    public ReservaResumo resumo(Reserva r) {
        return new ReservaResumo(r.getCodigo(), r.getAtividade(), r.getEspaco().getNome(), r.getData(),
            r.getInicio(), r.getFim(), r.getStatus().name(), r.getStatus().rotulo());
    }

    public ReservaDetalhe detalhe(Reserva r, boolean podeGerenciar) {
        return new ReservaDetalhe(
            r.getCodigo(), r.getAtividade(), r.getData(), r.getInicio(), r.getFim(),
            recurso(r.getEspaco()), professor(r.getProfessor()),
            r.getMateriais().stream().map(this::recurso).toList(),
            r.getSolicitante().getNome(), r.getStatus().name(), r.getStatus().rotulo(), r.getMotivoRejeicao(),
            r.exigeAprovacao(), podeGerenciar, r.getCriadaEm(),
            r.getHistorico().stream().map(this::evento).toList());
    }

    public EventoHistorico evento(HistoricoReserva h) {
        return new EventoHistorico(h.getDataHora(), h.getAtor(), h.getAcao(), h.getDescricao(),
            h.getEstadoAnterior() == null ? null : h.getEstadoAnterior().rotulo(),
            h.getEstadoNovo() == null ? null : h.getEstadoNovo().rotulo());
    }

    /** "Laboratório 02 + Projetor 04 + Notebook 08" */
    public String recursosDaReserva(Reserva r) {
        StringBuilder sb = new StringBuilder(r.getEspaco().getNome());
        r.getMateriais().forEach(m -> sb.append(" + ").append(m.getNome()));
        return sb.toString();
    }
}
