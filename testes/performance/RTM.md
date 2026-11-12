# Matriz de rastreabilidade (RTM)

Legenda: **Parcial** = rota SPA testável, sem regra de negócio; **Bloqueado** = depende de API/ambiente/dados; **Fora do escopo** = exige teste funcional, segurança ou cobertura específico. Nenhum requisito foi aprovado sem execução.

| ID | Requisito | Teste | Critério | Estado |
|---|---|---|---|---|
| RF-01 | Acesso por perfil | Autenticação/autorização | Permissões por perfil | Bloqueado |
| RF-02 | Cadastro de recursos | CRUD API | Dados persistidos e validados | Bloqueado |
| RF-03 | Pesquisa/disponibilidade | SPA-02; API-PESQ | p95 <= 2 s e filtros corretos | Parcial |
| RF-04 | Criar/alterar/cancelar reserva | API-RES | Operação correta; p95 <= 3 s | Bloqueado |
| RF-05 | Evitar horários inválidos/conflitos | API-CONFLITO | Rejeitar sobreposição; aceitar consecutivas | Bloqueado |
| RF-06 | Evitar dupla reserva simultânea | API-CONCORRENCIA | Exatamente uma aceita | Bloqueado |
| RF-07 | Aprovação de recurso restrito | API-APROVACAO | Sem uso antes da aprovação | Bloqueado |
| RF-08 | Bloqueio de manutenção | API-BLOQUEIO | Recurso indisponível no período | Bloqueado |
| RF-09 | Retirada/devolução | API-MOV | Registro com reserva, data/hora/usuário | Bloqueado |
| RF-10 | Histórico | API-HIST | Alterações auditáveis | Bloqueado |
| RF-11 | Mensagens | Fluxos funcionais | Confirmação/recusa claras | Bloqueado |
| RF-12 | Relatórios | SPA-07; API-REL | Totais corretos; p95 <= 3 s | Parcial |
| RF-13 | Responsividade/documentação | SPA-01..07; visual | Funciona em 360 px e 1280 px | Parcial |
| RF-14 | Agenda do professor | API-AGENDA | Agenda própria e autorizada | Bloqueado |
| RNF-01 | Desempenho | SPA-01..07; API futura | 50 usuários; p95 disponibilidade <= 2 s; p95 demais <= 3 s | Parcial |
| RNF-02 | Consistência concorrente | API-CONCORRENCIA | Uma única solicitação aceita | Bloqueado |
| RNF-03 | Segurança | Testes de perfil/validação | 100% perfil; zero crítica/alta aberta | Fora do escopo |
| RNF-04 | Persistência | Reinício de serviço | Nenhum dado confirmado perdido | Bloqueado |
| RNF-05 | Usabilidade | Viewports 360/1280 | Pesquisa, reserva, cancelamento | Fora do escopo |
| RNF-06 | Cobertura | Pipeline de cobertura | >=80% linhas e >=70% branches | Fora do escopo |
| RNF-07 | Qualidade | Triagem/análise | Zero bugs críticos; zero vulnerabilidades críticas abertas | Fora do escopo |
| RNF-08 | Rastreabilidade | Revisão RTM | 100% requisitos críticos com risco, teste e evidência | Parcial |
| RNF-09 | CI | GitHub Actions por PR | Build e testes em cada PR | Fora do escopo |

## Cenários SPA no JMX
SPA-01 /login; SPA-02 /disponibilidade; SPA-03 /nova-reserva; SPA-04 /minhas-reservas; SPA-05 /aprovacoes; SPA-06 /administracao; SPA-07 /relatorios.

**Conclusão:** 0 requisitos de negócio aprovados por teste de carga. RF-03/RF-12/RNF-01 têm cobertura parcial apenas de entrega HTTP de páginas. RF-06/RNF-02 permanecem bloqueados.
