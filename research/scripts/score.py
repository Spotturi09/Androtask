import json,time,re,sys
f=json.load(open(sys.argv[1])); dc=json.load(open(sys.argv[2]))
now=time.time()
HIGH={'Finance','Business','Productivity','Tools','Photography','Health & Fitness','Education','Medical'}
def price_max(s):
    v=[float(x) for x in re.findall(r'\$([\d.]+)',s or '')]; return max(v) if v else 0
rows=[]
for x in f:
    if not x['generic']: continue
    n=dc.get(x['developerId'])
    if n is None or n>=10: continue
    inst=x['realInstalls'] or x['minInstalls']
    upd=int(x['updated']) if x['updated'] else now
    stale=(now-upd)/86400
    mau=inst*0.08
    arpu_ads=0.12 if x['genre'] in HIGH else 0.05
    ads=mau*arpu_ads if x['containsAds'] else 0
    pm=price_max(x['inAppProductPrice'])
    iap=mau*0.01*min(pm or 4.99,9.99) if x['offersIAP'] else 0
    paid=(0 if x['free'] else (x['price'] or 0)*inst*0.7/max(12,(now-upd)/2.6e6+12))
    rev=ads+iap+paid
    score=(x['score'] or 0)
    vuln=(stale>180)+(3.5<=score<=4.3)
    rows.append(dict(id=x['appId'],t=x['title'][:40],g=x['genre'],dev=x['developer'][:24],n=n,inst=inst,sc=round(score,2),rt=x['ratings'],ads=x['containsAds'],iap=x['offersIAP'],pr=x['inAppProductPrice'],price=x['price'],stale=int(stale),rev=int(rev),vuln=vuln,kw=x['kw']))
rows.sort(key=lambda r:-r['rev'])
json.dump(rows,open(sys.argv[3],'w'),indent=1)
for r in rows[:70]: print(r['rev'],r['vuln'],r['id'],'|',r['t'],'|',r['g'],'|',r['dev'],r['n'],'|',r['inst'],r['sc'],r['rt'],'|ads' if r['ads'] else '','iap' if r['iap'] else '',r['pr'],r['price'],'| upd',r['stale'],'d')
print(len(rows))
