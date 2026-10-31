# ADR-0004: Limites online/batch, segurança e operação

- **Estado:** Proposto; topologia, dados e controles institucionais pendentes.
- **Data:** 2026-10-27.
- **Decisores necessários:** arquitetura, operações/SRE, segurança e responsável por privacidade.

## Contexto

RNF-01 contém metas de desempenho propostas para pesquisa e operações comuns. A execução batch diária pode competir por CPU, memória, armazenamento ou conexões se compartilhar recursos. RNF-03 exige autenticação/autorização, validação e mensagens sem detalhes internos. RF-09/RF-10 associam ações a usuários; não há inventário de dados pessoais, finalidade, retenção ou topologia confirmados.

## Forças

- Não permitir que o treino bloqueie operações de reservas sem decisão explícita.
- Medir comportamento do online durante o batch em vez de assumir isolamento.
- Limitar acesso e minimizar exposição de dados em logs e artefatos.
- Não declarar conformidade LGPD sem identificar dados, finalidade e papéis.
- Controlar custo sem assumir orçamento ou volume inexistentes.

## Alternativas

1. Execução compartilhada: menor infraestrutura, possível interferência no caminho online.
2. Execução isolada: reduz competição, adiciona custo e operação.
3. Isolamento lógico com limite de recursos: meio-termo, depende da plataforma.
4. Logging detalhado com payload: facilita depuração, mas aumenta exposição; não recomendado.
5. Linhagem por referências/hash: reduz cópia de dados, depende da disponibilidade e retenção na fonte.

## Decisão proposta

Modelar batch e online como responsabilidades separadas. Não impor separação física até que topologia seja conhecida; testar RNF-01 durante carga de batch caso compartilhem recursos. Aplicar autorização por operação, menor privilégio, validação e mensagens sem detalhes internos. Por padrão, logs não devem conter payload, credenciais, tokens ou dados pessoais; registrar referências técnicas mínimas. Fazer inventário de dados e validar finalidade, retenção e controles LGPD com responsáveis institucionais antes de usar dados pessoais no treino.

Separação física, limites de recursos, observabilidade, alertas, retenção e controles específicos de LGPD permanecem **sem requisito e pendentes**. Não há orçamento, logo custo é medido antes de otimizar.

## Consequências

- A arquitetura pode começar como limites lógicos e evoluir para isolamento físico se evidência de desempenho exigir.
- Relatórios e logs devem ser suficientes para correlação sem reproduzir dados brutos.
- Os critérios de retenção e incidentes dependem de política institucional.
- Custo, latência e risco de privacidade precisam ser avaliados juntos.

## Riscos

- Compartilhamento pode degradar RNF-01; isolamento não aprovado pode acrescentar custo.
- Minimização excessiva pode prejudicar investigação se não houver identificadores de correlação seguros.
- Logs/artifacts com dados pessoais podem criar exposição; pseudonimização não elimina automaticamente obrigação legal.
- Não existe evidência atual de incidente ou de tratamento de dados pelo batch.

## Requisitos e evidências

- **Requisitos:** RF-01, RF-09, RF-10; RNF-01, RNF-03, RNF-08.
- **Cenários:** AT-02, AT-09, AT-10, AT-11.
- **Evidências propostas:** teste de autorização por perfil; análise de mensagens/logs de erro; teste de carga com e sem batch; inventário e classificação dos dados; revisão de acesso, retenção e registros. Metas de RNF-01 permanecem hipóteses pendentes de validação.
