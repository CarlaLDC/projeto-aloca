package br.com.aloca.servico;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Regras de horário das reservas, sem banco nem Spring: recebem valores e devolvem resultados,
 * por isso são testadas isoladamente (RegrasDeHorarioTest).
 *
 * <p>Os períodos são semiabertos, [início, fim): uma reserva que termina às 11:00 não se
 * sobrepõe a outra que começa às 11:00. A consulta de conflitos no banco
 * ({@code ReservaRepositorio.conflitantes}) usa a mesma regra: {@code inicio < :fim and fim > :inicio}.
 */
public final class RegrasDeHorario {

    private static final int MINUTOS_NO_DIA = 24 * 60;

    private RegrasDeHorario() {}

    /** Um período dentro de um mesmo dia. */
    public record Periodo(LocalTime inicio, LocalTime fim) {}

    /**
     * Valida o horário pedido (RF-05): término depois do início, data de hoje ou futura e,
     * se for hoje, início que ainda não passou (comparado em minutos).
     *
     * @throws RegraNegocioException com a mensagem para o usuário
     */
    public static void validarPedido(LocalDate data, LocalTime inicio, LocalTime fim, LocalDateTime agora) {
        if (!fim.isAfter(inicio)) {
            throw RegraNegocioException.invalida("O término deve ser posterior ao início.");
        }
        LocalDate hoje = agora.toLocalDate();
        if (data.isBefore(hoje)) {
            throw RegraNegocioException.invalida("Escolha a data de hoje ou uma data futura.");
        }
        if (data.equals(hoje) && inicio.isBefore(agora.toLocalTime().withSecond(0).withNano(0))) {
            throw RegraNegocioException.invalida("O horário de início já passou. Escolha um horário futuro.");
        }
    }

    /** Dois períodos se sobrepõem quando um começa antes do outro terminar (consecutivos não se sobrepõem). */
    public static boolean sobrepoe(Periodo a, Periodo b) {
        return a.inicio().isBefore(b.fim()) && b.inicio().isBefore(a.fim());
    }

    /** Quantos minutos os dois períodos têm em comum (0 se não se sobrepõem). */
    public static long minutosSobrepostos(Periodo a, Periodo b) {
        if (!sobrepoe(a, b)) return 0;
        LocalTime inicio = a.inicio().isAfter(b.inicio()) ? a.inicio() : b.inicio();
        LocalTime fim = a.fim().isBefore(b.fim()) ? a.fim() : b.fim();
        return Duration.between(inicio, fim).toMinutes();
    }

    /** O período cabe inteiro no horário de funcionamento do recurso (as bordas contam). */
    public static boolean dentroDoFuncionamento(Periodo pedido, LocalTime abertura, LocalTime fechamento) {
        return !pedido.inicio().isBefore(abertura) && !pedido.fim().isAfter(fechamento);
    }

    /** O instante está dentro do período (o fim não conta: às 11:00 uma reserva de 09:00–11:00 já acabou). */
    public static boolean contem(Periodo periodo, LocalTime instante) {
        return !instante.isBefore(periodo.inicio()) && instante.isBefore(periodo.fim());
    }

    /** O período cobre todo o horário de funcionamento (usado para manutenção no dia inteiro). */
    public static boolean cobre(Periodo periodo, LocalTime abertura, LocalTime fechamento) {
        return !periodo.inicio().isAfter(abertura) && !periodo.fim().isBefore(fechamento);
    }

    /**
     * Intervalos livres entre {@code inicioBusca} e {@code fechamento}, descontando as ocupações
     * (em qualquer ordem, podendo se sobrepor). Janelas menores que {@code duracaoMinima} são descartadas.
     */
    public static List<Periodo> janelasLivres(LocalTime inicioBusca, LocalTime fechamento, List<Periodo> ocupacoes,
                                              Duration duracaoMinima) {
        List<Periodo> ordenadas = ocupacoes.stream().sorted(Comparator.comparing(Periodo::inicio)).toList();
        List<Periodo> livres = new ArrayList<>();
        LocalTime cursor = inicioBusca;
        for (Periodo o : ordenadas) {
            if (o.inicio().isAfter(cursor)) {
                adicionarSeCouber(livres, cursor, o.inicio().isBefore(fechamento) ? o.inicio() : fechamento, duracaoMinima);
            }
            if (o.fim().isAfter(cursor)) cursor = o.fim();
        }
        adicionarSeCouber(livres, cursor, fechamento, duracaoMinima);
        return livres;
    }

    /**
     * Horários de início possíveis para sugerir outra hora no mesmo dia: de {@code passo} em
     * {@code passo} a partir da abertura, terminando até o fechamento e sem passar da meia-noite.
     * Exclui o horário pedido e ordena do mais próximo para o mais distante dele.
     */
    public static List<LocalTime> horariosAlternativos(LocalTime abertura, LocalTime fechamento, Duration duracao,
                                                       Duration passo, LocalTime pedido) {
        int duracaoMin = (int) duracao.toMinutes();
        int passoMin = (int) passo.toMinutes();
        int limite = minutos(fechamento);
        int alvo = minutos(pedido);
        if (duracaoMin <= 0 || passoMin <= 0) return List.of();
        List<LocalTime> horarios = new ArrayList<>();
        for (int t = minutos(abertura); t + duracaoMin <= limite && t + duracaoMin < MINUTOS_NO_DIA; t += passoMin) {
            if (t != alvo) horarios.add(LocalTime.of(t / 60, t % 60));
        }
        horarios.sort(Comparator.comparingInt((LocalTime t) -> Math.abs(minutos(t) - alvo)).thenComparing(t -> t));
        return horarios;
    }

    /** Arredonda para cima no múltiplo de {@code passoMinutos}; perto da meia-noite, devolve 23:59:59. */
    public static LocalTime arredondarParaCima(LocalTime hora, int passoMinutos) {
        int total = hora.getHour() * 60 + hora.getMinute() + (hora.getSecond() > 0 || hora.getNano() > 0 ? 1 : 0);
        int arredondado = ((total + passoMinutos - 1) / passoMinutos) * passoMinutos;
        return arredondado >= MINUTOS_NO_DIA ? LocalTime.MAX.withNano(0) : LocalTime.of(arredondado / 60, arredondado % 60);
    }

    private static void adicionarSeCouber(List<Periodo> livres, LocalTime inicio, LocalTime fim, Duration minima) {
        if (Duration.between(inicio, fim).compareTo(minima) >= 0) {
            livres.add(new Periodo(inicio, fim));
        }
    }

    private static int minutos(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }
}
