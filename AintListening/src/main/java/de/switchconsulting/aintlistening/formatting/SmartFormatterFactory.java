/*
 * Copyright 2026 Switch Consulting (https://switch-consulting.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.switchconsulting.aintlistening.formatting;

import androidx.annotation.NonNull;

import de.switchconsulting.aintlistening.data.ModelInfo;

/**
 * Factory interface for creating {@link SmartFormatter} instances.
 */
public interface SmartFormatterFactory {

    /**
     * Creates a {@link SmartFormatter} for the given model metadata.
     *
     * @param modelInfo Metadata of the model to load.
     * @return An initialized {@link SmartFormatter}.
     * @throws Exception If model or tokenizer initialization fails.
     */
    @NonNull
    SmartFormatter create(@NonNull ModelInfo modelInfo) throws Exception;
}
