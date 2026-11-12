# language: pt
@aprovacao-bloqueio
Funcionalidade: Aprovação de recurso restrito e bloqueio por manutenção
  Como equipe responsável pela alocação
  Quero verificar aprovação, autorização e manutenção na API real
  Para impedir confirmações indevidas e preservar a auditoria

  Contexto:
    Dado que preparo um recurso restrito exclusivo para o cenário

  @BDD-APR-01
  Cenário: Responsável aprova solicitação restrita com auditoria
    Dado que existe uma solicitação pendente para o recurso exclusivo
    Quando o responsável aprova a solicitação exclusiva
    Então a solicitação exclusiva fica aprovada com histórico da decisão

  @BDD-APR-02
  Esquema do Cenário: Perfil sem permissão não aprova recurso restrito
    Dado que existe uma solicitação pendente para o recurso exclusivo
    Quando o perfil "<perfil>" tenta aprovar a solicitação exclusiva
    Então a aprovação é negada e a solicitação permanece pendente
    Exemplos:
      | perfil        |
      | solicitante   |
      | administrador |

  @BDD-BLO-01
  Cenário: Manutenção impede criação de reserva no mesmo período
    Dado que o administrador bloqueia o recurso exclusivo no período solicitado
    Quando o solicitante tenta reservar o recurso exclusivo
    Então a criação é recusada por manutenção e não há reserva persistida

  @BDD-BLO-02
  Cenário: Manutenção posterior impede aprovação de solicitação pendente
    Dado que existe uma solicitação pendente para o recurso exclusivo
    E que o administrador bloqueia o recurso exclusivo no período solicitado
    Quando o responsável aprova a solicitação exclusiva
    Então a aprovação é recusada por manutenção e preserva o histórico anterior
