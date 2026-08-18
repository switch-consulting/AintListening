# Ain't Listening

Offline-first Android app to transcribe shared WhatsApp voice messages (`.opus`) locally on-device.

## Stack
- Java
- Android SDK 34 / minSdk 26
- Native MediaCodec for Opus decoding
- Vosk Android (`com.alphacephei:vosk-android:0.3.75`)

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

## License Notes
- App source: Apache License 2.0
- Vosk model/data may have separate model licenses—verify before distribution
