"""Usage: python3 filter.py apps.json filtered.json — keep monetized apps with >=10K installs, flag generic support emails."""
import json,re,sys
gen=re.compile(r'@(gmail|googlemail|outlook|hotmail|yahoo|icloud|proton|protonmail|yandex|live|aol|mail)\.',re.I)
f=[]
for x in json.load(open(sys.argv[1])):
    inst=x['realInstalls'] or x['minInstalls'] or 0
    if (x['containsAds'] or x['offersIAP'] or not x['free']) and inst>=10000:
        x['generic']=bool(gen.search(x['developerEmail'] or '')); f.append(x)
json.dump(f,open(sys.argv[2],'w'),default=str); print(len(f))
