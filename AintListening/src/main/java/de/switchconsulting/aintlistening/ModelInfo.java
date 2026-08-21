package de.switchconsulting.aintlistening;

/**
 * Metadata for a Vosk speech model.
 */
public class ModelInfo {
    final String name;
    final String url;
    final String displayName;
    final String size;

    public ModelInfo(String name, String url, String displayName, String size) {
        this.name = name;
        this.url = url;
        this.displayName = displayName;
        this.size = size;
    }
}
