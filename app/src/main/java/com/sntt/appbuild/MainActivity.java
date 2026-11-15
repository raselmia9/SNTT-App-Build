package com.sntt.appbuild;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String JSON_LIST_URL = "https://raw.githubusercontent.com/raselmia9/SNTT-ALL-DATA/refs/heads/main/SNTT%20Download%20List.json";
    private static final String TAG_HOME_UI = "SNTT HOME UI.html";
    private static final String TAG_CALIBRATION_TEMPLATE = "Ship Calibration.html";

    private TinyDB tinyDB;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CardView cardProgress;
    private ProgressBar progressBar;
    private TextView tvOverallStatus, tvDetailStatus;
    private LinearLayout layoutCounters;
    private TextView tvCountSuccess, tvCountFailed, tvCountSkipped;
    private WebView webView;

    private boolean isDownloading = false;
    private boolean isHomeView = true;
    private int successCount = 0;
    private int failedCount = 0;
    private int skippedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("SNTT App Build");
            getSupportActionBar().setSubtitle("Tank Calibrator & UI");
        }

        tinyDB = new TinyDB(this);

        initViews();
        setupWebView();
        loadHomeUI();
    }

    private void initViews() {
        cardProgress = findViewById(R.id.cardProgress);
        progressBar = findViewById(R.id.progressBar);
        tvOverallStatus = findViewById(R.id.tvOverallStatus);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        layoutCounters = findViewById(R.id.layoutCounters);
        tvCountSuccess = findViewById(R.id.tvCountSuccess);
        tvCountFailed = findViewById(R.id.tvCountFailed);
        tvCountSkipped = findViewById(R.id.tvCountSkipped);
        webView = findViewById(R.id.webView);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        TinyDBJsInterface jsBridge = new TinyDBJsInterface(tinyDB, new TinyDBJsInterface.Listener() {
            @Override
            public void onRequestDownload() {
                mainHandler.post(() -> startDownloadAll());
            }

            @Override
            public void onShipSelected(String shipJson) {
                mainHandler.post(() -> handleShipSelection(shipJson));
            }

            @Override
            public void onGoHome() {
                mainHandler.post(() -> loadHomeUI());
            }

            @Override
            public void onShowToast(String message) {
                mainHandler.post(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });

        webView.addJavascriptInterface(jsBridge, "TinyDB");
        webView.addJavascriptInterface(jsBridge, "Android");
        webView.addJavascriptInterface(jsBridge, "AppInventor");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectBridgesAndSync(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return super.onConsoleMessage(consoleMessage);
            }
        });
    }

    private void injectBridgesAndSync(WebView view) {
        StringBuilder js = new StringBuilder();
        js.append("(function() {");
        js.append("  if (!window.AppInventor) {");
        js.append("    window.AppInventor = {");
        js.append("      setWebViewString: function(s) { if(window.TinyDB) window.TinyDB.setWebViewString(s); },");
        js.append("      getWebViewString: function() { return window.TinyDB ? window.TinyDB.getWebViewString() : ''; }");
        js.append("    };");
        js.append("  }");
        // Sync tags into localStorage
        Map<String, ?> all = tinyDB.getAll();
        for (String key : all.keySet()) {
            String safeKey = key.replace("'", "\\'");
            js.append("  try { localStorage.setItem('").append(safeKey).append("', window.TinyDB.getValue('").append(safeKey).append("')); } catch(e){}");
        }
        js.append("})();");
        view.evaluateJavascript(js.toString(), null);
    }

    public void loadHomeUI() {
        isHomeView = true;
        String homeHtml = tinyDB.getString(TAG_HOME_UI, "");
        if (!homeHtml.isEmpty()) {
            webView.loadDataWithBaseURL("https://sntt.local/", homeHtml, "text/html", "UTF-8", null);
        } else {
            // Friendly welcome placeholder prompting download
            String welcomeHtml = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                    "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                    "<style>" +
                    "*{box-sizing:border-box}body{margin:0;padding:24px 16px;font-family:Arial,sans-serif;background:#f1f5f9;color:#1e293b;display:flex;align-items:center;justify-content:center;min-height:85vh;text-align:center;}" +
                    ".card{background:#ffffff;border-radius:18px;padding:32px 20px;max-width:380px;box-shadow:0 10px 25px rgba(0,0,0,0.06);border:1px solid #e2e8f0;}" +
                    ".badge{display:inline-block;padding:5px 14px;border-radius:20px;background:#e0f2fe;color:#0284c7;font-size:12px;font-weight:700;margin-bottom:14px;}" +
                    "h1{color:#1e3a8a;font-size:22px;margin:0 0 10px;font-weight:800;}" +
                    "p{color:#64748b;font-size:14px;line-height:1.5;margin:0 0 24px;}" +
                    ".btn{display:inline-block;width:100%;padding:14px 18px;background:linear-gradient(135deg,#2563eb,#1d4ed8);color:#ffffff;border:none;border-radius:12px;font-size:15px;font-weight:700;cursor:pointer;box-shadow:0 4px 12px rgba(37,99,235,0.3);}" +
                    ".btn:active{transform:scale(0.98);}" +
                    "</style></head><body>" +
                    "<div class='card'>" +
                    "<span class='badge'>SNTT App Build</span>" +
                    "<h1>স্বাগতম</h1>" +
                    "<p>আপনার অ্যাপ্লিকেশনের ডাটাবেজ এবং ইউআই লোড করতে উপরের ডাউনলোড আইকন অথবা নিচের বাটনে চাপ দিন।</p>" +
                    "<button class='btn' onclick='if(window.Android) window.Android.startDownload();'>📥 ডাউনলোড শুরু করুন</button>" +
                    "</div></body></html>";
            webView.loadDataWithBaseURL("https://sntt.local/", welcomeHtml, "text/html", "UTF-8", null);
        }
    }

    private void handleShipSelection(String shipJson) {
        try {
            JSONObject obj = new JSONObject(shipJson);
            String shipName = obj.optString("Ship Name", "");
            int tankCount = obj.optInt("Tank C", 8);
            openShipCalibration(shipName, tankCount);
        } catch (Exception e) {
            Toast.makeText(this, "Ship selection error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openShipCalibration(String shipName, int tankCount) {
        String template = tinyDB.getString(TAG_CALIBRATION_TEMPLATE, "");
        if (template.isEmpty()) {
            Toast.makeText(this, "Ship Calibration template not downloaded yet. Please download first.", Toast.LENGTH_LONG).show();
            return;
        }

        String shipData = tinyDB.getString(shipName, "");
        if (shipData.isEmpty()) {
            Toast.makeText(this, "Calibration data for '" + shipName + "' not found in TinyDB. Please download data first.", Toast.LENGTH_LONG).show();
            return;
        }

        isHomeView = false;
        String populatedHtml = template
                .replace("?Ship Name?", shipName)
                .replace("...Total Tank...", String.valueOf(tankCount))
                .replace("/*PUT_JSON_STRING_HERE*/", shipData);

        webView.loadDataWithBaseURL("https://sntt.local/", populatedHtml, "text/html", "UTF-8", null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_download) {
            startDownloadAll();
            return true;
        } else if (item.getItemId() == R.id.action_home) {
            loadHomeUI();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            if (isHomeView) {
                loadHomeUI();
            } else {
                webView.reload();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startDownloadAll() {
        if (isDownloading) {
            Toast.makeText(this, "Download already in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        isDownloading = true;
        cardProgress.setVisibility(View.VISIBLE);
        layoutCounters.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);
        tvOverallStatus.setText("Downloading JSON list...");
        tvDetailStatus.setText("Connecting to server to retrieve download items...");

        successCount = 0;
        failedCount = 0;
        skippedCount = 0;
        updateCounters();

        executorService.execute(() -> {
            List<DownloadItem> downloadItemList = new ArrayList<>();
            try {
                // Fetch the dynamic JSON list
                String jsonListContent = downloadUrlContent(JSON_LIST_URL);
                if (jsonListContent == null || jsonListContent.trim().isEmpty()) {
                    throw new Exception("Empty response from JSON list URL");
                }

                JSONArray jsonArray = new JSONArray(jsonListContent);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String name = obj.optString("TinyDB Name", "").trim();
                    String link = obj.optString("File Link", "").trim();
                    if (!name.isEmpty()) {
                        downloadItemList.add(new DownloadItem(name, link));
                    }
                }

                if (downloadItemList.isEmpty()) {
                    throw new Exception("No valid items found in JSON list");
                }

            } catch (Exception e) {
                final String err = e.getMessage() != null ? e.getMessage() : "Failed to fetch list";
                mainHandler.post(() -> {
                    isDownloading = false;
                    progressBar.setIndeterminate(false);
                    tvOverallStatus.setText("Download list fetch failed");
                    tvDetailStatus.setText(err);
                    Toast.makeText(MainActivity.this, "Error: " + err, Toast.LENGTH_LONG).show();
                    mainHandler.postDelayed(() -> cardProgress.setVisibility(View.GONE), 3000);
                });
                return;
            }

            final int total = downloadItemList.size();

            mainHandler.post(() -> {
                progressBar.setIndeterminate(false);
                progressBar.setMax(total);
                progressBar.setProgress(0);
                layoutCounters.setVisibility(View.VISIBLE);
                tvOverallStatus.setText(String.format("List received (%d items). Starting download...", total));
            });

            for (int i = 0; i < total; i++) {
                final int index = i;
                final DownloadItem item = downloadItemList.get(i);

                mainHandler.post(() -> {
                    tvOverallStatus.setText(String.format("Downloading (%d/%d): %s", index + 1, total, item.getTinyDbName()));
                    tvDetailStatus.setText("Fetching content...");
                });

                String rawUrl = item.getFileLink();
                if (rawUrl == null || rawUrl.trim().equalsIgnoreCase("none") || rawUrl.trim().isEmpty()) {
                    mainHandler.post(() -> {
                        skippedCount++;
                        progressBar.setProgress(index + 1);
                        updateCounters();
                    });
                    continue;
                }

                // Clean url
                String cleanUrl = rawUrl.trim().replaceAll("\s+", "");
                if (cleanUrl.startsWith("Https://")) {
                    cleanUrl = "https://" + cleanUrl.substring(8);
                }

                try {
                    String content = downloadUrlContent(cleanUrl);
                    if (content != null && !content.isEmpty()) {
                        tinyDB.putString(item.getTinyDbName(), content);
                        mainHandler.post(() -> {
                            successCount++;
                            progressBar.setProgress(index + 1);
                            updateCounters();
                        });
                    } else {
                        throw new Exception("Empty content");
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        failedCount++;
                        progressBar.setProgress(index + 1);
                        updateCounters();
                    });
                }

                try {
                    Thread.sleep(150);
                } catch (InterruptedException ignored) {}
            }

            mainHandler.post(() -> {
                isDownloading = false;
                tvOverallStatus.setText(String.format("Finished! %d Saved, %d Failed, %d Skipped", successCount, failedCount, skippedCount));
                tvDetailStatus.setText("All items synchronized to TinyDB.");
                Toast.makeText(MainActivity.this, "Download finished! " + successCount + " items saved.", Toast.LENGTH_SHORT).show();

                // If SNTT HOME UI was saved, load it!
                loadHomeUI();

                // Auto-hide progress card after 4 seconds
                mainHandler.postDelayed(() -> cardProgress.setVisibility(View.GONE), 4000);
            });
        });
    }

    private String downloadUrlContent(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)");
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = conn.getHeaderField("Location");
            if (newUrl != null) {
                conn.disconnect();
                return downloadUrlContent(newUrl);
            }
        }

        if (status != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + status + ": " + conn.getResponseMessage());
        }

        try (InputStream in = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("
");
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private void updateCounters() {
        tvCountSuccess.setText("Success: " + successCount);
        tvCountFailed.setText("Failed: " + failedCount);
        tvCountSkipped.setText("Skipped: " + skippedCount);
    }

    @Override
    public void onBackPressed() {
        if (!isHomeView) {
            loadHomeUI();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
