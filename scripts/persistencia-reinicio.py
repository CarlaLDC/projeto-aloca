#!/usr/bin/env python3
"""Reinício de processo real com PostgreSQL (ALOCA_DB_URL); toda preparação/consulta usa HTTP."""
import datetime as dt
import json, os, pathlib, re, socket, subprocess, tempfile, time, urllib.request, urllib.error

# Pasta de evidências: só um nome de pasta, sempre dentro do diretório atual (sem caminho vindo de fora).
nome_saida=os.path.basename(os.getenv('EVIDENCE_DIR','evidencias-candidato'))
if not re.fullmatch(r'[A-Za-z0-9._-]+',nome_saida) or nome_saida in ('.','..'):
    raise SystemExit('EVIDENCE_DIR deve ser só o nome de uma pasta (letras, números, ".", "_" ou "-").')
out=pathlib.Path.cwd()/nome_saida;out.mkdir(parents=True,exist_ok=True)
jar=pathlib.Path(os.getenv('ALOCA_JAR') or next(pathlib.Path('backend/target').glob('aloca-backend-*.jar'))).resolve()
db_url=os.getenv('ALOCA_DB_URL','jdbc:postgresql://localhost:5432/aloca')
def call(base,path,token=None,data=None,method=None):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    req=urllib.request.Request(base+path,headers=headers,data=json.dumps(data).encode() if data is not None else None,method=method)
    with urllib.request.urlopen(req,timeout=15) as r:return json.load(r) if r.status!=204 else None

def login(base,email):return call(base,'/api/auth/login',data={'email':email,'senha':'aloca123'})['token']
def stop(p):
    p.terminate()
    try:p.wait(timeout=25)
    except subprocess.TimeoutExpired:p.kill();p.wait(timeout=10)
    assert p.poll() is not None

def start(db,port,log):
    p=subprocess.Popen(['java','-jar',str(jar),f'--server.port={port}',f'--spring.datasource.url={db}'],stdout=log,stderr=subprocess.STDOUT)
    base=f'http://127.0.0.1:{port}'
    deadline=time.monotonic()+120
    while time.monotonic()<deadline:
        if p.poll() is not None:raise RuntimeError('API encerrou antes de iniciar; consulte o log')
        try:login(base,'ana.martins@aloca.edu.br');return p,base
        except (OSError,urllib.error.HTTPError):time.sleep(1)
    stop(p);raise TimeoutError('API não iniciou')

with tempfile.TemporaryDirectory(prefix='aloca-reinicio-') as folder:
    db=db_url
    with socket.socket() as s:s.bind(('127.0.0.1',0));port=s.getsockname()[1]
    with (out/'reinicio-backend.log').open('w') as log:
        p,base=start(db,port,log)
        try:
            token=login(base,'ana.martins@aloca.edu.br');admin=login(base,'patricia.souza@aloca.edu.br')
            sala=call(base,'/api/admin/recursos',admin,{'nome':'Sala persistência','tipo':'SALA','localizacao':'Teste reinício','capacidade':15,'restrito':False,'abertura':'07:00','fechamento':'22:00'})
            data=(dt.date.today()+dt.timedelta(days=200)).isoformat()
            reserva=call(base,'/api/reservas',token,{'atividade':'Evidência reinício','data':data,'inicio':'10:00','fim':'11:00','espacoId':sala['id'],'professorId':None,'materialIds':[]})
            codigo=reserva['codigo']
            # Criação e alteração de estado reais, preservadas antes da parada.
            call(base,'/api/reservas/'+codigo+'/cancelamento',token,{'motivo':'Teste de persistência após reinício'})
            antes=call(base,'/api/reservas/'+codigo,token)
            assert antes['status']=='CANCELADA' and len(antes['historico'])>=3
            pid_antes=p.pid
        finally:stop(p)
        p,base=start(db,port,log)
        try:
            depois=call(base,'/api/reservas/'+codigo,login(base,'ana.martins@aloca.edu.br'))
            assert antes==depois,'Reserva ou histórico diferente após reiniciar o processo'
            recursos=call(base,'/api/admin/recursos',login(base,'patricia.souza@aloca.edu.br'))
            assert any(x['id']==sala['id'] and x['nome']==sala['nome'] for x in recursos)
            assert p.pid!=pid_antes
            result={'status':'APROVADO','banco':'PostgreSQL','processos_distintos':True,'reserva':codigo,'estado':depois['status'],'eventos_iguais':len(depois['historico']),'consulta_integral_igual':True,'recurso_preservado':True}
            (out/'persistencia-reinicio.json').write_text(json.dumps(result,indent=2,ensure_ascii=False))
            print(json.dumps(result,ensure_ascii=False))
        finally:stop(p)
