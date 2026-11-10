package br.com.aloca.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "professor")
public class Professor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 100)
    private String competencia;

    private boolean ativo = true;

    /** Conta de acesso do professor, usada para consultar a própria agenda (RF-14). */
    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    protected Professor() {}

    public Professor(String nome, String competencia, Usuario usuario) {
        this.nome = nome;
        this.competencia = competencia;
        this.usuario = usuario;
    }

    public void atualizar(String nome, String competencia) {
        this.nome = nome;
        this.competencia = competencia;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCompetencia() { return competencia; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Usuario getUsuario() { return usuario; }
}
