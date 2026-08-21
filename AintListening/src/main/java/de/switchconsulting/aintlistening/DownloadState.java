package de.switchconsulting.aintlistening;

public class DownloadState {
    public enum Status { IDLE, DOWNLOADING, EXTRACTING, SUCCESS, ERROR }

    public final Status status;
    public final int progress;
    public final Exception error;

    private DownloadState(Status status, int progress, Exception error) {
        this.status = status;
        this.progress = progress;
        this.error = error;
    }

    public static DownloadState idle() { return new DownloadState(Status.IDLE, 0, null); }
    public static DownloadState downloading(int progress) { return new DownloadState(Status.DOWNLOADING, progress, null); }
    public static DownloadState extracting() { return new DownloadState(Status.EXTRACTING, 0, null); }
    public static DownloadState success() { return new DownloadState(Status.SUCCESS, 100, null); }
    public static DownloadState error(Exception e) { return new DownloadState(Status.ERROR, 0, e); }
}
