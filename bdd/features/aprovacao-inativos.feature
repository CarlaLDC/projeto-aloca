# language: pt
@aprovacao-inativos
Funcionalidade: Revalidar cadastros ativos antes de aprovar uma solicitação
  Como responsável
  Quero impedir a aprovação de uma alocação desativada após a solicitação
  Para confirmar somente recursos e professores disponíveis para uso

  Esquema do Cenário: Desativação posterior bloqueia aprovação até a reativação
    Dado uma solicitação restrita com sala, material e professor ativos
    Quando o administrador desativa o "<alvo>" da solicitação
    Então o painel de aprovações informa a desativação do "<alvo>"
    Quando o responsável tenta aprovar a solicitação com cadastro desativado
    Então a API recusa com 409 e preserva a solicitação e sua auditoria
    Quando o administrador reativa o "<alvo>" da solicitação
    Então o painel informa que a solicitação pode ser aprovada
    Quando o responsável aprova a solicitação revalidada
    Então a reserva fica aprovada com auditoria da decisão
    Exemplos:
      | alvo      |
      | sala      |
      | material  |
      | professor |
