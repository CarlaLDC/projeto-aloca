// Tipos das respostas da API do back-end (backend/src/main/java/br/com/aloca/api/dto).

export type PerfilCodigo = 'SOLICITANTE' | 'RESPONSAVEL' | 'ADMINISTRADOR' | 'PROFESSOR';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  unidade: string;
  perfis: PerfilCodigo[];
  perfilRotulo: string;
  iniciais: string;
}

export interface LoginResposta {
  token: string;
  expiraEm: string;
  usuario: Usuario;
}

export interface ErroApi {
  mensagem: string;
  campos?: Record<string, string> | null;
}

export type StatusReserva =
  'SOLICITADA' | 'APROVADA' | 'REJEITADA' | 'EM_USO' | 'CONCLUIDA' | 'CANCELADA' | 'NAO_COMPARECEU';

export interface OpcaoRecurso {
  id: number;
  nome: string;
  tipo: 'SALA' | 'LABORATORIO' | 'EQUIPAMENTO';
  tipoRotulo: string;
  localizacao: string;
  capacidade: number;
  restrito: boolean;
  rotulo: string;
}

export interface OpcaoProfessor {
  id: number;
  nome: string;
  competencia: string;
  rotulo: string;
}

export interface OpcoesReserva {
  espacos: OpcaoRecurso[];
  professores: OpcaoProfessor[];
  materiais: OpcaoRecurso[];
}

export interface ReservaPedido {
  atividade: string;
  data: string;
  inicio: string;
  fim: string;
  espacoId: number;
  professorId: number | null;
  materialIds: number[];
}

export interface ReservaResumo {
  codigo: string;
  atividade: string;
  recurso: string;
  data: string;
  inicio: string;
  fim: string;
  status: StatusReserva;
  statusRotulo: string;
}

export interface EventoHistorico {
  dataHora: string;
  ator: string;
  acao: string;
  descricao: string | null;
  estadoAnterior: string | null;
  estadoNovo: string | null;
}

export interface ReservaDetalhe {
  codigo: string;
  atividade: string;
  data: string;
  inicio: string;
  fim: string;
  espaco: OpcaoRecurso;
  professor: OpcaoProfessor | null;
  materiais: OpcaoRecurso[];
  solicitante: string;
  status: StatusReserva;
  statusRotulo: string;
  motivoRejeicao: string | null;
  exigeAprovacao: boolean;
  podeGerenciar: boolean;
  criadaEm: string;
  historico: EventoHistorico[];
}

export interface ItemConflito {
  tipo: 'Sala' | 'Material' | 'Professor' | 'Manutenção';
  recurso: string;
  localizacao: string;
  inicio: string;
  fim: string;
  atividade: string | null;
  responsavel: string | null;
  sobreposicaoMinutos: number;
}

export interface Alternativa {
  data: string;
  inicio: string;
  fim: string;
  espacoId: number;
  espaco: string;
  localizacao: string;
  motivo: string;
}

/** Corpo do 409 devolvido ao criar ou alterar uma reserva com conflito. */
export interface ConflitoResposta {
  mensagem: string;
  atividade: string;
  data: string;
  inicio: string;
  fim: string;
  espacoId: number;
  professorId: number | null;
  materialIds: number[];
  conflitos: ItemConflito[];
  alternativas: Alternativa[];
}

export interface ItemAgenda {
  codigo: string;
  inicio: string;
  fim: string;
  local: string;
  atividade: string;
  status: StatusReserva;
  statusRotulo: string;
}

export interface Painel {
  perfil: string;
  data: string;
  reservasHoje: number;
  recursosDisponiveis: number;
  aguardandoAprovacao: number;
  agenda: ItemAgenda[];
}

export interface Intervalo {
  inicio: string;
  fim: string;
  motivo: string;
}

export interface RecursoDisponibilidade {
  id: number;
  nome: string;
  tipo: 'SALA' | 'LABORATORIO' | 'EQUIPAMENTO';
  tipoRotulo: string;
  localizacao: string;
  capacidade: number;
  restrito: boolean;
  status: 'Disponível' | 'Ocupado' | 'Em manutenção';
  livreDe: string | null;
  livreAte: string | null;
  ocupacoes: Intervalo[];
  janelasLivres: Intervalo[];
}

export interface Disponibilidade {
  data: string;
  resumo: { disponiveis: number; ocupados: number; total: number };
  recursos: RecursoDisponibilidade[];
}

export interface Solicitacao {
  codigo: string;
  atividade: string;
  solicitante: string;
  criadaEm: string;
  recursos: string;
  data: string;
  inicio: string;
  fim: string;
  professor: string | null;
  disponibilidadeValidada: boolean;
  problemas: string[];
}

export interface Aprovacoes {
  pendentes: number;
  aprovadasHoje: number;
  rejeitadasHoje: number;
  solicitacoes: Solicitacao[];
}

export type StatusMovimentacao = 'AGUARDANDO_RETIRADA' | 'EM_USO' | 'DEVOLUCAO_PENDENTE' | 'DEVOLVIDO';

export interface Movimentacao {
  codigo: string;
  reserva: string;
  item: string;
  categoria: string;
  responsavel: string;
  destino: string;
  retiradaPrevista: string;
  devolucaoPrevista: string;
  retiradaEm: string | null;
  devolucaoEm: string | null;
  status: StatusMovimentacao;
  statusRotulo: string;
  protocolo: string | null;
}

export interface Movimentacoes {
  aguardandoRetirada: number;
  emUso: number;
  devolucoesPendentes: number;
  movimentacoes: Movimentacao[];
}

export interface LinhaRecurso {
  id: number;
  nome: string;
  tipoRotulo: string;
  utilizacao: number;
  horas: number;
  reservas: number;
  conflitos: number;
}

export interface Relatorio {
  inicio: string;
  fim: string;
  utilizacaoEspacos: number;
  horasAlocadas: number;
  conflitosEvitados: number;
  totalReservas: number;
  recursos: LinhaRecurso[];
  professores: { id: number; nome: string; horas: number; reservas: number }[];
}

export interface Notificacoes {
  naoLidas: number;
  itens: { id: number; mensagem: string; codigoReserva: string | null; criadaEm: string; lida: boolean }[];
}

/** Administração: entidades persistidas pelo backend. */
export interface AdminRecurso {
  id: number; nome: string; tipo: 'SALA' | 'LABORATORIO' | 'EQUIPAMENTO';
  categoria: string | null; localizacao: string; capacidade: number;
  restrito: boolean; ativo: boolean; abertura: string; fechamento: string;
}
export interface AdminProfessor { id: number; nome: string; competencia: string; ativo: boolean; }
export interface AdminBloqueio {
  id: number; recursoId: number; recurso: string; inicio: string; fim: string; motivo: string; criadoPor: string | null;
}
