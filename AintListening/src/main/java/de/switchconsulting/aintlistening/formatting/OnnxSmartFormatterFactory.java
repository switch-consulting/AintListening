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

import android.content.Context;

import androidx.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import de.switchconsulting.aintlistening.data.ModelInfo;

/**
 * Default implementation of {@link SmartFormatterFactory} that instantiates
 * {@link OnnxSmartFormatter} using the application context.
 */
@Singleton
public class OnnxSmartFormatterFactory implements SmartFormatterFactory {

    private final Context context;

    /**
     * Constructs a new {@link OnnxSmartFormatterFactory}.
     *
     * @param context The application context.
     */
    @Inject
    public OnnxSmartFormatterFactory(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override
    @NonNull
    public SmartFormatter create(@NonNull ModelInfo modelInfo) throws Exception {
        return new OnnxSmartFormatter(context, modelInfo);
    }
}
