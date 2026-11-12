package br.com.aloca.servico;

import br.com.aloca.dominio.EventoAuditoria;
import br.com.aloca.dominio.Usuario;
import br.com.aloca.repositorio.EventoAuditoriaRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Escrita e consulta centralizadas da trilha de auditoria persistente. */
@Service
public class AuditoriaService {
    private final EventoAuditoriaRepositorio eventos;
    private final Clock relogio;

    public AuditoriaService(EventoAuditoriaRepositorio eventos, Clock relogio) {
        this.eventos = eventos;
        this.relogio = relogio;
    }

    @Transactional
    public EventoAuditoria registrar(Usuario ator, String acao, String tipoEntidade, Object id, String descricao) {
        return eventos.save(new EventoAuditoria(LocalDateTime.now(relogio), ator, acao, tipoEntidade, id, descricao));
    }

    @Transactional(readOnly = true)
    public List<EventoAuditoria> listarRecentes() {
        return eventos.findAll(org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Direction.DESC, "dataHora", "id"));
    }
}
