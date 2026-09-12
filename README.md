# Mangalore

تطبيق Android أصلي بـ Kotlin و Jetpack Compose مستوحى من واجهة Mangamello: واجهة عربية RTL داكنة، اكتشاف مانغا، تفاصيل الأعمال، الفصول، المكتبة، السجل، الملف الشخصي، الإعدادات، وقارئ تجريبي.

## التشغيل محليًا

افتح المشروع في Android Studio ثم شغّل `app` على جهاز Android أو محاكي. الحد الأدنى Android 8.0 (API 26).

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
