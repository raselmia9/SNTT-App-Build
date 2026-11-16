package com.sntt.appbuild;

import android.webkit.JavascriptInterface;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;
import java.util.Set;

public class TinyDBJsInterface {
    public interface Listener {
        void onRequestDownload();
        void onShipSelected(String shipJson);
        void onOpenTool(String tinyDbTag);
        void onGoHome();
        void onShowToast(String message);
        void onSaveImage(String base64Data, String fileName);
        void onShareImage(String base64Data, String fileName);
    }

    private final TinyDB tinyDB;
    private final Listener listener;
    private String lastWebViewString = "";
    private volatile boolean modalOpen = false;

    @JavascriptInterface
    public void setModalOpen(boolean open) {
        this.modalOpen = open;
    }

    @JavascriptInterface
    public void setPopupOpen(boolean open) {
        this.modalOpen = open;
    }

    @JavascriptInterface
    public boolean isModalOpen() {
        return this.modalOpen;
    }

    public void resetModalState() {
        this.modalOpen = false;
    }

    public TinyDBJsInterface(TinyDB tinyDB, Listener listener) {
        this.tinyDB = tinyDB;
        this.listener = listener;
    }

    @JavascriptInterface
    public String getValue(String tag, String defaultValue) {
        return tinyDB.getString(tag, defaultValue);
    }

    @JavascriptInterface
    public String getValue(String tag) {
        return tinyDB.getString(tag, "");
    }

    @JavascriptInterface
    public void storeValue(String tag, String value) {
        tinyDB.putString(tag, value);
    }

    @JavascriptInterface
    public boolean hasTag(String tag) {
        return tinyDB.hasTag(tag);
    }

    @JavascriptInterface
    public String getAllTags() {
        try {
            JSONArray arr = new JSONArray();
            Set<String> tags = tinyDB.getTags();
            for (String tag : tags) {
                arr.put(tag);
            }
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    @JavascriptInterface
    public String getAll() {
        try {
            JSONObject obj = new JSONObject();
            Map<String, ?> all = tinyDB.getAll();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                obj.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @JavascriptInterface
    public String getLocalStatus() {
        try {
            JSONObject obj = new JSONObject();
            Set<String> tags = tinyDB.getTags();
            obj.put("tagCount", tags.size());
            obj.put("hasHomeUI", tinyDB.hasTag("SNTT HOME UI.html"));
            obj.put("hasCalibration", tinyDB.hasTag("Ship Calibration.html"));

            JSONArray tagArray = new JSONArray();
            for (String tag : tags) {
                tagArray.put(tag);
            }
            obj.put("tags", tagArray);
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @JavascriptInterface
    public String getStatus() {
        return getLocalStatus();
    }

    @JavascriptInterface
    public void setWebViewString(String value) {
        this.lastWebViewString = value;
        if (listener != null && value != null) {
            if (value.startsWith("{") && value.contains("Ship Name")) {
                listener.onShipSelected(value);
            }
        }
    }

    @JavascriptInterface
    public String getWebViewString() {
        return lastWebViewString != null ? lastWebViewString : "";
    }

    @JavascriptInterface
    public void onShipSelected(String shipJson) {
        if (listener != null) {
            listener.onShipSelected(shipJson);
        }
    }

    @JavascriptInterface
    public void openTool(String tinyDbTag) {
        if (listener != null && tinyDbTag != null) {
            listener.onOpenTool(tinyDbTag);
        }
    }

    @JavascriptInterface
    public void startDownload() {
        if (listener != null) {
            listener.onRequestDownload();
        }
    }

    @JavascriptInterface
    public void goHome() {
        if (listener != null) {
            listener.onGoHome();
        }
    }

    @JavascriptInterface
    public void showToast(String message) {
        if (listener != null) {
            listener.onShowToast(message);
        }
    }

    @JavascriptInterface
    public void saveImage(String base64Data, String fileName) {
        if (listener != null) {
            listener.onSaveImage(base64Data, fileName);
        }
    }

    @JavascriptInterface
    public void shareImage(String base64Data, String fileName) {
        if (listener != null) {
            listener.onShareImage(base64Data, fileName);
        }
    }
}
