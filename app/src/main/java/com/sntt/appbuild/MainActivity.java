package com.sntt.appbuild;

import android.annotation.SuppressLint;
import android.Manifest;
import android.content.Intent;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import android.util.Base64;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String JSON_LIST_URL = "https://raw.githubusercontent.com/raselmia9/SNTT-ALL-DATA/refs/heads/main/SNTT%20Download%20List.json";
    private static final String TAG_HOME_UI = "SNTT HOME UI.html";
    private static final String TAG_CALIBRATION_TEMPLATE = "Ship Calibration.html";
    private static final String TAG_ALL_TANK_CALIBRATION = "All Tank Calibration.html";

    private TinyDB tinyDB;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CardView cardProgress;
    private ProgressBar progressBar;
    private TextView tvOverallStatus, tvDetailStatus;
    private LinearLayout layoutCounters;
    private TextView tvCountSuccess, tvCountFailed, tvCountSkipped;
    private WebView webView;
    private TinyDBJsInterface jsBridge;

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

        jsBridge = new TinyDBJsInterface(tinyDB, new TinyDBJsInterface.Listener() {
            @Override
            public void onRequestDownload() {
                mainHandler.post(() -> startDownloadAll());
            }

            @Override
            public void onShipSelected(String shipJson) {
                mainHandler.post(() -> handleShipSelection(shipJson));
            }

            @Override
            public void onOpenTool(String tinyDbTag) {
                mainHandler.post(() -> openStoredTool(tinyDbTag));
            }

            @Override
            public void onGoHome() {
                mainHandler.post(() -> loadHomeUI());
            }

            @Override
            public void onShowToast(String message) {
                mainHandler.post(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onSaveImage(String base64Data, String fileName) {
                mainHandler.post(() -> saveScreenshotFromWeb(base64Data, fileName));
            }

            @Override
            public void onShareImage(String base64Data, String fileName) {
                mainHandler.post(() -> shareScreenshotFromWeb(base64Data, fileName));
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
        String js = "(function() {"
                + "  if (!window.AppInventor) {"
                + "    window.AppInventor = {"
                + "      setWebViewString: function(s) { if(window.TinyDB) window.TinyDB.setWebViewString(s); },"
                + "      getWebViewString: function() { return window.TinyDB ? window.TinyDB.getWebViewString() : ''; }"
                + "    };"
                + "  }"
                + "  function isElVisible(el) {"
                + "    if (!el) return false;"
                + "    var r = el.getBoundingClientRect();"
                + "    var st = window.getComputedStyle(el);"
                + "    return (r.width > 0 && r.height > 0 && st.display !== 'none' && st.visibility !== 'hidden' && parseFloat(st.opacity || '1') > 0.05);"
                + "  }"
                + "  window.isAnyModalOpen = function() {"
                + "    var sc = document.getElementById('screenshotModalOverlay') || document.querySelector('.screenshot-modal-overlay');"
                + "    if (sc && isElVisible(sc)) return true;"
                + "    var overlays = document.querySelectorAll('.screenshot-modal-overlay, [class*=modal-overlay], [id*=modal-overlay], [class*=popup-overlay], [id*=popup-overlay], dialog[open]');"
                + "    for (var i = 0; i < overlays.length; i++) {"
                + "      var o = overlays[i];"
                + "      if (o === document.body || o === document.documentElement) continue;"
                + "      if (isElVisible(o)) return true;"
                + "    }"
                + "    return false;"
                + "  };"
                + "  window.closeAnyModal = function() {"
                + "    var closed = false;"
                + "    var sc = document.getElementById('screenshotModalOverlay') || document.querySelector('.screenshot-modal-overlay');"
                + "    if (sc && isElVisible(sc)) {"
                + "      if (typeof window.closeScreenshotModal === 'function') { try { window.closeScreenshotModal(); } catch(e) {} }"
                + "      if (sc.parentNode) { sc.parentNode.removeChild(sc); } else { sc.style.display = 'none'; }"
                + "      closed = true;"
                + "    }"
                + "    var overlays = document.querySelectorAll('.screenshot-modal-overlay, [class*=modal-overlay], [id*=modal-overlay], [class*=popup-overlay], [id*=popup-overlay], dialog[open]');"
                + "    for (var i = overlays.length - 1; i >= 0; i--) {"
                + "      var o = overlays[i];"
                + "      if (o === document.body || o === document.documentElement || o.classList.contains('container')) continue;"
                + "      if (isElVisible(o)) {"
                + "        var btn = o.querySelector('.close, [class*=close], [id*=close], button[onclick*=close]');"
                + "        if (btn) { try { btn.click(); } catch(e) {} }"
                + "        else if (typeof o.close === 'function') { try { o.close(); } catch(e) {} }"
                + "        if (o.parentNode) { o.parentNode.removeChild(o); } else { o.style.display = 'none'; }"
                + "        closed = true;"
                + "      }"
                + "    }"
                + "    if (window.TinyDB && typeof window.TinyDB.setModalOpen === 'function') { try { window.TinyDB.setModalOpen(false); } catch(e) {} }"
                + "    return closed;"
                + "  };"
                + "  try {"
                + "    var updateBridgeModal = function() {"
                + "      var open = window.isAnyModalOpen();"
                + "      if (window.TinyDB && typeof window.TinyDB.setModalOpen === 'function') { window.TinyDB.setModalOpen(open); }"
                + "    };"
                + "    var obs = new MutationObserver(updateBridgeModal);"
                + "    obs.observe(document.body || document.documentElement, {"
                + "      childList: true,"
                + "      subtree: true,"
                + "      attributes: true,"
                + "      attributeFilter: ['style', 'class']"
                + "    });"
                + "  } catch(e) {}"
                + "})();";
        view.evaluateJavascript(js, null);
    }

    private void saveScreenshotFromWeb(String base64Data, String fileName) {
        try {
            byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap == null) throw new IllegalArgumentException("Invalid screenshot image");

            String safeName = fileName == null || fileName.trim().isEmpty() ? "Advance_Oil_Calculator.png" : fileName.replaceAll("[^A-Za-z0-9._-]", "_");
            if (!safeName.toLowerCase().endsWith(".png")) safeName += ".png";
            Uri savedUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, safeName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SNTT");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
                savedUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (savedUri == null) throw new IllegalStateException("Could not create image file");
                try (java.io.OutputStream out = getContentResolver().openOutputStream(savedUri)) {
                    if (out == null) throw new IllegalStateException("Could not open image file");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(savedUri, values, null, null);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    pendingImageBase64 = base64Data;
                    pendingImageFileName = safeName;
                    androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                    bitmap.recycle();
                    return;
                }
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SNTT");
                if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create Pictures/SNTT directory");
                File file = new File(dir, safeName);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                android.media.MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, new String[]{"image/png"}, null);
            }
            bitmap.recycle();
            Toast.makeText(this, "ছবি Pictures/SNTT ফোল্ডারে সেভ হয়েছে", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "ছবি সেভ হয়নি: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String pendingImageBase64;
    private String pendingImageFileName;
    private static final int REQUEST_STORAGE_PERMISSION = 701;

    private void shareScreenshotFromWeb(String base64Data, String fileName) {
        try {
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
            File dir = new File(getCacheDir(), "shared_images");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create share cache");
            String safeName = fileName == null || fileName.trim().isEmpty() ? "Advance_Oil_Calculator.png" : fileName.replaceAll("[^A-Za-z0-9._-]", "_");
            if (!safeName.toLowerCase().endsWith(".png")) safeName += ".png";
            File file = new File(dir, safeName);
            try (FileOutputStream out = new FileOutputStream(file)) { out.write(bytes); }

            String authority = getPackageName() + ".fileprovider";
            Uri uri;
            try {
                uri = FileProvider.getUriForFile(this, authority, file);
            } catch (IllegalArgumentException e1) {
                uri = FileProvider.getUriForFile(this, "com.sntt.appbuild.fileprovider", file);
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Advance Oil Calculator");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                intent.setClipData(android.content.ClipData.newRawUri("Screenshot", uri));
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(intent, "ছবি শেয়ার করুন");
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "শেয়ার খোলা যায়নি: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && pendingImageBase64 != null) {
                String data = pendingImageBase64;
                String name = pendingImageFileName;
                pendingImageBase64 = null;
                pendingImageFileName = null;
                saveScreenshotFromWeb(data, name);
            } else {
                Toast.makeText(this, "ছবি সেভ করতে Storage permission দরকার", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void loadHomeUI() {
        isHomeView = true;
        String homeHtml = tinyDB.getString(TAG_HOME_UI, "");
        if (!homeHtml.isEmpty()) {
            webView.loadDataWithBaseURL("https://sntt.local/", homeHtml, "text/html", "UTF-8", null);
        } else {
            String welcomeHtml = "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                    + "<style>"
                    + "*{box-sizing:border-box}body{margin:0;padding:24px 16px;font-family:Arial,sans-serif;background:#f1f5f9;color:#1e293b;display:flex;align-items:center;justify-content:center;min-height:85vh;text-align:center;}"
                    + ".card{background:#ffffff;border-radius:18px;padding:32px 20px;max-width:380px;box-shadow:0 10px 25px rgba(0,0,0,0.06);border:1px solid #e2e8f0;}"
                    + ".badge{display:inline-block;padding:5px 14px;border-radius:20px;background:#e0f2fe;color:#0284c7;font-size:12px;font-weight:700;margin-bottom:14px;}"
                    + "h1{color:#1e3a8a;font-size:22px;margin:0 0 10px;font-weight:800;}"
                    + "p{color:#64748b;font-size:14px;line-height:1.5;margin:0 0 24px;}"
                    + ".btn{display:inline-block;width:100%;padding:14px 18px;background:linear-gradient(135deg,#2563eb,#1d4ed8);color:#ffffff;border:none;border-radius:12px;font-size:15px;font-weight:700;cursor:pointer;box-shadow:0 4px 12px rgba(37,99,235,0.3);}"
                    + ".btn:active{transform:scale(0.98);}"
                    + "</style></head><body>"
                    + "<div class='card'>"
                    + "<span class='badge'>SNTT App Build</span>"
                    + "<h1>স্বাগতম</h1>"
                    + "<p>আপনার অ্যাপ্লিকেশনের ডাটাবেজ এবং ইউআই লোড করতে উপরের ডাউনলোড আইকন অথবা নিচের বাটনে চাপ দিন।</p>"
                    + "<button class='btn' onclick='if(window.Android) window.Android.startDownload();'>📥 ডাউনলোড শুরু করুন</button>"
                    + "</div></body></html>";
            webView.loadDataWithBaseURL("https://sntt.local/", welcomeHtml, "text/html", "UTF-8", null);
        }
    }

    private void openStoredTool(String tinyDbTag) {
        if (jsBridge != null) jsBridge.resetModalState();
        String toolHtml = tinyDB.getString(tinyDbTag, "");
        if (toolHtml.isEmpty()) {
            Toast.makeText(this, "Tool not downloaded yet. Please use the download button first.", Toast.LENGTH_LONG).show();
            return;
        }
        isHomeView = false;
        webView.loadDataWithBaseURL("https://sntt.local/", toolHtml, "text/html", "UTF-8", null);
    }

    private void handleShipSelection(String shipJson) {
        try {
            JSONObject obj = new JSONObject(shipJson);
            String shipName = obj.optString("Ship Name", "").trim();
            if ("SNTT All Tank".equalsIgnoreCase(shipName)) {
                openStoredTool(TAG_ALL_TANK_CALIBRATION);
                return;
            }
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

                String cleanUrl = rawUrl.trim().replaceAll("\\s+", "");
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

                loadHomeUI();

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
                sb.append(line).append((char) 10);
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
        if (webView == null) {
            handleStandardBack();
            return;
        }

        webView.evaluateJavascript(
            "(function() {"
            + "  if (typeof window.closeAnyModal === 'function') { return window.closeAnyModal(); }"
            + "  var sc = document.getElementById('screenshotModalOverlay') || document.querySelector('.screenshot-modal-overlay');"
            + "  if (sc) {"
            + "    var r = sc.getBoundingClientRect();"
            + "    var st = window.getComputedStyle(sc);"
            + "    if (r.width > 0 && r.height > 0 && st.display !== 'none' && st.visibility !== 'hidden') {"
            + "      if (typeof window.closeScreenshotModal === 'function') { try { window.closeScreenshotModal(); } catch(e) {} }"
            + "      if (sc.parentNode) { sc.parentNode.removeChild(sc); } else { sc.style.display = 'none'; }"
            + "      return true;"
            + "    }"
            + "  }"
            + "  return false;"
            + "})();",
            value -> {
                if ("true".equals(value)) {
                    if (jsBridge != null) jsBridge.resetModalState();
                    return;
                }
                if (jsBridge != null) jsBridge.resetModalState();
                handleStandardBack();
            }
        );
    }

    private void dismissActiveModal() {
        if (jsBridge != null) {
            jsBridge.resetModalState();
        }
        if (webView != null) {
            webView.evaluateJavascript(
                "(function() {"
                + "  if (typeof window.closeAnyModal === 'function') { return window.closeAnyModal(); }"
                + "  var sc = document.getElementById('screenshotModalOverlay') || document.querySelector('.screenshot-modal-overlay');"
                + "  if (sc) {"
                + "    if (typeof window.closeScreenshotModal === 'function') { try { window.closeScreenshotModal(); } catch(e) {} }"
                + "    if (sc.parentNode) { sc.parentNode.removeChild(sc); } else { sc.style.display = 'none'; }"
                + "    return true;"
                + "  }"
                + "  return false;"
                + "})();", null
            );
        }
    }

    private void handleStandardBack() {
        if (!isHomeView) {
            loadHomeUI();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
