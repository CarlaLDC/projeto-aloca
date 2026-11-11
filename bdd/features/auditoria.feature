# language: pt
Funcionalidade: Consulta e manutenção demonstrativa da reserva
  Cenário: Consultar reserva inexistente
    Dado que acesso "/detalhe-auditoria/INEXISTENTE"
    Então vejo o título "Reserva não encontrada"

  Esquema do Cenário: Impedir manutenção de reserva encerrada
    Dado que acesso "/detalhe-auditoria/<codigo>"
    Então o botão "Alterar reserva" está desabilitado
    E o botão "Cancelar reserva" está desabilitado
    Exemplos:
      | codigo   |
      | RES-0241 |
      | RES-0238 |

  Cenário: Registrar alteração no histórico local
    Dado que acesso "/detalhe-auditoria/RES-0248"
    Quando clico em "Alterar reserva"
    E preencho "Atividade" com "Aula alterada pelo BDD"
    E clico em "Salvar simulação"
    Então vejo o texto "Alteração simulada. Nenhum dado foi enviado ao servidor."
    E o histórico contém "Alterada · simulação"

  Cenário: Exigir motivo do cancelamento
    Dado que acesso "/detalhe-auditoria/RES-0248"
    Quando clico em "Cancelar reserva"
    E clico em "Confirmar simulação"
    Então vejo o texto "Preencha os campos obrigatórios."
    E o diálogo permanece aberto

  Cenário: Cancelar e registrar o motivo no histórico local
    Dado que acesso "/detalhe-auditoria/RES-0248"
    Quando clico em "Cancelar reserva"
    E preencho "Motivo do cancelamento" com "Atividade reagendada"
    E clico em "Confirmar simulação"
    Então vejo o texto "Cancelamento simulado. Nenhum dado foi enviado ao servidor."
    E o histórico contém "Cancelada · simulação"
    E o histórico contém "Atividade reagendada"
    E o botão "Alterar reserva" está desabilitado
    E o botão "Cancelar reserva" está desabilitado
