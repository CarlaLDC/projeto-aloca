package br.com.aloca.dominio;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tentativa de reserva recusada por conflito. Alimenta o indicador
 * "conflitos evitados" dos relatórios (RF-12). Uma tentativa pode gerar
 * uma linha por recurso envolvido; todas compartilham o mesmo {@code tentativa}.
 */
@Entity
@Table(name = "conflito_registrado")
public class ConflitoRegistrado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String tentativa;

    @Column(nullable = false)
    private LocalDateTime registradoEm;

    @Column(nullable = false)
    private LocalDate dataSolicitada;

    @ManyToOne
    private Recurso recurso;

    @ManyToOne
    private Professor professor;

    @ManyToOne
    private Usuario solicitante;

    protected ConflitoRegistrado() {}

    public ConflitoRegistrado(String tentativa, LocalDateTime registradoEm, LocalDate dataSolicitada,
                              Recurso recurso, Professor professor, Usuario solicitante) {
        this.tentativa = tentativa;
        this.registradoEm = registradoEm;
        this.dataSolicitada = dataSolicitada;
        this.recurso = recurso;
        this.professor = professor;
        this.solicitante = solicitante;
    }

    public Long getId() { return id; }
    public String getTentativa() { return tentativa; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public LocalDate getDataSolicitada() { return dataSolicitada; }
    public Recurso getRecurso() { return recurso; }
    public Professor getProfessor() { return professor; }
}
