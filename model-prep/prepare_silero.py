import os
import zipfile
import requests
import shutil
from transformers import AutoTokenizer

# Configuration
# Silero TE (Text Enhancement) v2 is highly optimized for mobile (~40MB)
MODEL_URL = "https://models.silero.ai/models/punctuation/v2_recasepunc.onnx"
TOKENIZER_ID = "bert-base-multilingual-cased" # Silero 4-lang uses this tokenizer
MODELS_DIR = "../models"
EXPORT_DIR = "./silero_temp"

def prepare_silero(locale="de"):
    zip_filename = f"ONNXModel_{locale}.zip"
    zip_path = os.path.join(MODELS_DIR, zip_filename)

    if os.path.exists(EXPORT_DIR):
        shutil.rmtree(EXPORT_DIR)
    os.makedirs(EXPORT_DIR)

    # 1. Download the ONNX model
    model_path = os.path.join(EXPORT_DIR, "model.onnx")
    print(f"--- Step 1: Downloading Silero ONNX model ---")
    response = requests.get(MODEL_URL, stream=True)
    if response.status_code == 200:
        with open(model_path, 'wb') as f:
            shutil.copyfileobj(response.raw, f)
        print(f"Downloaded to {model_path}")
    else:
        print(f"Error downloading model: {response.status_code}")
        return

    # 2. Download and save the tokenizer
    print(f"\n--- Step 2: Saving tokenizer ({TOKENIZER_ID}) ---")
    tokenizer = AutoTokenizer.from_pretrained(TOKENIZER_ID)
    tokenizer.save_pretrained(EXPORT_DIR)

    # 3. Packaging into ZIP
    print(f"\n--- Step 3: Packaging into {zip_path} ---")
    model_name = os.path.splitext(zip_filename)[0]
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zipf:
        for root, _, files in os.walk(EXPORT_DIR):
            for file in files:
                file_path = os.path.join(root, file)
                rel_path = os.path.relpath(file_path, EXPORT_DIR)
                arcname = f"{model_name}/{rel_path.replace(os.sep, '/')}"
                zipf.write(file_path, arcname)
                print(f"Added {arcname} to zip")

    # Cleanup
    shutil.rmtree(EXPORT_DIR)
    print(f"\nSuccess! '{zip_path}' is ready (~40MB).")

if __name__ == "__main__":
    prepare_silero()
