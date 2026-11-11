package br.com.aloca.servico;

import br.com.aloca.api.dto.PainelDtos.*;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.ReservaRepositorio;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tela 02 · Visão geral: números do dia e agenda do usuário (inclui RF-14 para professores). */
@Service
public class PainelService {

    private final ReservaRepositorio reservas;
    private final DisponibilidadeService disponibilidade;
    private final Mapeador mapeador;
    private final Clock relogio;

    public PainelService(ReservaRepositorio reservas, DisponibilidadeService disponibilidade, Mapeador mapeador,
                         Clock relogio) {
        this.reservas = reservas;
        this.disponibilidade = disponibilidade;
        this.mapeador = mapeador;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Painel montar(Usuario usuario) {
        LocalDate hoje = LocalDate.now(relogio);
        EnumSet<StatusReserva> visiveis = EnumSet.complementOf(EnumSet.of(StatusReserva.CANCELADA, StatusReserva.REJEITADA));

        List<ItemAgenda> agenda = reservas.doDia(hoje, visiveis).stream()
            .filter(r -> r.getSolicitante().getId().equals(usuario.getId()) || doProfessor(r, usuario))
            .sorted(Comparator.comparing(Reserva::getInicio))
            .map(r -> new ItemAgenda(r.getCodigo(), r.getInicio(), r.getFim(), mapeador.recursosDaReserva(r),
                r.getAtividade(), r.getStatus().name(), r.getStatus().rotulo()))
            .toList();

        long aguardando = usuario.possui(Perfil.RESPONSAVEL)
            ? reservas.countByStatus(StatusReserva.SOLICITADA)
            : reservas.countBySolicitanteIdAndStatus(usuario.getId(), StatusReserva.SOLICITADA);

        String perfil = usuario.getPerfis().stream().min(Comparator.comparingInt(Perfil::ordinal))
            .map(Perfil::rotulo).orElse("");
        return new Painel(perfil, hoje, agenda.size(), disponibilidade.disponiveisHoje(), aguardando, agenda);
    }

    private static boolean doProfessor(Reserva r, Usuario u) {
        return r.getProfessor() != null && r.getProfessor().getUsuario() != null
            && r.getProfessor().getUsuario().getId().equals(u.getId());
    }
}
