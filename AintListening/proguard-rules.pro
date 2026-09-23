# Copyright 2026 Switch Consulting (https://switch-consulting.de/)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Hilt
-keep,allowobfuscation @dagger.hilt.android.AndroidEntryPoint class *
-keep,allowobfuscation @dagger.hilt.android.HiltAndroidApp class *
-keep,allowobfuscation @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Retain all native method signatures
-keepclasseswithmembernames class * {
    native <methods>;
}

# Prevent stripping of JNI interface classes
-keep class com.facebook.jni.** { *; }

# ONNX Runtime Native rules
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# HuggingFace / Deep Java Library (DJL) Tokenizer rules
-keep class ai.djl.huggingface.tokenizers.** { *; }
-keep class ai.djl.android.** { *; }
-dontwarn ai.djl.**

# WhisperCore C++ Wrapper rules
-keep class com.redravencomputing.whispercore.** { *; }
-dontwarn com.redravencomputing.whispercore.**

# Vosk Speech-to-Text Engine rules
-keep class com.alphacephei.vosk.** { *; }
-keep class org.kaldi.** { *; }
-keep class org.vosk.** { *; }
-dontwarn org.vosk.**
-dontwarn com.alphacephei.vosk.**
-dontwarn org.kaldi.**

# Apache Commons (used in archive extraction / compress)
-dontwarn org.apache.commons.**
