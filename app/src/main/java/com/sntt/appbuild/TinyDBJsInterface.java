package com.sntt.appbuild;

import android.webkit.JavascriptInterface;

public class TinyDBJsInterface {
    private final TinyDB tinyDB;

    public TinyDBJsInterface(TinyDB tinyDB) {
        this.tinyDB = tinyDB;
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
}
