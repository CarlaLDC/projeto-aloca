package br.com.aloca.dominio;

public enum TipoRecurso {
    SALA("Salas"),
    LABORATORIO("Laboratórios"),
    EQUIPAMENTO("Equipamentos");

    private final String rotulo;

    TipoRecurso(String rotulo) { this.rotulo = rotulo; }

    public String rotulo() { return rotulo; }

    /** Salas e laboratórios são espaços; equipamentos são materiais. */
    public boolean espaco() { return this != EQUIPAMENTO; }
}
