import json,sys,time,re,urllib.request
from bs4 import BeautifulSoup as B
rows=json.load(open(sys.argv[1])); out={}
def sec(t,label):
    m=re.search(r'\| \d+ \| '+re.escape(label)+r' SDKs \| (.*?)(?: \| [^|]+ has \| \d+ \| | \| App Details)',t)
    return [x.strip() for x in m.group(1).split('|') if x.strip()] if m else []
def secs(t):
    return {m.group(2):[x.strip() for x in m.group(3).split('|') if x.strip()] for m in re.finditer(r'has (\d+) ([A-Za-z:& ]+?) SDKs \| (.*?)(?= \| [^|]+? has \d+ [A-Z]| \| App Details)',t)}
for r in rows:
    pid=r['id']
    try:
        h=urllib.request.urlopen(urllib.request.Request("https://appgoblin.info/apps/"+pid,headers={"User-Agent":"Mozilla/5.0 (research script)"}),timeout=40).read().decode('utf8','ignore')
        t=re.sub(r'\s+',' ',B(h,'html.parser').get_text(' | ',strip=True))
        g=lambda p:(re.search(p,t).group(1) if re.search(p,t) else None)
        out[pid]=dict(mau=g(r'\| ([\d.]+[KMB]?) \| monthly active users'),rev=g(r'\| (\$[\d.]+[KMB]?\+?) \| monthly revenue est'),
          iap=g(r'IAP (\d+)%'),adpct=g(r'Ad (\d+)%'),monthly_inst=g(r'Monthly \| ([+\-][\d.]+[KMB]?)'),
          sdk=secs(t))
    except Exception as e: out[pid]={'err':str(e)[:80]}
    time.sleep(2)
json.dump(out,open(sys.argv[2],'w'),indent=1); print('done',len(out))
