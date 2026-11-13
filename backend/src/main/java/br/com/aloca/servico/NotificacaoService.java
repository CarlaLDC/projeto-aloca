package br.com.aloca.servico;

import br.com.aloca.api.dto.NotificacaoDtos.*;
import br.com.aloca.dominio.*;
import br.com.aloca.integracao.NotificacaoCriada;
import br.com.aloca.repositorio.NotificacaoRepositorio;
import br.com.aloca.repositorio.UsuarioRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notificações internas (RF-11). Cada notificação também é repassada ao serviço de e-mail externo,
 * quando ele está configurado (veja {@link br.com.aloca.integracao.NotificacaoExternaClient}).
 */
@Service
public class NotificacaoService {

    private final NotificacaoRepositorio notificacoes;
    private final UsuarioRepositorio usuarios;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    public NotificacaoService(NotificacaoRepositorio notificacoes, UsuarioRepositorio usuarios,
                              ApplicationEventPublisher eventos, Clock relogio) {
        this.notificacoes = notificacoes;
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    @Transactional
    public void notificar(Usuario destinatario, String mensagem, Reserva reserva) {
        salvar(destinatario, mensagem, reserva);
    }

    @Transactional
    public void notificarPerfil(Perfil perfil, String mensagem, Reserva reserva) {
        usuarios.comPerfil(perfil).forEach(u -> salvar(u, mensagem, reserva));
    }

    private void salvar(Usuario destinatario, String mensagem, Reserva reserva) {
        String codigo = reserva == null ? null : reserva.getCodigo();
        notificacoes.save(new Notificacao(destinatario, mensagem, codigo, LocalDateTime.now(relogio)));
        eventos.publishEvent(new NotificacaoCriada(destinatario.getEmail(), destinatario.getNome(), mensagem, codigo));
    }


    @Transactional(readOnly = true)
    public Notificacoes listar(Usuario usuario) {
        return new Notificacoes(
            notificacoes.countByDestinatarioIdAndLidaFalse(usuario.getId()),
            notificacoes.findTop20ByDestinatarioIdOrderByCriadaEmDesc(usuario.getId()).stream()
                .map(n -> new NotificacaoDto(n.getId(), n.getMensagem(), n.getCodigoReserva(), n.getCriadaEm(), n.isLida()))
                .toList());
    }

    @Transactional
    public void marcarLidas(Usuario usuario) {
        notificacoes.marcarTodasLidas(usuario.getId());
    }
}
