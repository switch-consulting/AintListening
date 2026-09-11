package de.switchconsulting.aintlistening.di;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import de.switchconsulting.aintlistening.Persistency;
import de.switchconsulting.aintlistening.Transcriber;
import de.switchconsulting.aintlistening.VoskTranscriber;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public static Persistency providePersistency(@ApplicationContext Context context) {
        return new Persistency(context);
    }

    @Provides
    @Singleton
    public static Transcriber provideTranscriber() {
        return new VoskTranscriber();
    }
}
