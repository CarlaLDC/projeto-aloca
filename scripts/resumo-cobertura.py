#!/usr/bin/env python3
import json,os,pathlib,subprocess,xml.etree.ElementTree as ET
r=ET.parse('backend/target/site/jacoco/jacoco.xml').getroot()
sha=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
counts={c.get('type'):{'cobertos':int(c.get('covered')),'perdidos':int(c.get('missed'))} for c in r.findall('counter')}
result={'checkout_sha':sha,'github_sha':os.getenv('GITHUB_SHA'),'run_id':os.getenv('GITHUB_RUN_ID'),'contadores':counts}
for tipo,meta in [('LINE',80),('BRANCH',70)]:
 c=counts[tipo];p=100*c['cobertos']/(c['cobertos']+c['perdidos']);result[tipo]={'percentual':round(p,2),'meta':meta,'aprovado':p>=meta}
out=pathlib.Path(os.getenv('EVIDENCE_DIR','evidencias-candidato'));out.mkdir(parents=True,exist_ok=True)
(out/'cobertura.json').write_text(json.dumps(result,indent=2))
print(json.dumps(result,indent=2))
assert all(result[t]['aprovado'] for t in ['LINE','BRANCH']),'Meta de cobertura não atingida'
