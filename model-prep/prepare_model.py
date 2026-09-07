import os
import zipfile
import shutil
import argparse
from huggingface_hub import hf_hub_download
from transformers import XLMRobertaTokenizer
from onnxruntime.quantization import quantize_dynamic, QuantType

# This is a multi-head XLM-RoBERTa model (~1.1GB unquantized).
# Gold standard for joint punctuation and capitalization restoration.
MODEL_ID = "1-800-BAD-CODE/xlm-roberta_punctuation_fullstop_truecase"

EXPORT_DIR = "./onnx_export"
QUANT_DIR = "./onnx_quantized"
MODELS_DIR = "../models"

def prepare_model(locale="de"):
    zip_filename = f"ONNXModel_{locale}.zip"
    zip_path = os.path.join(MODELS_DIR, zip_filename)

    # Clean up and ensure directories exist
    if not os.path.exists(MODELS_DIR):
        os.makedirs(MODELS_DIR)
    for d in [EXPORT_DIR, QUANT_DIR]:
        if os.path.exists(d):
            shutil.rmtree(d)
        os.makedirs(d)

    # 1. Download files manually (this is a NeMo export, not a standard HF model)
    print(f"--- Step 1: Downloading {MODEL_ID} files ---")
    hf_hub_download(repo_id=MODEL_ID, filename="model.onnx", local_dir=EXPORT_DIR)
    hf_hub_download(repo_id=MODEL_ID, filename="config.yaml", local_dir=EXPORT_DIR)
    hf_hub_download(repo_id=MODEL_ID, filename="sp.model", local_dir=EXPORT_DIR)

    # 2. Prepare Tokenizer
    print(f"\n--- Step 2: Preparing Tokenizer ---")
    # Using use_fast=False to ensure compatibility with SentencePiece and avoid conversion bugs
    tokenizer = XLMRobertaTokenizer.from_pretrained("xlm-roberta-base", use_fast=False)
    tokenizer.save_pretrained(EXPORT_DIR)

    # 3. Quantize (Mandatory for this 1.1GB model)
    print(f"\n--- Step 3: Quantizing model (INT8) ---")
    input_model_path = os.path.join(EXPORT_DIR, "model.onnx")
    output_model_path = os.path.join(QUANT_DIR, "model.onnx")

    quantize_dynamic(
        input_model_path,
        output_model_path,
        weight_type=QuantType.QUInt8
    )
    print(f"Quantized model saved to {QUANT_DIR}")

    # 4. Packaging into ZIP
    print(f"\n--- Step 4: Packaging files into {zip_path} ---")
    model_name = os.path.splitext(zip_filename)[0]
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zipf:
        # Include Quantized Model
        zipf.write(output_model_path, f"{model_name}/model.onnx")

        # Include Tokenizer files
        tokenizer_files = ["tokenizer.json", "tokenizer_config.json", "special_tokens_map.json", "sentencepiece.bpe.model"]
        for file in os.listdir(EXPORT_DIR):
            if file in tokenizer_files:
                file_path = os.path.join(EXPORT_DIR, file)
                zipf.write(file_path, f"{model_name}/{file}")
                print(f"Added {file} to zip")

    print(f"\nSuccess! '{zip_path}' is ready (~280MB).")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Prepare ONNX model for Android")
    parser.add_argument("--locale", type=str, default="de", help="Locale for the output filename (e.g. de, en)")
    args = parser.parse_args()

    try:
        prepare_model(args.locale)
    except Exception as e:
        print(f"\nCRITICAL ERROR during model preparation: {e}")
