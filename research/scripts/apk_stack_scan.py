#!/usr/bin/env python3
"""Detect the build framework of an APK/XAPK/APKS by its file signatures.
Usage: python3 apk_stack_scan.py app1.apk [app2.apk ...]   (works on .apk, .xapk, .apks, .aab)
Only reads the zip index (and dex bytes for package-name markers); never executes anything."""
import sys, zipfile, io, re, json

SIGS = [
 ("FlutterFlow",            lambda n,d: (any(x.endswith("libflutter.so") or "flutter_assets/" in x for x in n)) and re.search(rb"flutterflow|ff_theme|FFAppState", d)),
 ("Flutter",                lambda n,d: any(x.endswith("libflutter.so") for x in n) or any("flutter_assets/" in x for x in n)),
 ("React Native / Expo",    lambda n,d: "assets/index.android.bundle" in n or any(x.endswith(("libreactnativejni.so","libhermes.so","libreactnative.so")) for x in n)),
 ("Capacitor (Ionic)",      lambda n,d: any(x.endswith("capacitor.config.json") for x in n) or b"com/getcapacitor" in d),
 ("Cordova (Ionic)",        lambda n,d: any(x.endswith("cordova.js") for x in n) or b"org/apache/cordova" in d),
 ("Kodular / App Inventor / Niotron", lambda n,d: re.search(rb"com/google/appinventor|io/kodular|io/makeroid|com/niotron", d) or any(x.startswith("assets/") and x.endswith(".scm") for x in n)),
 ("Thunkable",              lambda n,d: b"thunkable" in d.lower()),
 ("Unity",                  lambda n,d: any(x.endswith(("libunity.so","libil2cpp.so")) for x in n) or any(x.startswith("assets/bin/Data/") for x in n)),
 (".NET MAUI / Xamarin",    lambda n,d: any("libmonodroid" in x or "libmonosgen" in x for x in n)),
 ("Median/GoNative WebView",lambda n,d: re.search(rb"io/gonative|co/median", d)),
 ("Generic WebView wrapper",lambda n,d: any(x.startswith("assets/www/") for x in n)),
]
COMPLEX = [(b"com/google/ar/core","ARCore"),(b"android/bluetooth/le","BLE"),(b"org/tensorflow","TFLite"),(b"org/webrtc","WebRTC")]
MONET = [(b"com/google/android/gms/ads","AdMob"),(b"com/applovin","AppLovin"),(b"com/unity3d/ads","Unity Ads"),(b"com/ironsource","ironSource"),(b"com/facebook/ads","Meta AN"),
         (b"com/revenuecat","RevenueCat"),(b"com/android/billingclient","Play Billing"),(b"com/adapty","Adapty"),(b"com/qonversion","Qonversion"),(b"com/superwall","Superwall")]
BACKEND = [(b"com/google/firebase/firestore","Firestore"),(b"io/supabase","Supabase"),(b"com/parse","Parse"),(b"com/onesignal","OneSignal")]

def inner_apks(path):
    z = zipfile.ZipFile(path)
    names = z.namelist()
    if any(x.endswith(".apk") for x in names):              # xapk / apks bundles
        for x in names:
            if x.endswith(".apk"):
                yield zipfile.ZipFile(io.BytesIO(z.read(x)))
    else:
        yield z

def scan(path):
    names, dex = [], b""
    for z in inner_apks(path):
        n = z.namelist(); names += n
        for x in n:
            if x.endswith(".dex") or (x.startswith("assets/") and x.endswith((".bundle",".json",".js"))) or x.endswith("libapp.so"):
                dex += z.read(x)
    n = set(names); low = names
    stacks = [s for s,f in SIGS if f(low, dex)]
    if "FlutterFlow" in stacks and "Flutter" in stacks: stacks.remove("Flutter")
    if "Generic WebView wrapper" in stacks and len(stacks) > 1: stacks.remove("Generic WebView wrapper")
    if not stacks: stacks = ["Native (Kotlin/Java)"]
    return {"file": path, "stack": stacks,
            "complex_native": [l for s,l in COMPLEX if s in dex] + (["custom NDK libs"] if any(x.endswith(".so") and not re.search(r"lib(flutter|app|hermes|react|unity|il2cpp|c\+\+_shared|mono|sqlite|jsc|fbjni|yoga|imagepipeline|folly|gle|native-|realm|crashlytics|sentry|tensorflow|mmkv)", x) for x in names) else []),
            "monetization_sdks": [l for s,l in MONET if s in dex],
            "backend_sdks": [l for s,l in BACKEND if s in dex]}

if __name__ == "__main__":
    for p in sys.argv[1:]:
        try: print(json.dumps(scan(p)))
        except Exception as e: print(json.dumps({"file": p, "error": str(e)}))
