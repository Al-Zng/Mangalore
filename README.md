# Mangalore

تطبيق Android أصلي بـ Kotlin و Jetpack Compose بواجهة عربية RTL داكنة لاكتشاف المانجا وقراءة الفصول.

## التنفيذ الحالي

التطبيق متصل بطبقة `MangaloreScraper` التي تستخرج الكتالوج، البحث، تفاصيل العمل، الفصول، وصور صفحات القارئ من بنية Madara المستخدمة في `mangalik.net`. عند فتح التطبيق يتم تحميل الكتالوج الحي مع عرض بيانات احتياطية عند تعذر الاتصال، وتظهر حالات تحميل وإعادة محاولة وأخطاء واضحة للمستخدم.

تستخدم الشاشة الرئيسية بيانات الشبكة الحية، ويستخدم البحث طلبًا مؤجلًا قصيرًا لتجنب الطلب مع كل ضغطة. تعرض شاشة العمل الوصف والفصول الحقيقية عند توفرها، ويمكن الضغط على الفصل لفتح قارئ عمودي يعرض كل صفحات الفصل بصور عالية الدقة مع عداد الصفحات وزر إغلاق مناسب للمساحات الصغيرة.

تتضمن طبقة الشبكة Cache محليًا لصفحات HTML، Cookies مستمرة للجلسة، وإعادة محاولة تدريجية للطلبات العابرة. كما يتم تمرير User-Agent وCookies الناتجة من التحقق إلى الطلبات اللاحقة.

## Cloudflare وTurnstile

لا يتم فتح نافذة التحقق عند تشغيل التطبيق أو عند فتح الصفحة الرئيسية. عند اكتشاف HTTP 403 أو مؤشرات Cloudflare/Turnstile، تتوقف العملية وتظهر `ChallengeWebViewDialog` تفاعلية داخل التطبيق. بعد أن يكمل المستخدم التحقق، يعاد `cf_clearance` وUser-Agent إلى عميل OkHttp ثم يعاد الطلب الأصلي تلقائيًا. لا يوجد تجاوز آلي أو إخفاء للتحدي.

## الملفات الرئيسية

- `app/src/main/java/com/mangalore/app/MainActivity.kt`: واجهة Compose، تحميل الكتالوج والبحث والتفاصيل والقارئ وحالات التحميل والخطأ.
- `app/src/main/java/com/mangalore/app/data/MangaloreScraper.kt`: النماذج، parser، الشبكة، cache، retry، والجلسة.
- `app/src/main/java/com/mangalore/app/ComposeChallengeHandler.kt`: جسر تعليق الطلب حتى انتهاء التحقق التفاعلي.
- `app/src/main/java/com/mangalore/app/ChallengeWebViewDialog.kt`: نافذة WebView الشرطية للتحقق.

## التشغيل

افتح المشروع في Android Studio ثم شغّل `app` على جهاز Android أو محاكي. الحد الأدنى Android 8.0 (API 26). يحتاج التطبيق إلى اتصال بالإنترنت عند تحميل البيانات الحية.

## البناء والنشر

ملف `.github/workflows/main.yml` يبني APK إصدار Release موقّعًا عند دفع tag بصيغة `v*`، ثم يرفعه كأصل داخل GitHub Release.

الأسرار المطلوبة في إعدادات GitHub Actions:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

لإنشاء إصدار جديد:

```bash
git tag v1.0.0
git push origin v1.0.0
```
