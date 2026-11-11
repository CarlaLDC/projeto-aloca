# language: pt
Funcionalidade: Acesso institucional na demonstração
  Cenário: Exigir os campos de acesso
    Dado que acesso "/login"
    Quando clico em "Entrar"
    Então vejo o texto "Informe um e-mail válido."
    E vejo o texto "Informe sua senha."

  Cenário: Rejeitar formato inválido de e-mail
    Dado que acesso "/login"
    Quando preencho "E-mail" com "email-invalido"
    E preencho "Senha" com "senha-de-teste"
    E clico em "Entrar"
    Então vejo o texto "Informe um e-mail válido."
    E permaneço em "/login"

  Cenário: Informar indisponibilidade sem simular autenticação
    Dado que acesso "/login"
    Quando preencho "E-mail" com "ana@instituicao.edu.br"
    E preencho "Senha" com "senha-de-teste"
    E clico em "Entrar"
    Então vejo o texto "O acesso ainda não está disponível. Aguarde a liberação pela instituição."
    E o campo "Senha" está vazio
    E permaneço em "/login"

  Cenário: Orientar recuperação de senha
    Dado que acesso "/login"
    Quando clico em "Esqueceu sua senha?"
    Então vejo o texto "Para recuperar sua senha, entre em contato com o suporte da sua instituição."
