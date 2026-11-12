# language: pt
Funcionalidade: Aloca integrado com autenticação, API e persistência
  Como equipe de qualidade
  Quero validar as regras reais da aplicação
  Para garantir que frontend, API e banco estejam integrados

  Cenário: Autenticar solicitante e carregar recursos reais
    Dado que acesso a tela de login integrada
    Quando entro com o usuário de teste "ana.martins@aloca.edu.br"
    Então vejo a página "Visão geral"
    Quando acesso a disponibilidade
    Então recebo recursos cadastrados pela API

  Cenário: Impedir acesso não autorizado à administração
    Dado que acesso a tela de login integrada
    Quando entro com o usuário de teste "ana.martins@aloca.edu.br"
    Então a API de administração responde 403

  Cenário: Persistir uma nova reserva realizada pela interface
    Dado que acesso a tela de login integrada
    Quando entro com o usuário de teste "ana.martins@aloca.edu.br"
    E cadastro uma reserva futura pela interface
    Então vejo o código de reserva registrado
    E a reserva está persistida e disponível na API

  Cenário: Validar conflito de dupla reserva no backend
    Dado que acesso a tela de login integrada
    Quando entro com o usuário de teste "ana.martins@aloca.edu.br"
    E crio duas reservas para a mesma sala e horário pela API
    Então a segunda reserva recebe conflito HTTP 409
