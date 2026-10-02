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

package de.switchconsulting.aintlistening.di;

import android.content.Context;

import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt module for providing thread executors across the app.
 */
@Module
@InstallIn(SingletonComponent.class)
public class ExecutorsModule {

    /**
     * Provides a singleton ExecutorService for background operations.
     *
     * @return The background ExecutorService.
     */
    @Provides
    @Singleton
    @BackgroundExecutor
    public static ExecutorService provideBackgroundExecutor() {
        return Executors.newFixedThreadPool(4);
    }

    /**
     * Provides a singleton Executor for posting to the main/UI thread.
     *
     * @param context The application context.
     * @return The main thread Executor.
     */
    @Provides
    @Singleton
    @MainExecutor
    public static Executor provideMainExecutor(@ApplicationContext Context context) {
        return ContextCompat.getMainExecutor(context);
    }
}
