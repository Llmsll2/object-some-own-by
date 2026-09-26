# Domino Score — بناء APK بدون Android Studio

هذا المشروع مهيأ للبناء تلقائيًا عبر GitHub Actions.

## الطريقة المختصرة
1. أنشئ مستودع GitHub جديدًا.
2. ارفع كل ملفات هذا المشروع إلى المستودع.
3. افتح تبويب **Actions**.
4. اختر **Build Domino Score APK**.
5. اضغط **Run workflow**.
6. بعد انتهاء البناء افتح نتيجة التشغيل، ومن قسم **Artifacts** نزّل `DominoScore-debug-apk`.
7. فك الضغط وستجد `app-debug.apk` وثبّته على Android.

لا يحتاج المشروع إلى Android Studio عند استخدام GitHub Actions.
