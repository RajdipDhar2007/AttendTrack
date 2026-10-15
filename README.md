<div align="center">

<img src="docs/banner.svg" alt="AttendTrack animated banner" width="100%"/>

<a href="https://github.com/RajdipDhar2007/AttendTrack">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=22&pause=1000&color=8B5CF6&center=true&vCenter=true&width=700&lines=Track+every+class+in+one+tap+%F0%9F%8E%AF;Know+exactly+how+many+classes+you+can+bunk+%F0%9F%98%8E;Recover+to+your+target+with+a+plan+%F0%9F%93%88;Glassy%2C+animated%2C+and+actually+fun+to+open+%E2%9C%A8" alt="Typing animation"/>
</a>

<br/>

![Platform](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Gemini](https://img.shields.io/badge/Gemini%20AI-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)
![Room](https://img.shields.io/badge/Room%20DB-F59E0B?style=for-the-badge)
![Min SDK](https://img.shields.io/badge/minSdk-26-0EA5E9?style=for-the-badge)

**[Features](#-features) · [Screenshots](#-screenshots) · [Install](#-install-in-60-seconds) · [Build](#%EF%B8%8F-build-from-source) · [How it works](#-how-the-math-works)**

</div>

---

## 🎯 Why AttendTrack?

> *"Can I skip tomorrow's class?"*  AttendTrack answers that, instantly, with real math.

| 😩 Without it | 😎 With AttendTrack |
|---|---|
| Guessing your percentage | Live, animated percentage per subject |
| Panicking before exams | Exact number of classes needed to recover |
| Forgetting to mark classes | Smart reminders with one-tap actions |
| Messy notebooks | Timetable, exams and study plans in one place |

## 📸 Screenshots

<div align="center">

| Dashboard | Subjects: All Classes | Subjects: Theory |
|:---:|:---:|:---:|
| <img src="docs/screenshots/dashboard.jpg" width="230" alt="Dashboard"/> | <img src="docs/screenshots/subjects-all-classes.jpg" width="230" alt="Subjects, all classes"/> | <img src="docs/screenshots/subjects-theory.jpg" width="230" alt="Subjects, theory only"/> |

| Subjects: Labs Only | Monthly | Backfill |
|:---:|:---:|:---:|
| <img src="docs/screenshots/subjects-labs-only.jpg" width="230" alt="Subjects, labs only"/> | <img src="docs/screenshots/monthly.jpg" width="230" alt="Monthly calendar"/> | <img src="docs/screenshots/backfill.jpg" width="230" alt="Backfill past days"/> |

| Bunk Calculator | Timetable (Thu) | Timetable (Wed) |
|:---:|:---:|:---:|
| <img src="docs/screenshots/bunk-calculator.jpg" width="230" alt="Bunk calculator"/> | <img src="docs/screenshots/timetable-thursday.jpg" width="230" alt="Timetable, Thursday"/> | <img src="docs/screenshots/timetable-wednesday.jpg" width="230" alt="Timetable, Wednesday"/> |

| Study Planner | Exam Countdown | Themes |
|:---:|:---:|:---:|
| <img src="docs/screenshots/study-planner.jpg" width="230" alt="Study planner"/> | <img src="docs/screenshots/exam-countdown.jpg" width="230" alt="Exam countdown"/> | <img src="docs/screenshots/themes.jpg" width="230" alt="Themes"/> |

| AI Copilot | AI Copilot: Faculty | Import / Export |
|:---:|:---:|:---:|
| <img src="docs/screenshots/ai-copilot-chat.jpg" width="230" alt="AI Copilot chat"/> | <img src="docs/screenshots/ai-copilot-faculty.jpg" width="230" alt="AI Copilot faculty answer"/> | <img src="docs/screenshots/import-export.jpg" width="230" alt="JSON import and export"/> |

<sub>More details on each image in <a href="docs/screenshots">docs/screenshots</a>.</sub>

</div>

## ✨ Features

<table>
<tr>
<td width="50%" valign="top">

### 📊 Attendance
- Dashboard with a percentage ring and streaks
- **One tap**: Attended · Missed · Cancelled
- Subjects screen filters: **All Classes**, **Lectures / Theory** and **Labs Only**
- A subject with both lectures and labs gets a separate card (and percentage) for each
- Cancelled classes and holidays never hurt your %

### 🗓️ Monthly & Backfill
- Interactive month calendar
- Backfill past days, plan future ones
- Log ad-hoc classes on any date
- Semester Subject Distribution with live bars

</td>
<td width="50%" valign="top">

### 🧮 Bunk & Recovery Calculator
- Safe bunks available right now
- Classes you must attend to hit your goal
- **What-If Simulator**: slide attend/skip and watch the result animate

### 📚 Academics
- Weekly timetable with teachers, rooms and labs
- Exam countdown and scheduler
- Study planner with goals and sessions
- **AI Copilot (Gemini)** that sees your attendance, timetable and faculty

</td>
</tr>
</table>

### 🎨 Built to be addictive

| | |
|---|---|
| 🫧 **Springy buttons** | Every press squishes and bounces back with a haptic tick |
| 🌊 **Cascading cards** | Cards fade and slide in one after another |
| 🔢 **Counting numbers** | Percentages roll up to their new value |
| 📶 **Living progress bars** | Bars grow with a bounce and change color at your target |
| 🎆 **Confetti** | Celebrate when you hit your goal |
| 🌈 **6 palettes** | Aurora Glow · Sunset Velvet · Ocean Deep · Cyber Neon · Emerald Forest · Mono Slate, light and dark |
| 🪟 **Glassmorphism** | Frosted cards over an interactive animated background |

### 🔔 Reminders & 💾 Data
- Class and end-of-day alarms, markable from the notification, restored after reboot
- Local-first Room database that works offline
- JSON export/import, cloud backup and restore, JSON timetable import

### 🚀 First launch & importing your timetable
- A fresh install opens the **onboarding / login page** first, where you enter your own details. No demo data is loaded.
- Import a timetable JSON (see [`Rajdip_Dhar_Schedule.json`](Rajdip_Dhar_Schedule.json) for the format) from the app's settings.
- The import brings in subjects, teachers, rooms and weekly slots only. **Attendance and exams start empty**, so you mark every class yourself and add your own exams.

## 🧠 How the math works

```text
percentage   = attended / (attended + missed) × 100      (cancelled is ignored)
safe bunks   = floor( (attended − t × total) / t )        t = target ÷ 100
must attend  = ceil( (t × total − attended) / (1 − t) )
```

```mermaid
flowchart LR
    A[Mark class] --> B[(Room DB)]
    B --> C[Repository merges duplicate subjects]
    C --> D[ViewModel StateFlow]
    D --> E[Animated UI]
    D --> F[Bunk and recovery math]
    D --> G[Gemini assistant]
```

## 📲 Install in 60 seconds

1. Grab [`AttendTrack.apk`](AttendTrack.apk) from the project root, or build the debug APK yourself (see below).
2. Copy it to your phone and open it.
3. Allow **Install unknown apps** if asked. Done. 🎉

> Updating from an older build? Uninstall it (or clear its data) first, so old demo data doesn't linger.

## 🛠️ Build from source

**Needs:** a full **JDK 25** (the project's Gradle daemon requires it, and it must include `jlink`; a bare JRE won't work) and the Android SDK. Android Studio is optional and its bundled JBR works.

Point Gradle at your SDK with a `local.properties` file in the project root (it is git-ignored), or set `ANDROID_HOME`:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

```bash
git clone https://github.com/RajdipDhar2007/AttendTrack.git
cd AttendTrack
```

<details>
<summary><b>Option A: Android Studio</b></summary>

Open the folder, let Gradle sync, press ▶ Run.

</details>

<details>
<summary><b>Option B: Command line (no Android Studio)</b></summary>

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"   # any full JDK 25
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"                # or use local.properties
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

The debug build signs with `debug.keystore` in the project root. Create one if missing:

```bash
keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
  -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

</details>

<details>
<summary><b>🔑 Gemini API key (optional, enables the AI Assistant)</b></summary>

Copy `.env.example` to `.env`:

```text
GEMINI_API_KEY=your_key_here
```

You can also paste a key inside the app's settings. Never commit `.env` or real keystores.

</details>

## 🗂️ Project structure

```text
.
├── AttendTrack.apk               Ready-to-install debug build
├── Rajdip_Dhar_Schedule.json     Sample timetable for the JSON import
├── docs/
│   ├── banner.svg                Animated README header
│   ├── footer.svg                Animated README footer
│   └── screenshots/              App screenshots used above (see its README for the file list)
└── app/src/main/java/com/example/
    ├── data/         Room entities, DAO, repository, Gemini and backup services
    ├── receiver/     Alarms, boot and notification action receivers
    └── ui/
        ├── components/   GlassCard, Interactions (animations), charts, dialogs
        ├── navigation/   Nav graph and routes
        ├── screens/      Dashboard, Monthly, Bunk, Timetable, Exams, Study, AI, Settings
        ├── theme/        Palettes and typography
        └── viewmodel/    AttendTrackViewModel
```

## 🗺️ Roadmap

- [ ] Home-screen widget
- [ ] Animations on every screen
- [ ] Attendance export to PDF
- [ ] Multi-semester history

## 🤝 Contributing

Fork → branch → commit → pull request. Issues and ideas are very welcome.

## 👤 Author

**Rajdip Dhar** · [@RajdipDhar2007](https://github.com/RajdipDhar2007)

<div align="center">

<a href="https://github.com/RajdipDhar2007/AttendTrack">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=18&pause=1200&color=F472B6&center=true&vCenter=true&width=700&lines=%E2%AD%90+One+click+keeps+this+project+alive;Bunk+smart.+Recover+fast.+Never+fall+below+target.;Thanks+for+stopping+by+%F0%9F%92%9C" alt="Footer typing animation"/>
</a>

<br/>

<img src="docs/footer.svg" alt="AttendTrack animated footer" width="100%"/>

</div>
