package br.com.aloca.dominio;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Período em que um recurso fica indisponível por manutenção (RF-08). */
@Entity
@Table(name = "bloqueio")
public class Bloqueio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Recurso recurso;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Column(nullable = false, length = 300)
    private String motivo;

    @ManyToOne
    private Usuario criadoPor;

    protected Bloqueio() {}

    public Bloqueio(Recurso recurso, LocalDateTime inicio, LocalDateTime fim, String motivo, Usuario criadoPor) {
        this.recurso = recurso;
        this.inicio = inicio;
        this.fim = fim;
        this.motivo = motivo;
        this.criadoPor = criadoPor;
    }

    public Long getId() { return id; }
    public Recurso getRecurso() { return recurso; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFim() { return fim; }
    public String getMotivo() { return motivo; }
    public Usuario getCriadoPor() { return criadoPor; }
}
