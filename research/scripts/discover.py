import json, sys, time
from google_play_scraper import search, app
kws = ["habit tracker","water reminder","intermittent fasting","prayer times","bible verse daily","quotes maker","invoice maker","receipt scanner","budget planner","expense tracker","pdf scanner","qr code generator","photo to pdf","ai photo enhancer","wallpaper 4k","ringtone maker","sticker maker whatsapp","logo maker","resume builder","flashcards","period tracker","pregnancy tracker","baby feeding tracker","workout planner","step counter","calorie counter","meditation","sleep sounds","white noise","tarot reading","horoscope","astrology","dream interpretation","name meaning baby","love calculator","truck driver log","tip calculator","loan calculator","emi calculator","gold price","fuel log","car maintenance log","plant care reminder","dog training","cat translator","recipe planner","grocery list","shift work calendar","timesheet","password manager","vpn","translator offline","bible audio","quran","hindu calendar panchang","kundli","lottery results","sports betting tips","football predictions","crypto signals","stock market india","english speaking practice","learn spanish","ielts preparation","driving test","citizenship test","nclex","typing tutor","coloring book adults","ai chat","ai art generator","tattoo designs","nail art","hairstyle try on","outfit planner","countdown widget","widget photo","kids learning abc","math tricks"]
cand = {}
for k in kws:
    try:
        res = search(k, lang="en", country="us", n_hits=30)
    except Exception as e:
        print("ERR", k, e, file=sys.stderr); continue
    for r in res:
        if r.get("appId"): cand.setdefault(r["appId"], k)
    time.sleep(0.4)
print(len(cand), file=sys.stderr)
out = []
for pid, k in cand.items():
    try:
        d = app(pid, lang="en", country="us")
    except Exception as e:
        continue
    keep = {f: d.get(f) for f in ["appId","title","developer","developerEmail","developerWebsite","genre","realInstalls","minInstalls","score","ratings","reviews","free","price","offersIAP","inAppProductPrice","containsAds","lastUpdatedOn","updated","released","url","developerId"]}
    keep["kw"] = k
    out.append(keep)
    time.sleep(0.25)
json.dump(out, open(sys.argv[1],"w"), indent=1, default=str)
print("done", len(out), file=sys.stderr)
