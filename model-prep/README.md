# Model Prep Tool (Python)

This tool downloads the `sonar-base` punctuation model, converts it to ONNX, quantizes it for mobile, and packages it into `punct_de.zip` for the AintListening Android app.

## Setup

1. Create a virtual environment:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
   *Note: Using a recent version of transformers (>= 4.40) is recommended to avoid tokenizer regex issues.*

## Usage

## Usage

There are two options depending on your size requirements:

### Option 1: High Quality (Transformer-based)
Best for accuracy, but larger (~200MB).
```bash
python prepare_model.py --locale de
```
- Uses `oliverguhr/fullstop-punctuation-multilingual-sonar-base`.
- Outputs `../models/ONNXModel_de.zip`.

### Option 2: Embedded Optimized (Silero)
Best for mobile/embedded devices (~40MB).
```bash
python prepare_silero.py
```
- Uses Silero TE v2.
- Outputs `../models/ONNXModel_de.zip`.
- **Note**: Silero models have a different architecture. Ensure your Android code handles the specific input/output shapes of Silero.

## Why Python?
Model export and quantization rely on the `optimum` and `transformers` libraries, which are natively supported in Python. This ensures we can trace the PyTorch graph correctly and apply mobile-optimized compression.
