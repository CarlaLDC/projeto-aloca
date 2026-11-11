package br.com.aloca.servico;

import br.com.aloca.api.dto.NotificacaoDtos.*;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.NotificacaoRepositorio;
import br.com.aloca.repositorio.UsuarioRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Notificações internas. O envio de e-mail é simulado, como permite o RF-11. */
@Service
public class NotificacaoService {

    private final NotificacaoRepositorio notificacoes;
    private final UsuarioRepositorio usuarios;
    private final Clock relogio;

    public NotificacaoService(NotificacaoRepositorio notificacoes, UsuarioRepositorio usuarios, Clock relogio) {
        this.notificacoes = notificacoes;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    @Transactional
    public void notificar(Usuario destinatario, String mensagem, Reserva reserva) {
        notificacoes.save(new Notificacao(destinatario, mensagem, reserva == null ? null : reserva.getCodigo(),
            LocalDateTime.now(relogio)));
    }

    @Transactional
    public void notificarPerfil(Perfil perfil, String mensagem, Reserva reserva) {
        usuarios.comPerfil(perfil).forEach(u -> notificar(u, mensagem, reserva));
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
