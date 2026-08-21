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

Run the script to generate the ZIP file:
```bash
python prepare_model.py
```

The script will:
- Export the model from Hugging Face to ONNX.
- Apply INT8 quantization (reducing size from ~1.1GB to ~280MB).
- Package `model.onnx`, `config.json`, `tokenizer.json`, etc., into `punct_de.zip`.

## Why Python?
Model export and quantization rely on the `optimum` and `transformers` libraries, which are natively supported in Python. This ensures we can trace the PyTorch graph correctly and apply mobile-optimized compression.
