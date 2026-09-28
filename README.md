# Inferra — Open-Weight AI Model Intelligence Platform

[![Android Build](https://img.shields.io/badge/Android-API%2031%2B-00F2FE?style=for-the-badge&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-8A2BE2?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-00F5A0?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

**Inferra** is a premium Android application designed for discovering, researching, comparing, tracking, and remotely managing open-weight AI models.

Rather than acting as a generic mobile API wrapper, Inferra serves as a **model discovery and decision engine** that answers the fundamental developer question:

> *"Which AI model should I actually use for my hardware and use case?"*

---

## 🌟 Key Features

* **🔍 Intelligence Discovery Engine**: Curated feeds tailored to hardware profiles ("For You"), rapidly gaining models ("Trending"), fresh releases ("New Drops"), and high-performance foundational workhorses.
* **⚡ Live Runability & Hardware Fit Calculator**: Real-time VRAM/RAM estimation, GPU layer offloading breakdown, estimated inference tokens/sec, and Time-To-First-Token (TTFT) prediction based on active hardware profiles (e.g., RTX 4090, Mac Studio M3 Max, Android Local).
* **📦 Quantization Explorer**: Deep exploration of GGUF (Q4_K_M, Q8_0, Q5_K_M, Q3_K_M), AWQ, and EXL2 quants with exact file sizes, quality tradeoffs, and memory requirements.
* **🌳 Interactive Model Lineage Tree**: Visual family tree navigation connecting Base Models → Instruction/Alignment Fine-tunes → GGUF Quantizations → Adapters.
* **⚖ Side-by-Side Model Comparison**: Compare up to 4 models simultaneously across parameters, active MoE parameters, context lengths, licenses, hardware fit, and benchmarks.
* **📡 Send-To-PC Download Dispatch**: Queue durable, resumable download jobs on paired desktop companion nodes (`Tirth-PC`, `Workstation-01`) without downloading multi-gigabyte models directly to your phone.
* **🧪 Provenance-Aware Benchmarks**: Clear distinction between **Source Facts** (author metadata), **Measurements** (HumanEval, MMLU-Pro, GSM8K, GPQA), and **Derived Intelligence** (app calculations).
* **🎨 True Liquid Glass UI System**: Dark Cyber-Obsidian visual language featuring real per-pixel background blur, specular edge highlights, gradient glass surfaces, and spring physics.

---

## 🏗 Architecture

Inferra is built following modern Android clean architecture principles (`com.inferra`):

```
app
└── com.inferra
    ├── data
    │   ├── local         // Room Database (ModelEntity, DownloadJobEntity, HardwareProfileEntity)
    │   ├── network       // HuggingFace Hub API client & DTO mappers
    │   └── repository    // ModelRepository, HardwareRepository, DownloadRepository, CompanionRepository
    ├── domain
    │   ├── model         // AiModel, QuantizationInfo, HardwareProfile, BenchmarkScore
    │   └── usecase       // HardwareFitCalculator engine
    └── ui
        ├── components    // GlassCard, GlassButton, GlassTextField, ModelCard, CapabilityRadar, LineageGraphView
        ├── navigation    // Navigation Compose Host & GlassBottomBar
        ├── screens       // Discovery, Search, Detail, Compare, Hardware, Downloads, Watchlist, Settings
        └── theme         // Cyber-Obsidian color palette & Typography
```

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio**: Ladybug / 2024.2+ or Android Studio Jellyfish/Koala
* **JDK**: Java 17 / 21
* **Android SDK**: `compileSdk = 37`, `minSdk = 31`

### Build Instructions

```bash
# Clone repository
git clone https://github.com/Tirth-PC/Inferra.git
cd Inferra

# Build Debug APK
./gradlew app:assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest
```

---

## 🔒 Privacy & Data Provenance

Inferra respects user privacy and data transparency:
* No personal data or model prompts are collected or transmitted.
* Hardware profile data is stored exclusively on-device.
* Community benchmarking participation is strictly opt-in.

---

## 📄 License

Distributed under the Apache 2.0 License. See `LICENSE` for more information.
