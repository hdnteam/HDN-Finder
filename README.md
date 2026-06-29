# SNI Finder - یابنده بهترین SNI

اپلیکیشن اندرویدی برای پیدا کردن بهترین SNI بر اساس Real Delay (لیتنسی واقعی).

## قابلیت‌ها

- ✅ پشتیبانی از پروتکل‌های **VLESS**, **VMess**, **Trojan**
- ✅ اندازه‌گیری **Real Delay** (زمان واقعی TLS Handshake)
- ✅ لیست پیش‌فرض SNI شامل CDNها و سرویس‌های معروف
- ✅ امکان وارد کردن لیست سفارشی SNI
- ✅ مرتب‌سازی نتایج بر اساس سرعت (کمترین لیتنسی اول)
- ✅ کپی مستقیم کانفیگ با SNI جدید
- ✅ تنظیم تایم‌اوت
- ✅ رابط کاربری Material 3 فارسی

## نحوه کار

1. کانفیگ خود را (vless://, vmess://, trojan://) در فیلد ورودی پیست کنید
2. دکمه "تجزیه کانفیگ" را بزنید
3. در صورت نیاز لیست SNI سفارشی وارد کنید
4. "شروع اسکن" را بزنید
5. برنامه هر SNI را تست می‌کند و زمان واقعی TLS Handshake را اندازه می‌گیرد
6. نتایج به ترتیب سرعت نمایش داده می‌شوند
7. با زدن آیکون Share، کانفیگ با SNI جدید کپی می‌شود

## نحوه تست SNI

برنامه برای هر SNI:
1. یک اتصال TLS به سرور کانفیگ برقرار می‌کند
2. SNI مورد نظر را در TLS ClientHello قرار می‌دهد
3. زمان کامل Handshake (از اتصال تا تکمیل TLS) را اندازه می‌گیرد
4. این زمان همان **Real Delay** واقعی است

## ساخت پروژه

```bash
# باز کردن در Android Studio
# یا با خط فرمان:
./gradlew assembleDebug
```

## حداقل نیازمندی‌ها

- Android 7.0 (API 24) به بالا
- دسترسی به اینترنت

## ساختار پروژه

```
app/src/main/java/com/snifinder/app/
├── model/
│   └── SniResult.kt          # مدل‌های داده
├── util/
│   ├── ConfigParser.kt        # تجزیه‌کننده کانفیگ
│   ├── SniTester.kt           # تست‌کننده SNI (Real Delay)
│   ├── SniListProvider.kt     # لیست SNIها
│   └── TrustAllManager.kt     # مدیریت TLS
├── viewmodel/
│   └── MainViewModel.kt       # ViewModel اصلی
├── ui/
│   ├── theme/Theme.kt         # تم
│   └── screens/MainScreen.kt  # صفحه اصلی
└── MainActivity.kt            # اکتیویتی
```
