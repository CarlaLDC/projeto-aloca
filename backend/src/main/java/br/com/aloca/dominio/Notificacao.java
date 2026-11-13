package br.com.aloca.dominio;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Notificação simulada dentro do sistema (RF-11 permite simulação). */
@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario destinatario;

    @Column(nullable = false, length = 300)
    private String mensagem;

    @Column(length = 20)
    private String codigoReserva;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    private boolean lida;

    protected Notificacao() {}

    public Notificacao(Usuario destinatario, String mensagem, String codigoReserva, LocalDateTime criadaEm) {
        this.destinatario = destinatario;
        this.mensagem = mensagem;
        this.codigoReserva = codigoReserva;
        this.criadaEm = criadaEm;
    }

    public Long getId() { return id; }
    public Usuario getDestinatario() { return destinatario; }
    public String getMensagem() { return mensagem; }
    public String getCodigoReserva() { return codigoReserva; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public boolean isLida() { return lida; }
    public void marcarLida() { this.lida = true; }
}
