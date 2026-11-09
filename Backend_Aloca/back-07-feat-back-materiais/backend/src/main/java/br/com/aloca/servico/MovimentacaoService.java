package br.com.aloca.servico;

import br.com.aloca.api.dto.MovimentacaoDtos.*;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.MovimentacaoRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Retirada e devolução de materiais (RF-09). Tela 09. */
@Service
public class MovimentacaoService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final MovimentacaoRepositorio movimentacoes;
    private final Clock relogio;

    public MovimentacaoService(MovimentacaoRepositorio movimentacoes, Clock relogio) {
        this.movimentacoes = movimentacoes;
        this.relogio = relogio;
    }

    // ---------------------------------------------------------- reações às reservas

    @EventListener
    public void aoAprovar(ReservaEventos.Aprovada e) { gerar(e.reserva()); }

    @EventListener
    public void aoAlterar(ReservaEventos.Alterada e) { regenerar(e.reserva()); }

    @EventListener
    public void aoCancelar(ReservaEventos.Cancelada e) { cancelarPendentes(e.reserva()); }

    /** Reserva com material já retirado está em uso e não pode ser alterada nem cancelada (RF-10). */
    @EventListener
    public void antesDeModificar(ReservaEventos.AntesDeModificar e) {
        if (possuiMaterialEmUso(e.reserva())) {
            throw RegraNegocioException.conflito("Esta reserva já está em uso e permanece no histórico.");
        }
    }

    // ---------------------------------------------------------- regras

    /** Cria uma movimentação "Aguardando retirada" para cada material de uma reserva aprovada. */
    @Transactional
    public void gerar(Reserva reserva) {
        for (Recurso material : reserva.getMateriais()) {
            Movimentacao m = movimentacoes.save(new Movimentacao(reserva, material));
            m.setCodigo("MOV-%04d".formatted(m.getId()));
        }
    }

    /** Após alteração da reserva: descarta o que ainda não foi retirado e gera de novo se estiver aprovada. */
    @Transactional
    public void regenerar(Reserva reserva) {
        cancelarPendentes(reserva);
        if (reserva.getStatus() == StatusReserva.APROVADA) {
            gerar(reserva);
        }
    }

    @Transactional
    public void cancelarPendentes(Reserva reserva) {
        movimentacoes.findByReservaId(reserva.getId()).stream()
            .filter(m -> m.getStatus() == StatusMovimentacao.AGUARDANDO_RETIRADA)
            .forEach(Movimentacao::cancelar);
    }

    @Transactional(readOnly = true)
    public boolean possuiMaterialEmUso(Reserva reserva) {
        return movimentacoes.findByReservaId(reserva.getId()).stream()
            .anyMatch(m -> m.getStatus() == StatusMovimentacao.EM_USO);
    }

    @Transactional(readOnly = true)
    public Movimentacoes listar(String status, String busca) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        List<Movimentacao> todas = movimentacoes.findByStatusNotOrderByRetiradaPrevistaDesc(StatusMovimentacao.CANCELADA);

        long aguardando = todas.stream().filter(m -> m.statusEm(agora) == StatusMovimentacao.AGUARDANDO_RETIRADA).count();
        long emUso = todas.stream().filter(m -> m.statusEm(agora) == StatusMovimentacao.EM_USO).count();
        long pendentes = todas.stream().filter(m -> m.statusEm(agora) == StatusMovimentacao.DEVOLUCAO_PENDENTE).count();

        String termo = busca == null ? "" : busca.trim().toLowerCase(PT_BR);
        List<MovimentacaoDto> itens = todas.stream()
            .filter(m -> status == null || status.isBlank() || m.statusEm(agora).name().equalsIgnoreCase(status))
            .map(m -> dto(m, agora))
            .filter(d -> termo.isEmpty() || (d.codigo() + " " + d.reserva() + " " + d.item() + " " + d.responsavel()
                + " " + d.destino()).toLowerCase(PT_BR).contains(termo))
            .toList();
        return new Movimentacoes(aguardando, emUso, pendentes, itens);
    }

    @Transactional
    public MovimentacaoDto registrarRetirada(String codigo, Usuario usuario) {
        Movimentacao m = buscar(codigo);
        if (m.getStatus() != StatusMovimentacao.AGUARDANDO_RETIRADA) {
            throw RegraNegocioException.conflito("Este material não está aguardando retirada.");
        }
        Reserva r = m.getReserva();
        if (r.getStatus() != StatusReserva.APROVADA && r.getStatus() != StatusReserva.EM_USO) {
            throw RegraNegocioException.conflito("A reserva " + r.getCodigo() + " não está aprovada para uso.");
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        m.registrarRetirada(usuario, agora);
        r.registrar(new HistoricoReserva(r, agora, usuario.getNome(), usuario, "Retirada",
            m.getMaterial().getNome() + " retirado · " + m.getCodigo(), r.getStatus(), r.getStatus()));
        return dto(m, agora);
    }

    @Transactional
    public MovimentacaoDto registrarDevolucao(String codigo, Usuario usuario) {
        Movimentacao m = buscar(codigo);
        if (m.getStatus() != StatusMovimentacao.EM_USO) {
            throw RegraNegocioException.conflito("Só é possível registrar a devolução de um material retirado.");
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        m.registrarDevolucao(usuario, agora, "DEV-%04d".formatted(m.getId()));
        Reserva r = m.getReserva();
        r.registrar(new HistoricoReserva(r, agora, usuario.getNome(), usuario, "Devolução",
            m.getMaterial().getNome() + " devolvido · protocolo " + m.getProtocolo(), r.getStatus(), r.getStatus()));
        return dto(m, agora);
    }

    private Movimentacao buscar(String codigo) {
        return movimentacoes.findByCodigo(codigo)
            .orElseThrow(() -> RegraNegocioException.naoEncontrada("Movimentação " + codigo + " não encontrada."));
    }

    private MovimentacaoDto dto(Movimentacao m, LocalDateTime agora) {
        Reserva r = m.getReserva();
        Recurso espaco = r.getEspaco();
        StatusMovimentacao status = m.statusEm(agora);
        String categoria = m.getMaterial().getCategoria() != null ? m.getMaterial().getCategoria()
            : m.getMaterial().getTipo().rotulo();
        return new MovimentacaoDto(m.getCodigo(), r.getCodigo(), m.getMaterial().getNome(), categoria,
            r.getSolicitante().getNome(), espaco.getNome() + " · " + espaco.getLocalizacao(),
            m.getRetiradaPrevista(), m.getDevolucaoPrevista(), m.getRetiradaEm(), m.getDevolucaoEm(),
            status.name(), status.rotulo(), m.getProtocolo());
    }
}
