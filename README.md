# Ain't Listening

Offline-first Android app to transcribe shared WhatsApp voice messages (`.opus`) locally on-device.

## Stack
- Java
- Android SDK 34 / minSdk 26
- Native MediaCodec for Opus decoding
- Vosk Android (`com.alphacephei:vosk-android:0.3.47`)

## Build & Run
1. Open project in a recent version of Android Studio (supporting AGP 9.3+).
2. Let Gradle sync.
3. Build and run on device/emulator.

## Vosk Model Setup (required)
1. Download `vosk-model-small-de-0.15` from Vosk models.
2. Unzip it.
3. Copy the full folder to app-internal files dir path:
   - `/data/data/de.switch.aintlistening/files/vosk-model-small-de-0.15`

> Current draft expects model folder to already exist in internal storage.
> Next iteration can auto-copy from `assets` on first launch.

## Usage
1. In WhatsApp, share a voice message via **Share**.
2. Select **Ain't Listening**.
3. App converts audio to 16kHz mono WAV and transcribes locally.
4. Copy transcript from selectable text view.

## License Notes
- App source: Apache License 2.0
- Vosk model/data may have separate model licenses—verify before distribution
