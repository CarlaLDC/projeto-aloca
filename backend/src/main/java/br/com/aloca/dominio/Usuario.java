package br.com.aloca.dominio;

import jakarta.persistence.*;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String senhaHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "usuario_perfil", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "perfil", nullable = false)
    private Set<Perfil> perfis = EnumSet.noneOf(Perfil.class);

    @Column(nullable = false, length = 100)
    private String unidade;

    private boolean ativo = true;

    protected Usuario() {}

    public Usuario(String nome, String email, String senhaHash, String unidade, Set<Perfil> perfis) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.unidade = unidade;
        this.perfis = EnumSet.copyOf(perfis);
    }

    public boolean possui(Perfil perfil) { return perfis.contains(perfil); }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public Set<Perfil> getPerfis() { return perfis; }
    public String getUnidade() { return unidade; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
