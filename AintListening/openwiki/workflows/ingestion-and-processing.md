---
type: workflow
title: Audio Ingestion & Processing Workflow
description: Document the end-to-end data flow of receiving, decoding, transcribing, formatting, and saving audio recordings from external apps entirely offline on device.
tags: [workflow, ingestion, audio-decoding, transcription, smart-formatting, multi-threading, offline-first]
sources:
  - id: openwiki-source-1667fbe5aa2896d93d29ef64
    resource: repo://src/main/java/de/switchconsulting/aintlistening/data/ModelDownloader.java
  - id: openwiki-source-ed6431c58e2bd530856218bc
    resource: repo://src/main/java/de/switchconsulting/aintlistening/data/Persistency.java
  - id: openwiki-source-85dfa2aef49cd169abedb8cf
    resource: repo://src/main/java/de/switchconsulting/aintlistening/data/TranscriptionProcessor.java
  - id: openwiki-source-9482b0ba8950176e384891bc
    resource: repo://src/main/java/de/switchconsulting/aintlistening/transcription/VoskTranscriber.java
  - id: openwiki-source-d559f223847d6101065cafd8
    resource: repo://src/main/java/de/switchconsulting/aintlistening/transcription/WhisperTranscriber.java
  - id: openwiki-source-81be2e8f2fae66e6fb3cddb4
    resource: repo://src/main/java/de/switchconsulting/aintlistening/ui/MainActivity.java
  - id: openwiki-source-1982667c217d6a26d7c1a605
    resource: repo://src/main/java/de/switchconsulting/aintlistening/ui/MainViewModel.java
  - id: openwiki-source-4fa4082a9c1fe41de7b15848
    resource: repo://src/main/java/de/switchconsulting/aintlistening/util/OpusToWavDecoder.java
verified:
  - by: openwiki/0.5.2
    at: 2026-09-23T10:59:03.026Z
generated: { by: "openwiki/0.5.2", at: "2026-09-23T10:59:03.026Z" }
---

# Audio Ingestion & Processing Workflow

## Introduction

AintListening features a completely offline, secure, and privacy-respecting pipeline to ingest, decode, transcribe, format, and persist audio recordings. This end-to-end workflow processes incoming voice notes or audio files shared from other applications (e.g., WhatsApp, Signal, or voice recorder apps) via Android's native share mechanisms, transforms them into a uniform audio format, transcribes them natively using deep learning, post-processes the results to insert punctuation/capitalization, and saves the final output to persistent storage—all without sending a single byte of data to external servers.

---

## 1. Share Ingestion & Intent Resolution

The workflow begins when the user shares an audio file from an external application. `MainActivity` serves as the entry point, filtering incoming `Intent` objects to catch shared media.

### Intent Interception
* **Lifecycle Hooks**: `MainActivity` overrides the activity lifecycle hooks `onCreate` and `onNewIntent` to intercept incoming intents.
* **MIME-Type Filtering**: It targets intents with action `Intent.ACTION_SEND` and a MIME type starting with `audio/` (e.g., `audio/ogg`, `audio/aac`, `audio/wav`). If the incoming intent does not match these criteria, it delegates to `MainViewModel.loadLastMessage()` to display the last saved session instead of starting a new transcription.
* **URI Extraction**: For valid intents, it retrieves the audio stream URI from the intent's `EXTRA_STREAM` parameter using `IntentCompat.getParcelableExtra`.

### Model Selection and Guarding
Before initiating the transcription process, `MainActivity` queries `ModelManager` to resolve which language models are downloaded and currently enabled by the user.
1. **Resolution Criteria**: It loops through `ModelManager.SUPPORTED_LANGUAGES` and checks whether each language is:
   - Enabled via user settings in `Persistency` (`persistency.isLanguageEnabled(lang.getLocale())`).
   - Downloaded to local device storage (`lang.hasTranscriptionModelDownloaded(context)`).
2. **Execution Paths**:
   - **Zero Models Available**: If no language models meet the criteria, the activity warns the user with a Toast message (`R.string.status_no_models_installed`) and aborts.
   - **Exactly One Model Available**: If only a single model is resolved, it skips prompting and immediately delegates the transcription request to the ViewModel via `viewModel.startTranscription(audioUri, availableLanguages.get(0).getLocale())`.
   - **Multiple Models Available**: If multiple models are enabled and downloaded, it builds and displays a `MaterialAlertDialogBuilder` list dialog showing the display names of the available languages.
3. **ViewModel Delegation**: Once the target `Locale` is resolved (automatically or via dialog selection), `MainActivity` invokes `viewModel.startTranscription(audioUri, selectedLocale)`.

---

## 2. The Decoding Pipeline (`OpusToWavDecoder`)

Since the shared audio file may be encoded in various compression formats (most notably Opus/OGG utilized by modern messengers), and local transcription engines like Vosk require raw, uncompressed audio with a uniform structure, AintListening passes the audio URI through a custom `OpusToWavDecoder`. 

This decoder uses Android's native multimedia API (`MediaExtractor` and `MediaCodec`) to decode the source file and resample it into a standard **16kHz, Mono, 16-bit PCM WAV** file.

### Step-by-Step Decoder Execution

1. **Audio Track Selection**: The decoder instantiates a `MediaExtractor` and binds it to the shared audio URI. It loops through the file's tracks to find the first track with a MIME type starting with `audio/`.
2. **Decoder Initialization**: It retrieves the `MediaFormat` for the audio track, extracts the track MIME type, and instantiates the appropriate hardware or software audio decoder via `MediaCodec.createDecoderByType(mime)`. It configures and starts the decoder.
3. **WAV File Setup & Header Initialization**: The decoder opens a `FileOutputStream` pointing to the target temporary WAV file and writes a placeholder 44-byte RIFF-WAV header containing zeroed-out lengths.
4. **The Decoding Loop**:
   - **Input Buffer Queueing**: The decoder dequeues available input buffers from `MediaCodec`. It reads raw encoded audio chunks from `MediaExtractor` and queues them into the decoder's input buffers using `queueInputBuffer`. If the end of the extractor stream is reached, the `BUFFER_FLAG_END_OF_STREAM` is set.
   - **Output Buffer Dequeueing**: It dequeues output buffers containing decoded, raw PCM bytes. For each output buffer, it reads the raw bytes and processes them through the resampling engine.
5. **Resampling & Downmixing (`processPcmTo16kMono`)**:
   - The raw byte buffer is parsed into a `short` array (little-endian, 16-bit PCM).
   - **Down-Sampling**: The decoder calculates the down-sampling step based on the ratio of the native sample rate to the target sample rate (16000Hz):
     $$\text{step} = \frac{\text{nativeSampleRate}}{\text{TARGET\_SAMPLE\_RATE}}$$
   - **Channel Downmixing**: If the input has multiple channels (e.g., stereo), the decoder averages the sample amplitudes across all channels to downmix the audio into a single mono channel:
     $$\text{sample}_{\text{mono}} = \frac{1}{\text{channels}} \sum_{c=0}^{\text{channels}-1} \text{sample}_{i+c}$$
   - **Re-serialization**: The resampled and downmixed `short` array is packed back into little-endian bytes and written directly to the output stream.
6. **WAV Header Update**: Once the input and output streams are completely processed (EOS), the decoder closes the output stream. It opens the WAV file using a `RandomAccessFile` and overwrites the initial 44-byte header with the actual PCM length and calculated subchunk sizes.

---

## 3. The Orchestration & Multi-Threading Model

To keep the application highly responsive, fluid, and free of Application Not Responding (ANR) errors, AintListening enforces a strict multi-threading model using structured thread boundaries and single-thread background executors.

```
┌─────────────────────────────────────────────────────────────────┐
│                       MAIN UI THREAD                            │
│  - MainActivity handles incoming Intent and UI events.         │
│  - Observes LiveData updates from MainViewModel.                │
│  - Updates progress indicator and transcription RecyclerView.   │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼ (Dispatches Task)
┌─────────────────────────────────────────────────────────────────┐
│                       MAIN VIEWMODEL                            │
│  - Relays transcription requests to TranscriptionProcessor.     │
│  - Observes and translates background callbacks into LiveData.  │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼ (Offloads to Executor)
┌─────────────────────────────────────────────────────────────────┐
│                 TRANSCRIPTION PROCESSOR EXECUTOR                │
│             (Single-Thread Background Executor)                 │
│  - Clear temporary cache and segment files sequentially.        │
│  - Invokes OpusToWavDecoder to convert audio files.             │
│  - Decodes and stream-transcribes WAV (Vosk / Whisper).         │
│  - Performs ONNX Smart Formatting sequentially.                 │
│  - Persists completed paragraphs to SharedPreferences.           │
└─────────────────────────────────────────────────────────────────┘
```
*Figure 1: High-level architectural threading and dispatching model.*

### Thread Responsibilities and Boundaries

* **Main UI Thread**: Responsible for handling user interactions, presenting dialogs, rendering the `RecyclerView`, and animating the progress indicators. All UI-related changes are driven by observing `uiState` (a LiveData stream) inside the `MainActivity`.
* **ViewModel Dispatching and State Translation**: `MainViewModel` serves as the reactive coordinator. It triggers transcription by calling `TranscriptionProcessor.startTranscription` and registers an anonymous `TranscriptionCallback` listener. The processor executes off the main thread, and `MainViewModel` translates the asynchronous callback events into distinct `MainUiState` values using `MutableLiveData.postValue()` to safely bridge thread boundaries:
  - **Status Updates (`onStatusUpdate(String message)`)**: Instantiates `MainUiState.loading(message, currentParagraphs)`, preserving already transcribed text, and posts it to notify the user of the ongoing phase (e.g., "Converting audio...", "Loading model...").
  - **Partial/Incremental Text Results (`onPartialResult(List<TranscriptionParagraph> paragraphs)`)**: Instantiates `MainUiState.loading(statusMessage, paragraphs)`, retaining the active status message while updating the list of transcription paragraphs as speech is recognized in chunks.
  - **Smart Formatting Progress (`onSmartFormattingProgress(int progress, int total, List<TranscriptionParagraph> paragraphs)`)**: Instantiates `MainUiState.progress("Applying smart formatting...", progress, total, paragraphs)` to update the horizontal progress indicator as each paragraph is processed.
  - **Completion (`onComplete(List<TranscriptionParagraph> paragraphs)`)**: Instantiates `MainUiState.idle(paragraphs)` to update the UI with the final result and deactivate loading/progress overlays.
  - **Error Handling (`onError(String message)`)**: Instantiates `MainUiState.error(message, paragraphs)` to display error dialogs or messages without discarding successfully transcribed text.
* **`TranscriptionProcessor` Single-Thread Executor**: Instantiated as a dedicated instance-level `Executors.newSingleThreadExecutor()`, this executor sequentially schedules and executes transcription workflows. Runs operations sequentially to prevent thread contention, avoid memory exhaustion from loading large deep-learning models simultaneously, and guarantee thread safety.
* **`ModelDownloader` Background Executor**: A separate static single-thread background executor is used inside `ModelDownloader` to handle HTTP download connections and extract model zip files. Keeping model downloading on a distinct thread ensures that a downloading task does not block or queue up behind a concurrent transcription task, allowing users to manage models while transcribing.

### TranscriptionProcessor Orchestration Flow

When `TranscriptionProcessor.startTranscription()` is called, it schedules an asynchronous task on its single-thread executor to perform the end-to-end processing pipeline:

```mermaid
flowchart TD
    start([Start Transcription Task]) --> clear["Clear Temporary WAV & Audio Chunks"]
    clear --> decode["Decode Audio via OpusToWavDecoder"]
    decode --> check_decode{"Decode Successful?"}
    
    check_decode -- "No" --> notify_fail["Notify Callback with Error"]
    check_decode -- "Yes" --> resolve_model["Resolve Transcriber & Load Language Model"]
    
    resolve_model --> run_transcribe["Execute Speech-To-Text Transcription"]
    run_transcribe --> check_smart{"Smart Formatting Enabled & Needed?"}
    
    check_smart -- "Yes" --> load_onnx["Load ONNX Smart Formatter & Format Paragraphs"]
    check_smart -- "No" --> save_state["Serialize & Save Session via Persistency"]
    
    load_onnx --> save_state
    save_state --> notify_success([Notify Callback with Completed Result])

    style start fill:#f9f,stroke:#333,stroke-width:2px
    style notify_success fill:#bbf,stroke:#333,stroke-width:2px
    style notify_fail fill:#ff9,stroke:#333,stroke-width:2px
```
*Figure 2: Sequential control flow of the TranscriptionProcessor.startTranscription() execution.*

1. **Clean Environment**: Wipes any left-over audio artifacts from previous sessions by calling `repository.clearTemporaryFiles()`.
2. **Audio Decoding**: Converts the incoming stream URI to a standardized WAV file using `OpusToWavDecoder.decodeOpusToWav()`.
3. **Background Model Loading**: Resolves the appropriate transcriber engine (Vosk or Whisper) and triggers `activeTranscriber.ensureModelLoaded(context, locale)`. This loads the model weights into memory directly on the background processor thread, avoiding any UI stutter.
4. **Speech-To-Text**: Feeds the WAV file to the active transcriber, receiving incremental partial updates and generating paragraph-level audio chunk segment files via a listener callback.
5. **Smart Formatting**: Post-processes raw paragraphs sequentially through the `OnnxSmartFormatter` model (if enabled and applicable), updating the caller on formatting steps.
6. **Persistence**: Invokes `repository.saveLastMessage()` to serialize the results for later retrieval before notifying the ViewModel of complete success.

---

## 4. Temporary File Clearing, Audio Chunk Splitting, and Persistence

AintListening utilizes local file boundaries and segmented cache files to enable paragraph-level playback and ensure efficient storage utilization.

### Temporary File Cleansing

Before any new audio file is processed, `TranscriptionProcessor` ensures a pristine environment by invoking `persistency.clearTemporaryFiles()`. This operation:
1. Deletes the primary decoded audio WAV file (`incoming_audio_16k_mono.wav`) located in the cache directory.
2. Deletes all previously split audio chunk files (`chunk_*.wav`) stored in the `audio_chunks/` internal directory.

This prevents stale files from polluting device storage and ensures that paragraph playback buttons always refer to the current transcription's audio segments.

### Audio Chunk Splitting

Both local transcription engines segment the incoming audio file and map each transcription paragraph to its exact corresponding raw audio chunk. However, they achieve this through different technical mechanisms:

#### A. Vosk-Based Chunk Splitting (On-The-Fly)
Because the `VoskTranscriber` does not natively output word-level or sentence-level timestamps, it splits audio dynamically during the recognition loop:
* The WAV file is read sequentially in 4096-byte buffers, which are fed into Vosk's `Recognizer`.
* The raw PCM bytes are continuously written to an active `ByteArrayOutputStream`.
* When `recognizer.acceptWaveForm(buffer, nread)` returns `true` (signaling that a natural silence or segment boundary has been finalized), the transcriber triggers `listener.onAudioChunkAvailable(pcmData, chunkIndex++)`.
* This flushes the accumulated bytes in the stream to a new WAV chunk file (`chunk_{index}.wav`) via `persistency.saveAudioChunk()`, returns its file path, and resets the stream.
* The transcriber then maps the newly finalized `TranscriptionParagraph` to this specific audio chunk path.

#### B. Whisper-Based Chunk Splitting (Timestamp-Based Extraction)
Since `WhisperTranscriber` processes the entire audio stream and returns precise segment-level timing markers (`[startMs --> endMs]`), it segments the audio retrospectively:
* It analyzes consecutive segments to detect silence pauses between sentences. If a pause exceeds `SILENCE_GAP_MS` (100ms) and the sentence has ended, or if the paragraph exceeds 400 characters, a split boundary is triggered.
* To extract the corresponding audio chunk, it opens the source WAV file using a `RandomAccessFile` and seeks directly to the target byte offsets.
* Since the WAV file is structured as 16kHz, 16-bit mono, each millisecond of audio consumes exactly **32 bytes**:
  $$\text{bytes/ms} = 16 \text{ samples/ms} \times 2 \text{ bytes/sample} = 32 \text{ bytes/ms}$$
* The exact byte range is computed as:
  $$\text{startByte} = 44 + (\text{startMs} \times 32)$$
  $$\text{endByte} = 44 + (\text{endMs} \times 32)$$
* The transcriber reads this precise byte range, packages it as a WAV chunk via `listener.onAudioChunkAvailable()`, and maps it to the paragraph.

### Session Persistence & SharedPreferences Serialization

To support offline, instant session reloads upon application launches or configuration changes without re-running any CPU-heavy transcription or smart-formatting models, `Persistency` serializes the finalized state inside the app's private `SharedPreferences` (named `"AintListeningPrefs"`).

#### Save Serialization Details (`Persistency.saveLastMessage`)
When `Persistency.saveLastMessage(paragraphs, locale)` is called, it serializes the entire session state into a single JSON array format:
* **JSON Array Structure**: Every paragraph in the list is packed as a `JSONObject` containing the following exact fields:
  - `"raw"`: *(String)* The raw, unformatted speech text transcribed by the engine.
  - `"formatted"`: *(String, nullable)* The post-processed smart-formatted text containing restored capitalization, spelling corrections, and punctuation marks.
  - `"showFormatted"`: *(Boolean)* A toggle state flag indicating whether the user is currently displaying the formatted version or the raw text in the UI list.
  - `"audioPath"`: *(String, nullable)* The absolute local directory file path of the corresponding split WAV audio chunk (`chunk_*.wav`).
* **Preferences Storage**:
  - The `JSONArray` is converted to a string and written under the preference key `"last_paragraphs_json"`.
  - The active language model's locale metadata is serialized under the key `"last_locale_tag"` using `locale.toLanguageTag()`.
  - The changes are immediately flushed using `apply()`.

#### State Reconstruction Details (`Persistency.loadLastMessage`)
When `loadLastMessage()` is invoked during app launch:
1. It reads the raw JSON string from `"last_paragraphs_json"`.
2. If available, it iterates through the `JSONArray` items, reconstructs individual `TranscriptionParagraph` objects, populating their raw, formatted, showFormatted, and audioPath fields, and adds them to a list.
3. The list is returned to the `MainViewModel`, which transitions the UI state straight to `MainUiState.idle(paragraphs)`, allowing instantaneous rendering in the activity's `RecyclerView` with operational audio playback buttons.

---

## 5. End-To-End Runtime Ingestion Sequence

The following sequence diagram traces the complete runtime flow of an incoming share intent, showing the thread hops and execution steps from ingestion to final persistence:

```mermaid
sequenceDiagram
    autonumber
    
    %% Participant aliases and thread classification
    participant MA as MainActivity (Main Thread)
    participant VM as MainViewModel (Main Thread)
    participant TP as TranscriptionProcessor (Processor Thread)
    participant DEC as OpusToWavDecoder (Processor Thread)
    participant PE as Persistency (Processor Thread)
    participant TR as TranscriberRegistry (Processor Thread)
    participant T as Transcriber (Processor Thread)
    participant SF as OnnxSmartFormatter (Processor Thread)

    %% Flow initiation
    Note over MA: User shares audio file from external app
    MA->>MA: handleIncomingIntent(intent)
    MA->>MA: checkModelsAndProceed(audioUri)
    
    MA->>VM: startTranscription(audioUri, modelIndex)
    activate VM
    VM->>VM: Update UI state to loading("Preparing...")
    
    VM->>TP: startTranscription(audioUri, modelIndex, callback)
    activate TP
    
    %% Background Dispatching
    Note over TP: Tasks executed sequentially on single-thread executor
    
    TP->>VM: onStatusUpdate("Converting audio...")
    VM-->>MA: Update LiveData (Status displayed)
    
    TP->>PE: getIncomingWavFile()
    PE-->>TP: wavFile
    TP->>PE: clearTemporaryFiles()
    Note over PE: Deletes old WAV and chunk files
    
    %% Decoding and Resampling
    TP->>DEC: decodeOpusToWav(context, audioUri, wavFile)
    activate DEC
    Note over DEC: MediaExtractor reads encoded stream
    Note over DEC: MediaCodec decodes to PCM
    DEC->>DEC: processPcmTo16kMono(pcmData, nativeSampleRate, channels)
    Note over DEC: Down-sample to 16kHz & downmix channels
    DEC-->>TP: conversionSuccess
    deactivate DEC
    
    %% Engine & Model Loading
    TP->>VM: onStatusUpdate("Loading model...")
    VM-->>MA: Update LiveData (Status displayed)
    
    TP->>TR: getTranscriber(activeType)
    TR-->>TP: Transcriber
    TP->>T: ensureModelLoaded(context, modelIndex)
    
    %% Transcription Loop
    TP->>VM: onStatusUpdate("Transcribing...")
    VM-->>MA: Update LiveData (Status displayed)
    
    TP->>T: transcribe(context, wavFile, listener)
    activate T
    
    loop Stream Processing & Chunking
        Note over T: Process speech input
        T->>PE: saveAudioChunk(pcmData, chunkIndex)
        PE-->>T: chunkPath (chunk_x.wav saved)
        T->>TP: onPartialResult(interimText)
        TP->>VM: onPartialResult(interimParagraphs)
        VM-->>MA: Update LiveData (Incremental paragraphs updated)
    end
    
    T-->>TP: rawParagraphs
    deactivate T
    
    %% Smart Formatting
    alt Smart Formatting Enabled & Required
        TP->>VM: onStatusUpdate("Applying smart formatting...")
        VM-->>MA: Update LiveData (Status displayed)
        
        loop For Each Paragraph
            TP->>SF: format(rawText)
            SF-->>TP: formattedText
            TP->>VM: onSmartFormattingProgress(progress, total, currentList)
            VM-->>MA: Update LiveData (Progress indicator updated)
        end
    end
    
    %% Persistence and Completion
    TP->>PE: saveLastMessage(formattedParagraphs, modelIndex)
    Note over PE: Serializes paragraphs to JSON in SharedPreferences
    
    TP->>VM: onComplete(formattedParagraphs)
    deactivate TP
    
    VM->>VM: Update UI state to idle(paragraphs)
    VM-->>MA: Update LiveData
    MA->>MA: Render paragraphs and enable playback buttons
    deactivate VM
```
*Figure 3: Sequence diagram detailing the ingestion and processing flow starting from an incoming Intent.*
