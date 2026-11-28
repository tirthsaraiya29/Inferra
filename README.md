# Inferra — Native Hugging Face Hub Client for Android

<p align="center">
  <strong>Discover, explore, search, inspect, bookmark, and download Hugging Face repositories natively on Android.</strong>
</p>

<p align="center">
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-API%2031%2B-00F2FE?style=for-the-badge&logo=android" alt="Android Version" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.1.10-8A2BE2?style=for-the-badge&logo=kotlin" alt="Kotlin Version" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Compose-Material%203-00F5A0?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge" alt="Apache 2.0 License" /></a>
</p>

<p align="center">
  <a href="#-key-features">Key Features</a> •
  <a href="#-architecture--design">Architecture</a> •
  <a href="#-security">Security</a> •
  <a href="#-quick-start--build">Quick Start</a> •
  <a href="#-testing">Testing</a>
</p>

---

## 🌟 Key Features

* **🤗 Full Hugging Face Hub Discovery**: Explore trending models, popular datasets, trending Spaces, and new releases directly from official Hugging Face APIs.
* **🔍 Global Multi-Type Search**: Search models, datasets, and Spaces by repo ID, name, author, pipeline tag, library, or license with debouncing, sorting, and pagination.
* **📄 Rendered Markdown READMEs**: View repository README cards cleanly rendered with Markdown typography, headings, code blocks with copy action, tables, quotes, and images.
* **🌳 Repository File Tree Browser**: Browse nested directory trees, file sizes, and revisions. Inspect raw text file previews or download artifacts directly.
* **🔐 Secure Access Tokens**: Authenticate using your Hugging Face access token (`hf_...`) backed by Android Keystore hardware encryption. Access gated or private repositories seamlessly.
* **👤 Users & Organizations**: View user or organization profile pages, follower statistics, and public repository showcases.
* **🔖 Local Bookmarks**: Save favorite models, datasets, and Spaces locally with Room persistence.
* **⚡ Dependable Background Downloads**: Download model weights or dataset files with WorkManager, background progress reporting, SHA256 integrity checks, and storage availability checks.

---

## 🏗 Architecture & System Design

Inferra follows Modern Android Architecture principles with Jetpack Compose, Material 3, Kotlin Coroutines, Retrofit, Room, and WorkManager.

```
app/src/main/java/com/inferra/
├── core/
│   └── security/        # Android Keystore encrypted token storage
├── data/
│   ├── download/        # WorkManager & OkHttp streaming file downloader
│   ├── local/           # Room Database (Bookmarks, DownloadJobs, LocalFiles)
│   ├── network/         # Hugging Face Retrofit API, DTOs & Mappers
│   └── repository/      # HubRepository, DownloadRepository & SettingsRepository
├── domain/
│   └── model/           # Strongly typed domain models (RepoItem, RepoDetail, FileTreeItem, AuthState)
└── ui/
    ├── components/      # Material 3 UI cards, MarkdownViewer, file tree rows
    ├── navigation/      # NavHost & floating liquid bottom bar
    ├── screens/         # Discovery, Search, RepoList, RepoDetail, Downloads, Account, Bookmarks
    └── theme/           # Material 3 color palettes & typography
```

---

## 🔒 Security

* **Android Keystore Encryption**: Hugging Face access tokens are encrypted with AES-256-GCM via the Android Keystore system. Tokens are never stored in plaintext.
* **No Credential Exposure**: Authorization headers are injected securely only for `huggingface.co` host requests.
* **Network Security Configuration**: TLS 1.3 enforced with strict certificate validation.

---

## 🛠 Quick Start & Build

### Prerequisites
- Android Studio Ladybug or higher
- JDK 17 configured as Gradle JDK

```bash
# 1. Clone the repository
git clone https://github.com/tirthsaraiya29/Inferra.git
cd Inferra

# 2. Build Debug APK
./gradlew app:assembleDebug

# 3. Install to connected Android device or emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 Testing

```bash
# Run Unit Tests
./gradlew testDebugUnitTest

# Build Release APK
./gradlew app:assembleRelease
```

---

## 📄 License

Inferra is distributed under the [Apache License 2.0](LICENSE).
