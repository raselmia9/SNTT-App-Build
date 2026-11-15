package com.sntt.appbuild;

public class DownloadItem {
    public enum Status {
        PENDING,
        DOWNLOADING,
        COMPLETED,
        FAILED,
        SKIPPED
    }

    private final String tinyDbName;
    private final String fileLink;
    private Status status = Status.PENDING;
    private String message = "";

    public DownloadItem(String tinyDbName, String fileLink) {
        this.tinyDbName = tinyDbName;
        this.fileLink = fileLink;
    }

    public String getTinyDbName() {
        return tinyDbName;
    }

    public String getFileLink() {
        return fileLink;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
