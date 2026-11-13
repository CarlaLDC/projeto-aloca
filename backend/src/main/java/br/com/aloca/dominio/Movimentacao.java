package br.com.aloca.dominio;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Retirada e devolução de um material ligado a uma reserva (RF-09). */
@Entity
@Table(name = "movimentacao")
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String codigo;

    @ManyToOne(optional = false)
    private Reserva reserva;

    @ManyToOne(optional = false)
    private Recurso material;

    @Column(nullable = false)
    private LocalDateTime retiradaPrevista;

    @Column(nullable = false)
    private LocalDateTime devolucaoPrevista;

    private LocalDateTime retiradaEm;

    @ManyToOne
    private Usuario retiradaRegistradaPor;

    private LocalDateTime devolucaoEm;

    @ManyToOne
    private Usuario devolucaoRegistradaPor;

    /** Guarda AGUARDANDO_RETIRADA, EM_USO, DEVOLVIDO ou CANCELADA; "pendente" é calculado. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusMovimentacao status = StatusMovimentacao.AGUARDANDO_RETIRADA;

    @Column(length = 20)
    private String protocolo;

    @Version
    private long versao;

    protected Movimentacao() {}

    public Movimentacao(Reserva reserva, Recurso material) {
        this.reserva = reserva;
        this.material = material;
        this.retiradaPrevista = reserva.inicioEm();
        this.devolucaoPrevista = reserva.fimEm();
    }

    public StatusMovimentacao statusEm(LocalDateTime agora) {
        if (status == StatusMovimentacao.EM_USO && agora.isAfter(devolucaoPrevista)) {
            return StatusMovimentacao.DEVOLUCAO_PENDENTE;
        }
        return status;
    }

    public void registrarRetirada(Usuario usuario, LocalDateTime quando) {
        this.status = StatusMovimentacao.EM_USO;
        this.retiradaEm = quando;
        this.retiradaRegistradaPor = usuario;
    }

    public void registrarDevolucao(Usuario usuario, LocalDateTime quando, String protocolo) {
        this.status = StatusMovimentacao.DEVOLVIDO;
        this.devolucaoEm = quando;
        this.devolucaoRegistradaPor = usuario;
        this.protocolo = protocolo;
    }

    public void cancelar() { this.status = StatusMovimentacao.CANCELADA; }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Reserva getReserva() { return reserva; }
    public Recurso getMaterial() { return material; }
    public LocalDateTime getRetiradaPrevista() { return retiradaPrevista; }
    public LocalDateTime getDevolucaoPrevista() { return devolucaoPrevista; }
    public LocalDateTime getRetiradaEm() { return retiradaEm; }
    public Usuario getRetiradaRegistradaPor() { return retiradaRegistradaPor; }
    public LocalDateTime getDevolucaoEm() { return devolucaoEm; }
    public Usuario getDevolucaoRegistradaPor() { return devolucaoRegistradaPor; }
    public StatusMovimentacao getStatus() { return status; }
    public String getProtocolo() { return protocolo; }
}
