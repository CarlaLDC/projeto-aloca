package br.com.aloca.dominio;

public enum Perfil {
    SOLICITANTE("Solicitante"),
    RESPONSAVEL("Responsável"),
    ADMINISTRADOR("Administrador"),
    PROFESSOR("Professor");

    private final String rotulo;

    Perfil(String rotulo) { this.rotulo = rotulo; }

    public String rotulo() { return rotulo; }
}
