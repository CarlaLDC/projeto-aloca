package br.com.aloca.repositorio;

import br.com.aloca.dominio.Notificacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificacaoRepositorio extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findTop20ByDestinatarioIdOrderByCriadaEmDesc(Long usuarioId);

    long countByDestinatarioIdAndLidaFalse(Long usuarioId);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.destinatario.id = :usuarioId and n.lida = false")
    int marcarTodasLidas(Long usuarioId);
}
