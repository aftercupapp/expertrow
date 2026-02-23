# Expert Row

A minimal, offline calculator application designed specifically for the Minimal Phone (MP01) and other Android e-paper devices.

## Description
Expert Row is built to eliminate ghosting and latency issues common on e-ink screens. It features a high-contrast black-and-white interface, zero animations, and a large typography-focused layout. It functions as both a standard scientific calculator and an algebraic equation solver.

## Gallery
<p align="center">
  <img src="https://i.postimg.cc/PJybSjvb/Screenshot_20260223_173346.png" width="45%" />
  <img src="https://i.postimg.cc/PJFNYxT6/Screenshot_20260214_181923.png" width="45%" />
</p>
<p align="center">
  <img src="https://i.postimg.cc/7hQb06wK/Screenshot_20260214_181932.png" width="45%" />
  <img src="https://i.postimg.cc/D0YmLZn6/Screenshot_20260214_182542.png" width="45%" />
</p>

## Features
* **MP01 optimized:** High contrast, no animations, and full-screen UI.
* **Algebra solver:** Automatically detects variables (e.g., `2x + 5 = 15`) and solves for x using the Newton-Raphson method.
* **Scientific functions:** Support for trigonometry (sin, cos, tan, ctg), logarithms (log, ln, logn), and powers.
* **Implicit multiplication:** Understands syntax like `2x` or `5(x+1)` without needing explicit multiplication signs.
* **History:** Saves calculations locally to review later.
* **Theme support:** Switch between light and dark modes in settings.

## Privacy
This application is completely offline.
* **No data collection:** Expert Row does not collect, store, or transmit any personal user data.
* **Local storage:** Calculation history is stored locally on your device and can be cleared at any time via the settings.
* **No internet permissions:** The app does not request internet access in its manifest.

## Installation
1. Go to the [Releases](https://github.com/aftercupapp/expertrow/releases) page.
2. Download the latest `.apk` file.
3. Install the file on your device.

## Usage
**Standard calculation**
Type any mathematical expression and press Enter.
* Input: `5 + 5`
* Result: `= 10`

**Solving for x**
Type an equation containing `x` and an `=` sign.
* Input: `2x + 10 = 50`
* Result: `x = 20`

## Built with
* Kotlin
* exp4j (math parsing engine)

## License
This project is open source.
