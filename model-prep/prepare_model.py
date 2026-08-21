import os
import zipfile
import shutil
import argparse
from optimum.onnxruntime import ORTModelForTokenClassification
from transformers import AutoTokenizer
from optimum.onnxruntime.configuration import AutoQuantizationConfig
from optimum.onnxruntime import ORTQuantizer

# Configuration
MODEL_ID = "oliverguhr/fullstop-punctuation-multilingual-sonar-base"
EXPORT_DIR = "./onnx_export"
QUANT_DIR = "./onnx_quantized"
ZIP_NAME = "punct_de.zip"

def prepare_model():
    # 1. Export to ONNX
    print(f"--- Step 1: Exporting {MODEL_ID} to ONNX ---")
    if os.path.exists(EXPORT_DIR):
        shutil.rmtree(EXPORT_DIR)

    # Added fix_mistral_regex=True as suggested by the transformers warning to handle specific regex issues in some tokenizers
    try:
        tokenizer = AutoTokenizer.from_pretrained(MODEL_ID, fix_mistral_regex=True)
    except TypeError:
        # Fallback if the installed transformers version doesn't support the flag yet
        tokenizer = AutoTokenizer.from_pretrained(MODEL_ID)

    model = ORTModelForTokenClassification.from_pretrained(MODEL_ID, export=True)

    model.save_pretrained(EXPORT_DIR)
    tokenizer.save_pretrained(EXPORT_DIR)
    print(f"Exported to {EXPORT_DIR}")

    # 2. Quantize (Crucial for Mobile - reduces size by ~4x)
    print(f"\n--- Step 2: Quantizing model (INT8) ---")
    if os.path.exists(QUANT_DIR):
        shutil.rmtree(QUANT_DIR)

    try:
        quantizer = ORTQuantizer.from_pretrained(EXPORT_DIR, fix_mistral_regex=True)
    except TypeError:
        quantizer = ORTQuantizer.from_pretrained(EXPORT_DIR)

    # Using 'arm64' or basic 'avx' configs works well for generic mobile targets
    dqconfig = AutoQuantizationConfig.arm64(is_static=False, per_channel=False)

    quantizer.quantize(
        save_dir=QUANT_DIR,
        quantization_config=dqconfig,
    )

    # Copy necessary meta-files (tokenizer, config) to quantization directory
    for file in os.listdir(EXPORT_DIR):
        if not file.endswith(".onnx"):
            src = os.path.join(EXPORT_DIR, file)
            dst = os.path.join(QUANT_DIR, file)
            if os.path.isfile(src):
                shutil.copy(src, dst)
    print(f"Quantized model saved to {QUANT_DIR}")

    # 3. Packaging into ZIP
    print(f"\n--- Step 3: Packaging files into {ZIP_NAME} ---")
    with zipfile.ZipFile(ZIP_NAME, "w", zipfile.ZIP_DEFLATED) as zipf:
        for root, _, files in os.walk(QUANT_DIR):
            for file in files:
                # Include model, config, and tokenizer files
                if file.endswith((".onnx", ".json", ".txt", ".model")):
                    file_path = os.path.join(root, file)

                    # Rename the quantized model to 'model.onnx' inside the zip
                    # so the Android app can always look for the same filename
                    if "model_quantized.onnx" in file:
                        arcname = "model.onnx"
                    else:
                        arcname = os.path.relpath(file_path, QUANT_DIR)

                    zipf.write(file_path, arcname)
                    print(f"Added {arcname} to zip")

    print(f"\nSuccess! '{ZIP_NAME}' is ready for the Android app.")

if __name__ == "__main__":
    prepare_model()
