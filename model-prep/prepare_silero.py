import os
import zipfile
import requests
import shutil

# Configuration
# Silero TE (Text Enhancement) v2 is highly optimized for mobile/embedded (~40MB)
MODEL_URL = "https://models.silero.ai/models/punctuation/v2_recasepunc.onnx"
MODELS_DIR = "../models"

def prepare_silero(locale="de"):
    zip_filename = f"ONNXModel_{locale}.zip"
    zip_path = os.path.join(MODELS_DIR, zip_filename)
    temp_model = "model.onnx"

    # 1. Ensure directories exist
    if not os.path.exists(MODELS_DIR):
        os.makedirs(MODELS_DIR)

    # 2. Download the pre-quantized ONNX model from Silero
    print(f"--- Step 1: Downloading Silero ONNX model ---")
    response = requests.get(MODEL_URL, stream=True)
    if response.status_code == 200:
        with open(temp_model, 'wb') as f:
            shutil.copyfileobj(response.raw, f)
        print(f"Downloaded to {temp_model}")
    else:
        print(f"Error downloading model: {response.status_code}")
        return

    # 3. Packaging into ZIP
    # Note: Silero doesn't require a complex tokenizer.json for the ONNX runtime
    # as it's often handled by simple char-level or custom logic,
    # but we package it as 'model.onnx' for compatibility with your app's extractor.
    print(f"\n--- Step 2: Packaging into {zip_path} ---")
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zipf:
        zipf.write(temp_model, "model.onnx")
        # Silero models are self-contained, but if your app expects a config.json,
        # you might need to add a placeholder one here.

    # Cleanup
    if os.path.exists(temp_model):
        os.remove(temp_model)

    print(f"\nSuccess! '{zip_path}' is ready (~40MB).")
    print("NOTE: Silero models use a different architecture than Transformers.")
    print("Ensure your Android ONNX inference code is compatible with Silero's input/output shapes.")

if __name__ == "__main__":
    prepare_silero()
