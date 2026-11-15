package com.sntt.appbuild;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;
import java.util.Set;

/**
 * TinyDB helper implementation matching MIT App Inventor / Kodular conventions.
 * Data is persisted in SharedPreferences under "TinyDB1".
 */
public class TinyDB {
    private static final String PREF_NAME = "TinyDB1";
    private final SharedPreferences preferences;

    public TinyDB(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void putString(String tag, String value) {
        preferences.edit().putString(tag, value).apply();
    }

    public synchronized String getString(String tag, String defaultValue) {
        return preferences.getString(tag, defaultValue);
    }

    public synchronized String getString(String tag) {
        return getString(tag, "");
    }

    public synchronized boolean hasTag(String tag) {
        return preferences.contains(tag);
    }

    public synchronized Map<String, ?> getAll() {
        return preferences.getAll();
    }

    public synchronized Set<String> getTags() {
        return preferences.getAll().keySet();
    }

    public synchronized void remove(String tag) {
        preferences.edit().remove(tag).apply();
    }

    public synchronized void clear() {
        preferences.edit().clear().apply();
    }
}
