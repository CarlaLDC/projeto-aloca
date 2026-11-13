package br.com.aloca.dominio;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Evento auditável de uma reserva: quem, quando, estado anterior e novo (RF-10). */
@Entity
@Table(name = "historico_reserva")
public class HistoricoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Reserva reserva;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    /** Nome exibido de quem fez a ação ("Sistema" para ações automáticas). */
    @Column(nullable = false, length = 100)
    private String ator;

    @ManyToOne
    private Usuario usuario;

    @Column(nullable = false, length = 60)
    private String acao;

    @Column(length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusReserva estadoNovo;

    protected HistoricoReserva() {}

    public HistoricoReserva(Reserva reserva, LocalDateTime dataHora, String ator, Usuario usuario, String acao,
                            String descricao, StatusReserva estadoAnterior, StatusReserva estadoNovo) {
        this.reserva = reserva;
        this.dataHora = dataHora;
        this.ator = ator;
        this.usuario = usuario;
        this.acao = acao;
        this.descricao = descricao;
        this.estadoAnterior = estadoAnterior;
        this.estadoNovo = estadoNovo;
    }

    public Long getId() { return id; }
    public Reserva getReserva() { return reserva; }
    public LocalDateTime getDataHora() { return dataHora; }
    public String getAtor() { return ator; }
    public Usuario getUsuario() { return usuario; }
    public String getAcao() { return acao; }
    public String getDescricao() { return descricao; }
    public StatusReserva getEstadoAnterior() { return estadoAnterior; }
    public StatusReserva getEstadoNovo() { return estadoNovo; }
}
