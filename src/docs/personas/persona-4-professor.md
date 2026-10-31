# Persona 4: Professor

## Perfil

- **Nome fictício:** André Lima.
- **Função:** docente cujos horários são associados às atividades e reservas.
- **Perfil de uso:** consulta a própria agenda para saber quando e onde está alocado.
- **Familiaridade com tecnologia:** básica a intermediária; precisa encontrar a agenda sem navegar por funções administrativas.
- **Frequência de uso:** diariamente, especialmente antes das aulas e quando há mudanças na programação.

## Contexto

André pode ser selecionado como professor em uma reserva feita por um Solicitante. Ele precisa consultar seus compromissos para se preparar e identificar rapidamente alterações. A agenda deve estar ligada às reservas para que sua disponibilidade seja considerada quando o sistema verifica conflitos.

## Objetivos

- Consultar as próprias aulas e atividades reservadas.
- Ver data, horário, sala e estado de cada compromisso.
- Evitar que duas atividades sejam marcadas para o mesmo horário.
- Perceber alterações ou cancelamentos que afetem sua agenda.
- Informar ao Solicitante ou ao Responsável quando notar uma divergência.

## Atividades no sistema

- Entrar com sua conta de Professor.
- Abrir a própria agenda e escolher o período que deseja consultar.
- Consultar os detalhes das reservas associadas ao seu perfil.
- Verificar se uma reserva está solicitada, aprovada, cancelada ou concluída, conforme as permissões disponíveis.
- Comunicar uma divergência ao Solicitante ou ao Responsável.

## Dificuldades e necessidades

- Precisa de uma agenda atualizada e fácil de consultar.
- Precisa que horários conflitantes sejam bloqueados antes da confirmação da reserva.
- Precisa diferenciar solicitações pendentes de atividades confirmadas.
- Não deve ver informações pessoais desnecessárias de outros usuários.
- Não deve conseguir alterar cadastros ou reservas de outras pessoas.

## Permissões

- Pode consultar a própria agenda e os detalhes necessários de suas atividades.
- Pode receber associação a uma reserva feita por um Solicitante.
- Não pode criar ou aprovar reservas por padrão, alterar cadastros, bloquear recursos nem modificar a agenda de outros professores.
- Pode solicitar correção de uma informação por meio do Solicitante ou Responsável.

## Cenário de uso

André consulta a agenda da semana e vê uma aula aprovada com horário e sala. Um Solicitante tenta criar outra reserva que também utiliza André nesse mesmo período. O sistema detecta a sobreposição e recusa a nova solicitação, preservando a agenda sem conflito.

## Resultado esperado

André consulta seus compromissos e não é alocado em duas atividades simultâneas. Alterações relevantes ficam refletidas na agenda e no histórico da reserva.

## Requisitos relacionados

- **Funcionais:** RF-01, RF-05, RF-10, RF-11, RF-13 e RF-14.
- **Não funcionais:** RNF-01, RNF-02, RNF-03, RNF-05 e RNF-08.
