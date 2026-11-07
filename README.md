# Hospital 666 (Android, LAN co-op)

"Hospital 666" (Lokin, Steam) konseptidan ilhomlangan **original** Android o'yin. Kod va tekstura/ovozlar noldan yozilgan, tashqi kutubxona yo'q.


[![Build APK](https://github.com/shakhzodjonuzb-commits/Hospital666/actions/workflows/android.yml/badge.svg)](https://github.com/shakhzodjonuzb-commits/Hospital666/actions/workflows/android.yml)


## Qoidalar
- Siz cheksiz kasalxona yo'lagidasiz. Yo'lak oxirida **oldingi lift (▲)**, boshlanishda **orqa lift (▼)**.
- Yo'lakda **g'alati narsa** (anomaliya) bo'lmasa — oldingi liftga, bo'lsa — orqaga qaytib orqa liftga kiring.
- To'g'ri tanlov = keyingi qavat. Maqsad: **666-qavat**.
- Xato tanlov yoki hamshira (ta'qibchi) tutib olsa: **Oson** −1 qavat, **Oddiy** oxirgi 10 lik nazorat nuqtasiga, **Do'zax** 1-qavatga.
- Har qavat avtomatik saqlanadi.
- 13 xil anomaliya: arvoh, noto'g'ri qavat raqami, qonli devor, ko'zlar, yo'qolgan eshik, yo'qolgan karavot, qizil yorug'lik, miltillash, devordagi yozuv, ta'qibchi hamshira, ortiqcha aravacha, qonli pol, lift belgisi almashgan.

## Boshqaruv
Chap yarim ekran — joystick (yurish). O'ng yarim ekran — burilish. "⟲ 180°" — tez orqaga burilish. Chap yuqori ✕ — chiqish.

## LAN (1–4 kishi)
Hamma bir Wi-Fi yoki hotspotda bo'lsin. Bittasi "Xona ochish" (ekranda IP ko'rinadi), qolganlari "Xonaga qo'shilish" (IP bo'sh bo'lsa avtomatik topadi). Lift tanlovini birinchi kirgan o'yinchi hamma uchun hal qiladi. Host: TCP 36666, kashfiyot: UDP 36667.

## GitHub orqali build
1. Papkani GitHub reposiga push qiling.
2. Actions → "Build APK" → tugagach `Hospital666-APK` artifactidan `app-debug.apk` ni yuklab oling.
(Workflow Gradle 8.14 va JDK 17 ni o'zi o'rnatadi, gradle-wrapper.jar shart emas. O'zingizning wrapper jar/gradlew ni qo'ysangiz ham ishlaydi.)

## Fayllar
`Tex.kt` teksturalar · `FloorMap.kt` qavat/anomaliyalar · `Renderer.kt` 3D raycaster · `Game.kt` qoidalar/host mantiq · `Net.kt` LAN · `GameView.kt` chizish/boshqaruv · `Sfx.kt` ovoz · `MainActivity.kt` menyu · `GameActivity.kt`.
