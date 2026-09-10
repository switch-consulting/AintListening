# Ain't Listening

Offline-first Android app to transcribe shared WhatsApp voice messages (`.opus`) locally on-device.

## Stack
- Java
- Android SDK 37 / minSdk 26
- Native MediaCodec for Opus decoding
- Vosk Android (`com.alphacephei:vosk-android:0.3.75`) for offline speech recognition
- Microsoft ONNX Runtime (`com.microsoft.onnxruntime:onnxruntime-android`) for model inference
- Deep Java Library (DJL) & Hugging Face Tokenizers for text processing
- Material Design 3 UI components

## Build & Run
1. Open project in a recent version of Android Studio.
2. Let Gradle sync.
3. Build and run on device/emulator.

## Vosk Model Setup
The app requires the German speech model for offline transcription.

- **Automatic**: Upon first launch (or if the model is missing), the app shows a status indicator on the main screen. Simply click the **Download** button to automatically fetch and extract the model (~45MB) from alphacephei.com.
- **Manual**: Alternatively, you can download `vosk-model-small-de-0.15.zip` from [alphacephei.com](https://alphacephei.com/vosk/models), unzip it, and place the folder at:
  `/data/data/de.switchconsulting.aintlistening/files/vosk-model-small-de-0.15`

## Usage
1. In WhatsApp, share a voice message via **Share**.
2. Select **Ain't Listening**.
3. App converts audio to 16kHz mono WAV and transcribes locally.
4. Copy transcript from selectable text view.

## Testing & Sideloading (Beta)
To test the app on your Android device without building from source:

1. **Download the APK**: Go to the [Releases](https://github.com/switchconsulting/AintListening/releases) section and download the latest `AintListening-debug.apk`.
2. **Transfer (if needed)**: Move the file to your Android device.
3. **Enable Unknown Sources**: 
   - Open the APK on the phone. 
   - If prompted, go to **Settings** and enable "Allow from this source" for the app you are using to open the file (e.g., your browser or File Manager).
4. **Install**: Follow the prompts to complete the installation.
5. **Note**: Since this is a Debug build, Android might show a warning about an "Unsafe App" or "Play Protect". Choose "Install anyway" to proceed.

## License Notes
- **App Source**: Apache License 2.0
- **Vosk**: Apache License 2.0. Models (e.g., German small) may have separate licenses—verify before commercial distribution.
- **ONNX Runtime**: MIT License
- **DJL / Hugging Face Tokenizers**: Apache License 2.0
- **Vosk Model**: Verify individual model license at [alphacephei.com](https://alphacephei.com/vosk/models) (usually Alpaca/Apache/Creative Commons).
