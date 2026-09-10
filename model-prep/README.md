# Model Prep Tool (Python)

This tool downloads state-of-the-art punctuation and capitalization models, quantizes them for mobile use, and packages them into `ONNXModel_<locale>.zip` for the AintListening Android app.

## Setup

1. **Create a virtual environment**:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

2. **Install dependencies**:
   ```bash
   pip install transformers huggingface_hub onnx onnxruntime sentencepiece protobuf
   ```

## Usage

The tool provides a high-quality joint model that handles both punctuation restoration and German capitalization (true-casing) in a single pass using a Multi-Head XLM-RoBERTa architecture.

It uses the `1-800-BAD-CODE` model which features 4 prediction heads for pre-punctuation, post-punctuation, character-level capitalization, and sentence segmentation.

```bash
python prepare_model.py --locale de
```
- **Model ID**: `1-800-BAD-CODE/xlm-roberta_punctuation_fullstop_truecase`
- **Output**: `../models/ONNXModel_de.zip`
- **Size**: ~280MB (INT8 Quantized)
- **Note**: The first run will download ~1.1GB of model weights before quantizing.

## Troubleshooting

### missing `sentencepiece`
If you encounter an `ImportError` regarding `SentencePiece`, ensure you have installed the library:
```bash
pip install sentencepiece
```

### Tokenizer `NoneType` Error
The script uses `use_fast=False` for XLM-RoBERTa to avoid a known bug in the `transformers` library (v4.45+) where the vocabulary file path is incorrectly resolved for certain multi-head models.

## Why Python?
Model downloading, quantization, and tokenizer serialization rely on the `transformers` and `onnxruntime` ecosystems, which are most robust in Python. This ensures we can apply mobile-optimized INT8 compression (quantization) to reduce the model size by ~4x while maintaining high accuracy.
