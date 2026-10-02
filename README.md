# Inferra — Open-Weight AI Model Intelligence Platform

[![Android Build](https://img.shields.io/badge/Android-API%2031%2B-00F2FE?style=for-the-badge&logo=android)](https://developer.android.com)[cite: 1]
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-8A2BE2?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)[cite: 1]
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-00F5A0?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)[cite: 1]
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](LICENSE)
[![Issues](https://img.shields.io/github/issues/tirthsaraiya29/Inferra?style=for-the-badge&color=orange)](https://github.com/tirthsaraiya29/Inferra/issues)
[![Share on X](https://img.shields.io/badge/Share-Post%20on%20X-black?style=for-the-badge&logo=x)](https://twitter.com/intent/tweet?text=Inspect%20open-weight%20LLM%20runability,%20GGUF%20quants,%20and%20hardware%20fit%20on%20Android%20with%20Inferra:&url=https://github.com/tirthsaraiya29/Inferra&hashtags=LocalLLM,OpenSource,AndroidDev,MachineLearning)

Inferra is an open-source Android intelligence and decision engine designed for AI engineers, researchers, and local-model practitioners who need to determine which open-weight model will actually run on their target hardware before committing storage or compute[cite: 1].

---

## 📱 Demo & Interface Preview


```

┌────────────────────────────────────────────────────────────────────────┐
│                              [ PREVIEW ]                               │
│                                                                        │
│   Place a 1080x1920 screenshot or animated GIF (e.g., docs/demo.gif)  │
│    demonstrating the Hardware Fit Calculator and Lineage Graph here.   │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘

```
> *Add your asset to `docs/demo.gif` or `docs/screenshot.png` and update the link above.*

---

## 🌟 Key Features

* **⚡ Live Runability & Hardware Fit Calculator**: Real-time VRAM/RAM estimation, GPU layer offloading breakdown, estimated inference tokens/sec, and Time-To-First-Token (TTFT) prediction against hardware profiles (e.g., RTX 4090, Apple Silicon, local Android chipsets)[cite: 1].
* **📦 Quantization Explorer**: Deep exploration of GGUF (Q4_K_M, Q8_0, Q5_K_M, Q3_K_M), AWQ, and EXL2 formats with explicit file sizes, perplexity/quality trade-offs, and memory bounds[cite: 1].
* **🌳 Interactive Model Lineage Tree**: Visual hierarchy tracing Base Models → Instruction/Alignment Fine-tunes → Quantizations → LoRA Adapters[cite: 1].
* **⚖ Side-by-Side Model Comparison**: Compare up to 4 models simultaneously across parameters, active MoE parameters, context lengths, licenses, and verified benchmarks[cite: 1].
* **📡 Send-To-PC Download Dispatch**: Queue durable, resumable model downloads to desktop companion nodes without storing multi-gigabyte weights on mobile devices[cite: 1].
* **🧪 Provenance-Aware Benchmarks**: Explicit isolation between upstream metadata (author claims), objective benchmarks (HumanEval, MMLU-Pro, GSM8K, GPQA), and derived on-device calculations[cite: 1].

---

## 💻 Requirements

* **Build Environment**: Android Studio (Ladybug 2024.2+ / Jellyfish / Koala)[cite: 1]
* **Java Development Kit**: JDK 17 or JDK 21[cite: 1]
* **Android Target**: `compileSdk = 37`, `minSdk = 31` (Android 12+)[cite: 1]
* **Companion Node (Optional)**: Desktop workstation running the Inferra daemon to accept remote download dispatch[cite: 1].

---

## 🛠 Installation & Quick Start

### 1. Clone & Build
```bash
# Clone the repository
git clone [https://github.com/tirthsaraiya29/Inferra.git](https://github.com/tirthsaraiya29/Inferra.git)
cd Inferra

# Build the debug APK
./gradlew app:assembleDebug

```

**Expected output:**

```text
BUILD SUCCESSFUL in 18s
38 actionable tasks: 38 executed
Output: app/build/outputs/apk/debug/app-debug.apk

```

### 2. Install to Connected Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk

```

### 3. Run Unit Tests

```bash
./gradlew testDebugUnitTest

```

---

## 🎯 Practical Walkthrough: Evaluating Hardware Fit

1. **Configure Hardware**: Navigate to **Hardware** and select or create a profile (e.g., *Desktop: 16 GB VRAM RTX 4080* or *Laptop: 8 GB VRAM RTX 3050*).
2. **Search Model**: Query `Qwen/Qwen2.5-Coder-7B-Instruct` or `Meta-Llama-3.1-8B-Instruct`.
3. **Inspect Runability Breakdown**:
* **FP16 / BF16**: Exceeds 8 GB VRAM; triggers partial CPU/RAM offloading warning.
* **GGUF Q4_K_M**: Fits 100% within VRAM with a 16k context window buffer; displays projected throughput (~38 t/s).


4. **Dispatch Weights**: Tap **Send to Node** to queue the GGUF download directly onto your host workstation via the local pairing port.

---

## 🏗 Architecture

Inferra applies modern Android clean architecture patterns (`com.inferra`):

```text
app
└── com.inferra
    ├── data
    │   ├── local         // Room Database (ModelEntity, DownloadJobEntity, HardwareProfileEntity)[cite: 1]
    │   ├── network       // HuggingFace Hub API client & DTO mappers[cite: 1]
    │   └── repository    // ModelRepository, HardwareRepository, DownloadRepository[cite: 1]
    ├── domain
    │   ├── model         // AiModel, QuantizationInfo, HardwareProfile, BenchmarkScore[cite: 1]
    │   └── usecase       // HardwareFitCalculator engine[cite: 1]
    └── ui
        ├── components    // GlassCard, GlassButton, CapabilityRadar, LineageGraphView[cite: 1]
        ├── navigation    // Compose Navigation Host & GlassBottomBar[cite: 1]
        ├── screens       // Discovery, Search, Detail, Compare, Hardware, Downloads[cite: 1]
        └── theme         // Cyber-Obsidian palette[cite: 1]

```

---

## ⚠️ Current Limitations

* **No Direct Mobile Inference**: Inferra evaluates fit, visualizes lineage, and dispatches download jobs; it does not execute multi-gigabyte LLM inference natively on the Android phone chip.
* **Hugging Face Rate Limits**: Unauthenticated API calls are subject to standard Hugging Face Hub public rate limits. Add an API token under `Settings` for heavy indexing.
* **Desktop Node Dependency**: The download dispatch feature requires the desktop pairing daemon to be reachable on your local network.

---

## 🤝 Contributing & Community

Contributions are welcomed. Areas where help is immediately needed:

* **Good First Issues**: UI edge cases in landscape mode, additional hardware profile presets (Apple M-series, Intel Arc GPUs).
* **Core Calculator**: Enhancing KV-cache scaling formulas for extended context lengths (128k+ tokens).

To contribute:

1. Check open issues tagged [`good first issue`](https://www.google.com/search?q=https://github.com/tirthsaraiya29/Inferra/issues%3Fq%3Dis%253Aissue%2Bis%253Aopen%2Blabel%253A%2522good%2Bfirst%2Bissue%2522) or [`help wanted`](https://www.google.com/search?q=https://github.com/tirthsaraiya29/Inferra/issues%3Fq%3Dis%253Aissue%2Bis%253Aopen%2Blabel%253A%2522help%2Bwanted%2522).
2. Open an issue before submitting major architectural changes.
3. Submit a pull request targeting the `main` branch with passing unit tests (`./gradlew testDebugUnitTest`).

---

## 📄 License

Inferra is released under the [Apache License 2.0](https://www.google.com/search?q=LICENSE).