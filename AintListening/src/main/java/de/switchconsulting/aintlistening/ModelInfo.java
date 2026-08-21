package de.switchconsulting.aintlistening;

/**
 * Data class containing metadata for a Vosk speech model.
 */
public class ModelInfo {
    /** The internal name/directory name of the model. */
    final String name;
    /** The URL where the model zip file can be downloaded. */
    final String url;
    /** The human-readable name of the language. */
    final String displayName;
    /** The approximate download size of the model (e.g., "45MB"). */
    final String size;

    /**
     * Constructs a new ModelInfo.
     *
     * @param name        The internal name of the model.
     * @param url         The download URL.
     * @param displayName The display name.
     * @param size        The download size.
     */
    public ModelInfo(String name, String url, String displayName, String size) {
        this.name = name;
        this.url = url;
        this.displayName = displayName;
        this.size = size;
    }
}
