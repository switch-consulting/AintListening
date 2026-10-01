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

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt module for binding formatting abstractions to their concrete implementations.
 */
@Module
@InstallIn(SingletonComponent.class)
@SuppressWarnings("unused")
public abstract class FormattingModule {

    /**
     * Binds the {@link OnnxSmartFormatterFactory} implementation to the {@link SmartFormatterFactory} interface.
     *
     * @param impl The concrete OnnxSmartFormatterFactory instance.
     * @return The SmartFormatterFactory interface.
     */
    @Binds
    @Singleton
    public abstract SmartFormatterFactory bindSmartFormatterFactory(OnnxSmartFormatterFactory impl);
}
