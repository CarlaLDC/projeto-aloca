package br.com.aloca.repositorio;

import br.com.aloca.dominio.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoAuditoriaRepositorio extends JpaRepository<EventoAuditoria, Long> {}
