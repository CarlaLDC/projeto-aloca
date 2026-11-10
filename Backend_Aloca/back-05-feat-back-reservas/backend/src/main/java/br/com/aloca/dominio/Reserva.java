package br.com.aloca.dominio;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Entity
@Table(name = "reserva", indexes = @Index(name = "idx_reserva_data", columnList = "data"))
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String atividade;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime inicio;

    @Column(nullable = false)
    private LocalTime fim;

    @ManyToOne(optional = false)
    private Recurso espaco;

    @ManyToOne
    private Professor professor;

    @ManyToMany
    @JoinTable(name = "reserva_material",
        joinColumns = @JoinColumn(name = "reserva_id"),
        inverseJoinColumns = @JoinColumn(name = "recurso_id"))
    @OrderBy("nome")
    private Set<Recurso> materiais = new LinkedHashSet<>();

    @ManyToOne(optional = false)
    private Usuario solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusReserva status;

    @Column(length = 500)
    private String motivoRejeicao;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataHora, id")
    private List<HistoricoReserva> historico = new ArrayList<>();

    @Version
    private long versao;

    protected Reserva() {}

    public Reserva(String atividade, LocalDate data, LocalTime inicio, LocalTime fim, Recurso espaco,
                   Professor professor, Collection<Recurso> materiais, Usuario solicitante, LocalDateTime criadaEm) {
        alterarDados(atividade, data, inicio, fim, espaco, professor, materiais);
        this.solicitante = solicitante;
        this.criadaEm = criadaEm;
        this.status = StatusReserva.SOLICITADA;
    }

    public void alterarDados(String atividade, LocalDate data, LocalTime inicio, LocalTime fim, Recurso espaco,
                             Professor professor, Collection<Recurso> materiais) {
        this.atividade = atividade;
        this.data = data;
        this.inicio = inicio;
        this.fim = fim;
        this.espaco = espaco;
        this.professor = professor;
        this.materiais.clear();
        this.materiais.addAll(materiais);
    }

    /** Muda o estado e registra o evento no histórico (RF-10). */
    public HistoricoReserva mudarStatus(StatusReserva novo, String ator, Usuario usuario, String acao,
                                        String descricao, LocalDateTime quando) {
        StatusReserva anterior = this.status;
        this.status = novo;
        return registrar(new HistoricoReserva(this, quando, ator, usuario, acao, descricao, anterior, novo));
    }

    public HistoricoReserva registrar(HistoricoReserva evento) {
        historico.add(evento);
        return evento;
    }

    public boolean exigeAprovacao() {
        return espaco.isRestrito() || materiais.stream().anyMatch(Recurso::isRestrito);
    }

    public LocalDateTime inicioEm() { return data.atTime(inicio); }
    public LocalDateTime fimEm() { return data.atTime(fim); }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getAtividade() { return atividade; }
    public LocalDate getData() { return data; }
    public LocalTime getInicio() { return inicio; }
    public LocalTime getFim() { return fim; }
    public Recurso getEspaco() { return espaco; }
    public Professor getProfessor() { return professor; }
    public Set<Recurso> getMateriais() { return materiais; }
    public Usuario getSolicitante() { return solicitante; }
    public StatusReserva getStatus() { return status; }
    public String getMotivoRejeicao() { return motivoRejeicao; }
    public void setMotivoRejeicao(String motivoRejeicao) { this.motivoRejeicao = motivoRejeicao; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public List<HistoricoReserva> getHistorico() { return historico; }
}
