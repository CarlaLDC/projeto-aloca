# language: pt
Funcionalidade: Revisão local de uma nova reserva
  Contexto:
    Dado que acesso "/nova-reserva"

  Cenário: Exigir atividade
    Quando preencho "Atividade" com "   "
    E clico em "Revisar e solicitar"
    Então vejo o texto "Preencha todos os campos obrigatórios."
    E não vejo o título "Revise sua reserva"

  Esquema do Cenário: Impedir intervalo de horário inválido
    Quando preencho "Início" com "<inicio>"
    E preencho "Término" com "<termino>"
    E clico em "Revisar e solicitar"
    Então vejo o texto "O término deve ser posterior ao início."
    E não vejo o título "Revise sua reserva"
    Exemplos:
      | inicio | termino |
      | 11:00  | 09:00   |
      | 09:00  | 09:00   |

  Cenário: Impedir data passada
    Quando preencho "Data" com "2000-01-01"
    E clico em "Revisar e solicitar"
    Então vejo o texto "Escolha a data de hoje ou uma data futura."

  Cenário: Revisar e concluir uma demonstração
    Quando preencho "Atividade" com "Aula BDD"
    E clico em "Revisar e solicitar"
    Então vejo o título "Revise sua reserva"
    E vejo o texto "Aula BDD"
    Quando clico em "Concluir demonstração"
    Então vejo o título "Revisão concluída"
    E vejo o texto "Demonstração: os dados ainda não são enviados ao serviço de reservas. A disponibilidade e a aprovação serão verificadas após a integração."

  Cenário: Voltar à edição preservando os dados
    Quando preencho "Atividade" com "Aula BDD"
    E clico em "Revisar e solicitar"
    E clico em "Voltar e editar"
    Então o campo "Atividade" contém "Aula BDD"
