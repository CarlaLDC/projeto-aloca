package br.com.aloca.dominio;

public enum StatusMovimentacao {
    AGUARDANDO_RETIRADA("Aguardando retirada"),
    EM_USO("Em uso"),
    DEVOLUCAO_PENDENTE("Devolução pendente"),
    DEVOLVIDO("Devolvido"),
    CANCELADA("Cancelada");

    private final String rotulo;

    StatusMovimentacao(String rotulo) { this.rotulo = rotulo; }

    public String rotulo() { return rotulo; }
}
