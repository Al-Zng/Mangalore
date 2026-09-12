# Mangalore

تطبيق Android أصلي بـ Kotlin و Jetpack Compose مستوحى من واجهة Mangamello: واجهة عربية RTL داكنة، اكتشاف مانغا، تفاصيل الأعمال، الفصول، المكتبة، السجل، الملف الشخصي، الإعدادات، وقارئ تجريبي.

## ما تم إصلاحه

- إضافة بيانات أولية واقعية حتى لا تبدأ واجهة المنزل والبحث بقائمة فارغة.
- إضافة طبقة `MangaloreScraper` typed تستخرج القوائم، تفاصيل العمل، الفصول، وصفحات القارئ من بنية Madara المستخدمة في `mangalik.net`.
- إضافة تخزين مؤقت للـ HTML داخل cache التطبيق، وملفات تعريف ارتباط مستمرة، وإعادة محاولة تدريجية للطلبات العابرة.
- إضافة `ChallengeWebViewDialog` تفاعلي. لا يظهر عند تشغيل التطبيق أو عند فتح الصفحة الرئيسية؛ يظهر فقط عندما تكتشف طبقة الشبكة استجابة 403 أو مؤشرات Cloudflare/Turnstile. بعد إكمال التحقق، تُعاد كوكيز الجلسة إلى طبقة العميل.
- إضافة صلاحية `INTERNET` واعتماديات OkHttp وJsoup وCoroutines.

## التشغيل محليًا

افتح المشروع في Android Studio ثم شغّل `app` على جهاز Android أو محاكي. الحد الأدنى Android 8.0 (API 26).

## بنية scraper

الملف `app/src/main/java/com/mangalore/app/data/MangaloreScraper.kt` هو نقطة الدخول:

- `search(query)` للبحث.
- `catalog(page)` لقائمة الأعمال.
- `details(url)` للبيانات الوصفية وقائمة الفصول.
- `reader(url)` لمصفوفة صور الفصل.

`ChallengeHandler` مقصود كواجهة بين طبقة الشبكة وواجهة Compose؛ يمكن ربطه بحالة شاشة `ChallengeWebViewDialog` عند بناء تدفق التحميل الحقيقي.

## البناء والنشر

ملف `.github/workflows/main.yml` يبني APK إصدار Release موقّعًا عند دفع tag بصيغة `v*`، ثم يرفعه كأصل داخل GitHub Release. لا يستخدم workflow قسم Artifacts، ولا يرفع APK إلى أي مكان آخر.

الأسرار المطلوبة في إعدادات GitHub Actions هي:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

لإنشاء إصدار جديد:

```bash
git tag v1.0.0
git push origin v1.0.0
```

سيتم إنشاء Release تلقائيًا وإرفاق `Mangalore-v1.0.0.apk` به.
