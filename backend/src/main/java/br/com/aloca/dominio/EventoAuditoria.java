package br.com.aloca.dominio;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Registro persistente e transversal de ações administrativas. */
@Entity
@Table(name = "evento_auditoria", indexes = {
    @Index(name = "idx_auditoria_data", columnList = "dataHora"),
    @Index(name = "idx_auditoria_entidade", columnList = "tipoEntidade, entidadeId")
})
public class EventoAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private LocalDateTime dataHora;
    @Column(nullable = false, length = 100) private String ator;
    @ManyToOne private Usuario usuario;
    @Column(nullable = false, length = 60) private String acao;
    @Column(nullable = false, length = 60) private String tipoEntidade;
    @Column(length = 80) private String entidadeId;
    @Column(length = 1000) private String descricao;

    protected EventoAuditoria() {}

    public EventoAuditoria(LocalDateTime dataHora, Usuario usuario, String acao,
                           String tipoEntidade, Object entidadeId, String descricao) {
        this.dataHora = dataHora;
        this.usuario = usuario;
        this.ator = usuario == null ? "Sistema" : usuario.getNome();
        this.acao = acao;
        this.tipoEntidade = tipoEntidade;
        this.entidadeId = entidadeId == null ? null : entidadeId.toString();
        this.descricao = descricao;
    }

    public Long getId() { return id; }
    public LocalDateTime getDataHora() { return dataHora; }
    public String getAtor() { return ator; }
    public Usuario getUsuario() { return usuario; }
    public String getAcao() { return acao; }
    public String getTipoEntidade() { return tipoEntidade; }
    public String getEntidadeId() { return entidadeId; }
    public String getDescricao() { return descricao; }
}
