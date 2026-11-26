import json,re,time,sys,urllib.request,urllib.parse
f=json.load(open(sys.argv[1]))
devs={x['developerId'] for x in f if x['generic']}
out={}
for d in devs:
    u=("https://play.google.com/store/apps/dev?id="+d) if d.isdigit() else ("https://play.google.com/store/apps/developer?id="+urllib.parse.quote_plus(d))
    try:
        h=urllib.request.urlopen(urllib.request.Request(u+"&hl=en",headers={"User-Agent":"Mozilla/5.0"}),timeout=30).read().decode('utf8','ignore')
        out[d]=len(set(re.findall(r'/store/apps/details\?id=([\w.]+)',h)))
    except Exception as e: out[d]=None
    time.sleep(0.4)
json.dump(out,open(sys.argv[2],'w'))
print("done",len(out))
