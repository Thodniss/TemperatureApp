# 🌊 Lillesand Badetemperaturer

> Sjekk badetemperaturen i Lillesand – før du hopper i!

An Android app that fetches and displays real-time sea temperatures for bathing locations around Lillesand, Norway — complete with local dialect phrases for every reading.

---

## ✨ Features

- **Live temperatures** — Scrapes data directly from Lillesand Kommune, updated every 4 hours
- **5 bathing locations** — Guttebukta, Kaldvell, Orrehola, Springvannsheia, Langedalstjønna
- **Sørlandsk dialect** — Every temperature gets its own phrase: *"Sabla nyd!"*, *"Spiggent!"*, *"E kovner!"*
- **Tap for details** — Opens a detail card with a direct link to the location in Maps
- **Distance display** — Optionally shows how far you are from each spot (requires location permission)
- **Drag to reorder** — Arrange your favourite spots in any order
- **Sort by temperature** — Hottest first, or your custom order
- **Temperature history** — Keeps a 3-week log of all readings
- **Dark mode** — Saves your preference across sessions
- **Pull to refresh** — Always get the latest data

---

## 📸 Screenshots

| Today | Detail | History | Settings |
|-------|--------|---------|----------|
| <img src="docs/screenshot_today.png" width="180"/> | <img src="docs/screenshot_detail.png" width="180"/> | <img src="docs/screenshot_history.png" width="180"/> | <img src="docs/screenshot_settings.png" width="180"/> |

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | ViewModel + StateFlow |
| Networking | OkHttp |
| HTML parsing | Jsoup |
| Drag & drop | [reorderable](https://github.com/Calvin-LL/Reorderable) |
| Persistence | SharedPreferences |
| Min SDK | API 26 (Android 8.0) |

---

## 🗺️ Bathing Locations

| Location | Coordinates |
|----------|-------------|
| Guttebukta | 58.2443° N, 8.3805° E |
| Kaldvell | 58.2709° N, 8.4154° E |
| Orrehola | 58.2486° N, 8.3856° E |
| Springvannsheia | 58.2473° N, 8.3734° E |
| Langedalstjønna | 58.2596° N, 8.4045° E |

---

## 🚀 Building

1. Clone the repo
2. Open in Android Studio
3. Sync Gradle
4. Run on device or emulator (API 26+)

> No API keys or external services required — data is scraped from [lillesand.kommune.no](https://www.lillesand.kommune.no/Badeplasser.html).

---

## 🌡️ Temperature Scale

| Temperature | Colour | Phrase |
|-------------|--------|--------|
| < 8 °C | 🔵 Deep blue | *"Spiggent!"* / *"Jysla kaldt!"* |
| 8 – 14 °C | 🩵 Teal | *"Ikkje heilt smeig"* |
| 14 – 18 °C | 🟢 Green | *"Godt for faula!"* |
| 18 – 22 °C | 🟡 Amber | *"Sabla nyd!"* |
| > 22 °C | 🔴 Red | *"E kovner!"* |

---

## 📡 Data Source

Temperature data is fetched from the official Lillesand Kommune website and cached locally. The source updates at **02:00, 06:00, 10:00, 14:00, 18:00 and 22:00** Oslo time — the app shows a countdown to the next update.

---

## 👤 Author

Made by **Thomas** — for Lillesand, by Lillesand.

---

*Lillesand – den hvite by ved Skagerrak* 🏖️
