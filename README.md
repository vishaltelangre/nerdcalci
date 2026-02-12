<h1 align="center">NerdCalci</h1>

<p align="center">
  <img align="center" src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="NerdCalci Icon" width="192" height="192">
</p>

<p align="center">
  <strong>A nerd-style calculator for Android with variable support, syntax highlighting, and file-based sessions.</strong>
</p>

## Screenshots

![1](fastlane/metadata/android/en-US/images/phoneScreenshots/1.png)
![2](fastlane/metadata/android/en-US/images/phoneScreenshots/2.png)
![3](fastlane/metadata/android/en-US/images/phoneScreenshots/3.png)
![4](fastlane/metadata/android/en-US/images/phoneScreenshots/4.png)
![5](fastlane/metadata/android/en-US/images/phoneScreenshots/5.png)
![6](fastlane/metadata/android/en-US/images/phoneScreenshots/6.png)

## Features

### Smart Calculations
- **Variable Support**: Define variables and use them in calculations
  ```
  a = 100
  b = 200
  total = a + b  # 300
  ```
- **Percentage Calculations**: Natural percentage syntax
  ```
  20% of 50000    # 10000
  15% off 1000    # 850
  50000 + 10%     # 55000
  50000 - 5%      # 47500
  ```
- **Comments**: Add notes with `#` symbol
  ```
  price = 1000  # base price
  tax = 18% of price  # 180
  ```

### Editor Features
- **Syntax Highlighting**: Color-coded variables, numbers, operators, and comments
- **Auto-completion**: Smart variable suggestions as you type
- **Line Numbers**: Easy reference and navigation

### File Management
- **Multiple Files**: Create and manage separate calculation files
- **Auto-save**: Changes are saved automatically
- **Pin Files**: Keep important files at the top (max 10 pinned files)
- **Duplicate Files**: Create a copy of a file with a new name
- **Import/Export**: Share files in `.nerdcalci` format (ZIP)
- **Copy with Results**: Copy file content with calculated results to clipboard

### And More...
- **Undo/Redo**: Up to 30 steps per file
- **Dark/Light Theme**: System, dark, or light mode
- **Real-time Results**: See calculations update as you type

## Installation

### From GitHub Releases (Recommended)

1. Go to the [Releases](https://github.com/vishaltelangre/nerdcalci/releases/latest) page
2. Download the latest APK file
3. Transfer to your Android device
4. Enable "Install from Unknown Sources" if prompted
5. Open the APK and install
6. Start calculating!

### From F-Droid

_Submission in progress - will be available soon!_

[<img src="https://f-droid.org/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/com.vishaltelangre.nerdcalci/)

### From GitHub
1. Download the latest release from the [Releases](https://github.com/vishaltelangre/nerdcalci/releases) page
2. Install the APK file
3. Open the app and start using it

### From Source
1. Clone the repository:
   ```bash
   git clone https://github.com/vishaltelangre/nerdcalci.git
   cd nerdcalci
   ```

2. Open the project in Android Studio

3. Build and run on your device or emulator

## Built With

- [**Kotlin**](https://kotlinlang.org/) - Primary programming language
- [**Jetpack Compose**](https://developer.android.com/compose) - Modern UI toolkit
- [**Room Database**](https://developer.android.com/training/data-storage/room) - Local data persistence
- [**Material Design 3**](https://developer.android.com/jetpack/androidx/releases/compose-material3) - UI components and theming
- [**exp4j**](https://github.com/fasseg/exp4j) - Mathematical expression evaluation
- [**Fira Code**](https://github.com/tonsky/FiraCode) - Monospace font with ligatures

## Development

### Running Tests

Run all unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Run specific test class:
```bash
./gradlew :app:testDebugUnitTest --tests "com.vishaltelangre.nerdcalci.core.MathEngineTest"
```

After running tests, view the HTML report:

```bash
open app/build/reports/tests/testDebugUnitTest/index.html
```

### Generate App Icons

Automatically generate all required app icon sizes from a single source image:

```bash
./scripts/generate-icons.sh ~/Downloads/app-icon.png
```

This creates:
- Android mipmap icons (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
- Fastlane metadata icon
- Both regular and round launcher icons

**Requirements:**
- Source image: PNG, minimum 512x512 (recommended: 2048x2048)
- Square aspect ratio (1:1)
- macOS: `sips` (built-in) or ImageMagick

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
