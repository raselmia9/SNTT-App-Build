package com.sntt.appbuild;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    private TinyDB tinyDB;
    private List<DownloadItem> downloadItemList;
    private DownloadAdapter adapter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ProgressBar progressBar;
    private TextView tvOverallStatus, tvCountSuccess, tvCountFailed, tvCountSkipped;
    private Button btnStartDownload;
    private RecyclerView recyclerView;
    private FrameLayout webViewContainer;
    private WebView webView;

    private boolean isDownloading = false;
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
        setupDataList();
        setupWebView();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        tvOverallStatus = findViewById(R.id.tvOverallStatus);
        tvCountSuccess = findViewById(R.id.tvCountSuccess);
        tvCountFailed = findViewById(R.id.tvCountFailed);
        tvCountSkipped = findViewById(R.id.tvCountSkipped);
        btnStartDownload = findViewById(R.id.btnStartDownload);
        recyclerView = findViewById(R.id.recyclerView);
        webViewContainer = findViewById(R.id.webViewContainer);
        webView = findViewById(R.id.webView);

        btnStartDownload.setOnClickListener(v -> startDownloadAll());
    }

    private void setupDataList() {
        downloadItemList = new ArrayList<>();
        downloadItemList.add(new DownloadItem("SNTT All Tank", "https://drive.usercontent.google.com/download?id=1tC4GRaIBjzhqBl9Ry7UEww4dm0d3iEXl&export=download"));
        downloadItemList.add(new DownloadItem("SGTT All Tank", "None"));
        downloadItemList.add(new DownloadItem("BAT 01", "https://drive.usercontent.google.com/download?id=1QzFs2cm2h7MRl1Up9aopP-Zc80Jh4rID&export=download"));
        downloadItemList.add(new DownloadItem("BAT 02", "https://drive.usercontent.google.com/download?id=1qcPEZtSlXuyRxL5-d6VptbIHA2HrXHHv&export=download"));
        downloadItemList.add(new DownloadItem("BAT 03", "https://drive.usercontent.google.com/download?id=18E1cdvNdCVfaPNF4CjfMZXlfK9bDH6ke&export=download"));
        downloadItemList.add(new DownloadItem("Binzaman 01", "https://drive.usercontent.google.com/download?id=1q08_ENZYdDib20Hgh_zDU9kUeekmiff1&export=download"));
        downloadItemList.add(new DownloadItem("Binzaman 02", "https://drive.usercontent.google.com/download?id=1WlQxR_z2x92Z0VxMO7wIulxdxyrGmip4&export=download"));
        downloadItemList.add(new DownloadItem("Binzaman 03", "https://drive.usercontent.google.com/download?id=1XqSkpBntZMQRA8A3u48257M3qQGQRKGb&export=download"));
        downloadItemList.add(new DownloadItem("Binzaman 04", "https://drive.usercontent.google.com/download?id=1_HDEyU8mBLdbIVLJFXjK67Rc6I2Xlkhy&export=download"));
        downloadItemList.add(new DownloadItem("Mesra", "https://drive.usercontent.google.com/download?id=13vHg3LGq3p_WWYlwI1ppzIbYCcvpqVx2&export=download"));
        downloadItemList.add(new DownloadItem("Kingfisher 03", "https://drive.usercontent.google.com/download?id=1ePwDVkpswBPu-3lvkGyj0lOzZWPADWHn&export=download"));
        downloadItemList.add(new DownloadItem("Kingfisher 04", "https://drive.usercontent.google.com/download?id=1dCtgC1gNWNFyRUN0N9oXLSijdtKwfmHM&export=download"));
        downloadItemList.add(new DownloadItem("M.T Rukh", "https://drive.usercontent.google.com/download?id=1BKKmBYon3VFg04KKu2pSdLyCyicemnmd&export=download"));
        downloadItemList.add(new DownloadItem("Shanghai 07", "https://drive.usercontent.google.com/download?id=1N9tvzfOR3xJxh1-8_P6ITdZofaQkZvtv&export=download"));
        downloadItemList.add(new DownloadItem("O.T SHANGHAI-01", "https://drive.usercontent.google.com/download?id=1vzSKqSiwNlSuYJDygiBYWafF0tFne2WE&export=download"));
        downloadItemList.add(new DownloadItem("SNTT HOME UI.html", "https://raw.githubusercontent.com/raselmia9/SNTT-ALL-DATA/refs/heads/main/SNTT%20Home%20New.html"));

        // Check if already in TinyDB
        for (DownloadItem item : downloadItemList) {
            if (tinyDB.hasTag(item.getTinyDbName())) {
                item.setStatus(DownloadItem.Status.COMPLETED);
                item.setMessage("Already cached in TinyDB");
            }
        }

        progressBar.setMax(downloadItemList.size());
        adapter = new DownloadAdapter(this, downloadItemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setDatabaseEnabled(true);

        webView.addJavascriptInterface(new TinyDBJsInterface(tinyDB), "TinyDB");
        webView.setWebViewClient(new WebViewClient());
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
        } else if (item.getItemId() == R.id.action_toggle_view) {
            toggleWebView();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleWebView() {
        if (webViewContainer.getVisibility() == View.VISIBLE) {
            webViewContainer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            findViewById(R.id.cardProgress).setVisibility(View.VISIBLE);
        } else {
            // Load SNTT HOME UI.html from TinyDB if available
            String homeHtml = tinyDB.getString("SNTT HOME UI.html", "");
            if (!homeHtml.isEmpty()) {
                webView.loadDataWithBaseURL(null, homeHtml, "text/html", "UTF-8", null);
            } else {
                webView.loadUrl("https://raw.githubusercontent.com/raselmia9/SNTT-ALL-DATA/refs/heads/main/SNTT%20Home%20New.html");
            }
            webViewContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            findViewById(R.id.cardProgress).setVisibility(View.GONE);
        }
    }

    private void startDownloadAll() {
        if (isDownloading) {
            Toast.makeText(this, "Download already in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        isDownloading = true;
        btnStartDownload.setEnabled(false);
        successCount = 0;
        failedCount = 0;
        skippedCount = 0;
        progressBar.setProgress(0);
        updateCounters();

        executorService.execute(() -> {
            for (int i = 0; i < downloadItemList.size(); i++) {
                final int index = i;
                final DownloadItem item = downloadItemList.get(i);

                mainHandler.post(() -> {
                    item.setStatus(DownloadItem.Status.DOWNLOADING);
                    item.setMessage("Downloading...");
                    adapter.notifyItemChanged(index);
                    tvOverallStatus.setText(String.format("Downloading (%d/%d): %s", index + 1, downloadItemList.size(), item.getTinyDbName()));
                });

                String rawUrl = item.getFileLink();
                if (rawUrl == null || rawUrl.trim().equalsIgnoreCase("none") || rawUrl.trim().isEmpty()) {
                    mainHandler.post(() -> {
                        item.setStatus(DownloadItem.Status.SKIPPED);
                        item.setMessage("No link available");
                        adapter.notifyItemChanged(index);
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
                        // Store into TinyDB under TinyDB Name
                        tinyDB.putString(item.getTinyDbName(), content);

                        mainHandler.post(() -> {
                            item.setStatus(DownloadItem.Status.COMPLETED);
                            item.setMessage("Saved (" + content.length() + " chars)");
                            adapter.notifyItemChanged(index);
                            successCount++;
                            progressBar.setProgress(index + 1);
                            updateCounters();
                        });
                    } else {
                        throw new Exception("Received empty content");
                    }
                } catch (Exception e) {
                    final String errorMsg = e.getMessage() != null ? e.getMessage() : "Download error";
                    mainHandler.post(() -> {
                        item.setStatus(DownloadItem.Status.FAILED);
                        item.setMessage(errorMsg);
                        adapter.notifyItemChanged(index);
                        failedCount++;
                        progressBar.setProgress(index + 1);
                        updateCounters();
                    });
                }

                try {
                    Thread.sleep(200); // slight delay for smooth UI progress
                } catch (InterruptedException ignored) {}
            }

            mainHandler.post(() -> {
                isDownloading = false;
                btnStartDownload.setEnabled(true);
                tvOverallStatus.setText(String.format("Finished! %d Saved, %d Failed, %d Skipped", successCount, failedCount, skippedCount));
                Toast.makeText(MainActivity.this, "Download completed!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private String downloadUrlContent(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)");
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();
        // Handle redirect if needed
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
                sb.append(line).append("\n");
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
}
