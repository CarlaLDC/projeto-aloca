# ADR-0005: Arquitetura implementada do Aloca

- **Estado:** Documentado a partir do código; não representa aprovação de produção.
- **Data:** 2026-11-12.
- **Base:** develop, commit 1cc881aad1ea4fdd9b093855a74eb63715658644.

## Contexto

A arquitetura anterior misturava o planejamento de reservas com propostas de batch e treinamento de modelos. O snapshot integrado contém Angular, uma API Java e persistência relacional. É necessário distinguir o implementado das propostas históricas para a apresentação e revisão do projeto.

## Decisões observadas

1. Monólito modular Spring Boot, organizado em api, servico, dominio, repositorio, seguranca e config; frontend Angular separado.
2. Java 21 e Spring Boot 4.1.2 no pom.xml. O requisito acadêmico pede 3.x: esta divergência permanece aberta.
3. H2 em arquivo no desenvolvimento, MySQL como perfil alternativo e MySQL com Testcontainers nos testes de integração.
4. Reservas transacionais: recursos travados por id e professor com PESSIMISTIC_WRITE; consultas após lock com READ_COMMITTED. Reserva também usa @Version.
5. SOLICITADA já ocupa agenda. Aprovação automática para recursos livres de restrição; somente Responsável decide restritos.
6. Eventos locais síncronos acoplam a reserva às movimentações na mesma transação, sem broker.
7. Auditoria em HistoricoReserva, notificações simuladas no banco e ciclo temporal pelo CicloReservaAgendador.
8. JWT e autorização na API. O guard de frontend está aplicado à administração; os outros acessos dependem também das verificações da API.

## Alternativas e consequências

As propostas antigas de mecanismo de consistência independente de tecnologia foram concretizadas pelas travas JPA e pelo isolamento configurado. Locks dão uma seção crítica compartilhada pelo banco, mas podem introduzir espera; throughput precisa ser medido.

Eventos síncronos evitam uma fila operacional adicional e mantêm atomicidade com materiais; falha do ouvinte pode desfazer a operação da reserva. Isso não oferece entrega assíncrona ou integração externa.

H2 em arquivo facilita execução local. O ambiente MySQL permite verificar semântica real de persistência e concorrência; sucesso de um ambiente não deve ser presumido para outro sem execução.

## Relação com ADRs anteriores

| ADR | Situação nesta revisão |
|---|---|
| 0001 | Proposta de consistência concretizada, para este snapshot, pelas decisões 3–5 acima |
| 0002 | Proposta histórica de batch; sem implementação neste snapshot |
| 0003 | Proposta histórica de governança de modelos; sem implementação neste snapshot |
| 0004 | Proposta histórica de limites online/batch; controles atuais descritos em arquitetura.md |

Os textos anteriores são preservados e recebem aviso. Não se declara que uma proposta de ML tenha sido implementada ou que tenha sido excluída por decisão anterior da equipe.

## Evidência e limites

Fontes: ReservaService, AprovacaoService, MovimentacaoService, Reserva, SecurityConfig, repositórios de recursos/professor, application.properties e pom.xml. ConcorrenciaReservaTest possui cinco repetições com duas chamadas simultâneas, sala e material comuns, esperando uma única reserva aceita. A revisão documental não executou esse teste e não equivale a homologação de segurança ou desempenho.

Diagramas e sequências: [arquitetura.md](../arquitetura.md).
