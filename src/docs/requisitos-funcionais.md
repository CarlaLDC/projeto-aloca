# Requisitos funcionais

O sistema deve organizar reservas de salas, professores e materiais sem conflitos de horário.

## Requisitos

- **RF-01 - Acesso:** o sistema deve permitir a entrada de usuários e controlar o acesso de acordo com o perfil: Solicitante, Responsável, Administrador ou Professor.
	- **Verificação:** cada perfil só consegue acessar as funções permitidas.
	- **Personas:** todas.

- **RF-02 - Cadastro de recursos:** o Administrador deve cadastrar, consultar, alterar e desativar salas, professores e materiais.
	- **Verificação:** os dados salvos aparecem na consulta e campos obrigatórios não podem ficar vazios.
	- **Persona:** Administrador.

- **RF-03 - Pesquisa:** o sistema deve permitir pesquisar recursos por tipo, capacidade, localização, competência e disponibilidade.
	- **Verificação:** os resultados devem corresponder aos filtros escolhidos.
	- **Personas:** Solicitante e Administrador.

- **RF-04 - Reservas:** o Solicitante deve criar, alterar e cancelar suas próprias reservas.
	- **Verificação:** cada reserva deve informar recurso, solicitante, data e horários; o sistema deve confirmar o resultado.
	- **Persona:** Solicitante.

- **RF-05 - Conflitos de horário:** o sistema deve impedir horários inválidos e reservas sobrepostas para a mesma sala, professor ou material.
	- **Verificação:** o horário final deve ser posterior ao inicial; horários sobrepostos são recusados; reservas consecutivas são permitidas.
	- **Personas:** Solicitante, Responsável e Professor.

- **RF-06 - Reservas simultâneas:** o sistema deve impedir que duas solicitações simultâneas reservem o mesmo recurso no mesmo horário.
	- **Verificação:** em um teste com duas solicitações conflitantes, somente uma pode ser aceita.
	- **Personas:** Solicitante e Administrador.

- **RF-07 - Aprovação:** o sistema deve exigir aprovação do Responsável para recursos restritos.
	- **Verificação:** uma reserva restrita não pode ser usada antes da aprovação.
	- **Personas:** Solicitante e Responsável.

- **RF-08 - Manutenção:** o Administrador deve poder bloquear recursos durante manutenção ou indisponibilidade.
	- **Verificação:** um recurso bloqueado não pode ser reservado durante o período informado.
	- **Personas:** Administrador e Solicitante.

- **RF-09 - Materiais:** o Responsável deve registrar a retirada e a devolução de materiais e equipamentos.
	- **Verificação:** cada registro deve guardar o material, a reserva, a data e hora e o usuário responsável.
	- **Persona:** Responsável.

- **RF-10 - Histórico:** o sistema deve guardar os estados e as mudanças das reservas.
	- **Verificação:** cada mudança registra usuário, data e estado anterior e novo. Reservas em uso não podem ser apagadas.
	- **Personas:** Solicitante, Responsável e Administrador.

- **RF-11 - Mensagens:** o sistema deve confirmar solicitações, alterações, cancelamentos e decisões de aprovação, ou informar o motivo da recusa.
	- **Verificação:** cada ação apresenta uma mensagem clara; a notificação pode ser simulada.
	- **Personas:** Solicitante e Responsável.

- **RF-12 - Relatórios:** o sistema deve apresentar utilização dos recursos, carga horária alocada e conflitos evitados.
	- **Verificação:** o usuário pode consultar os totais por período e recurso.
	- **Personas:** Administrador e Responsável.

- **RF-13 - Interface e documentação:** o sistema deve funcionar em celular e computador e documentar seus fluxos ou sua API.
	- **Verificação:** as funções principais são acessíveis em telas de 360 px e 1280 px; os fluxos ou dados da API estão descritos.
	- **Personas:** todas.

- **RF-14 - Agenda do professor:** o Professor deve poder consultar sua própria agenda.
	- **Verificação:** a agenda mostra as reservas associadas ao professor, sem permitir que ele altere cadastros ou reservas de outras pessoas.
	- **Persona:** Professor.

## Regras simples

- O horário final deve ser posterior ao horário inicial.
- A mesma sala, professor ou material não pode estar em duas reservas ao mesmo tempo.
- Recursos em manutenção não podem ser reservados durante o bloqueio.
- Apenas o Responsável aprova reservas de recursos restritos.
- Toda mudança de estado fica no histórico.
