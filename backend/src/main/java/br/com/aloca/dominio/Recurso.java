package br.com.aloca.dominio;

import jakarta.persistence.*;
import java.time.LocalTime;

/** Sala, laboratório ou equipamento que pode ser reservado. */
@Entity
@Table(name = "recurso")
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRecurso tipo;

    @Column(length = 60)
    private String categoria;

    @Column(nullable = false, length = 150)
    private String localizacao;

    private int capacidade;

    /** Recurso restrito exige aprovação do Responsável (RF-07). */
    private boolean restrito;

    private boolean ativo = true;

    @Column(nullable = false)
    private LocalTime abertura = LocalTime.of(7, 0);

    @Column(nullable = false)
    private LocalTime fechamento = LocalTime.of(22, 0);

    protected Recurso() {}

    public Recurso(String nome, TipoRecurso tipo, String categoria, String localizacao, int capacidade,
                   boolean restrito, LocalTime abertura, LocalTime fechamento) {
        atualizar(nome, tipo, categoria, localizacao, capacidade, restrito, abertura, fechamento);
    }

    public void atualizar(String nome, TipoRecurso tipo, String categoria, String localizacao, int capacidade,
                          boolean restrito, LocalTime abertura, LocalTime fechamento) {
        this.nome = nome;
        this.tipo = tipo;
        this.categoria = categoria;
        this.localizacao = localizacao;
        this.capacidade = capacidade;
        this.restrito = restrito;
        this.abertura = abertura;
        this.fechamento = fechamento;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public TipoRecurso getTipo() { return tipo; }
    public String getCategoria() { return categoria; }
    public String getLocalizacao() { return localizacao; }
    public int getCapacidade() { return capacidade; }
    public boolean isRestrito() { return restrito; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalTime getAbertura() { return abertura; }
    public LocalTime getFechamento() { return fechamento; }
}
