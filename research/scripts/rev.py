import json,sys,time
from google_play_scraper import reviews, Sort, app
ids=sys.argv[2:]; out={}
for i in ids:
    neg=[]; tok=None
    for _ in range(4):
        r,tok=reviews(i,lang='en',country='us',sort=Sort.NEWEST,count=200,continuation_token=tok)
        neg+=[dict(s=x['score'],d=str(x['at'])[:10],t=x['content']) for x in r if x['score']<=3]
        if len(neg)>=40 or not tok: break
        time.sleep(0.5)
    d=app(i,lang='en',country='us')
    out[i]=dict(neg=neg[:40],web=d.get('developerWebsite'),email=d.get('developerEmail'),addr=d.get('developerAddress'),desc=(d.get('description') or '')[:900],histogram=d.get('histogram'),released=d.get('released'),version=d.get('version'),androidVersion=d.get('androidVersion'),url=d.get('url'),dev=d.get('developer'),devId=d.get('developerId'))
    time.sleep(0.5)
json.dump(out,open(sys.argv[1],'w'),indent=1,default=str)
print('ok',len(out))
