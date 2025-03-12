package moe.oh64.exarcontrol;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainManager extends AppCompatActivity {

    private final String API_URL = getResources().getString(R.string.api_url) + "servers/";
    private String serverId;
    private String token;

    private TextView tvServerAddress, tvServerStatus, tvServerRam, tvRamPercentage, tvMOTD;
    private Button btnStartStop, btnReboot;

    private WebSocket webSocket;
    private final OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler();
    private final Runnable statusUpdaterRunnable = this::fetchServerStatus;
    private ScheduledExecutorService scheduler;

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainmanager);

        // Initialize UI components
        tvServerAddress = findViewById(R.id.tv_server_address);
        tvServerStatus = findViewById(R.id.tv_server_status);
        tvServerRam = findViewById(R.id.tv_server_ram);
        tvRamPercentage = findViewById(R.id.ram_percentage);
        tvMOTD = findViewById(R.id.motd);
        btnStartStop = findViewById(R.id.btn_start_stop);
        btnReboot = findViewById(R.id.btn_reboot);
        Button btnServerList = findViewById(R.id.btn_server_list);
        Button btnPlayerList = findViewById(R.id.playerlist);
        Button btnCreditPool = findViewById(R.id.creditpool);
        Button btnLogs = findViewById(R.id.btn_logs);
        Button btnconsole = findViewById(R.id.btn_console);
        Button btnoption = findViewById(R.id.option);
        findViewById(R.id.server_icon);

        // Retrieve server ID and token
        serverId = getIntent().getStringExtra("server_id");
        SharedPreferences sharedPreferences = getSharedPreferences("ExarotonPrefs", MODE_PRIVATE);
        token = sharedPreferences.getString("token", "");

        if ("OwO".equals(token)) {
            scheduler = Executors.newScheduledThreadPool(1);
            tvServerAddress.setText((serverId != null ? serverId : "Unknown") + ".exaroton.me");
            tvMOTD.setText("THAT'S THE BEST SERVER");
            tvServerRam.setText(getString(R.string.server_ram_cpu, (int) (Math.random() * 5), (int) (Math.random() * 2)));
            tvRamPercentage.setText(String.format("RAM Usage %.1f%%", Math.random() * 100));

            scheduler.scheduleWithFixedDelay(() -> {
                int randomValue = (int) (Math.random() * 11) % 11;
                runOnUiThread(() -> {
                    updateServerStatus(randomValue);
                    tvRamPercentage.setText(String.format("RAM Usage %.1f%%", Math.random() * 100));
                });
            }, 0, 1, TimeUnit.SECONDS);
        } else {
            // Fetch server info
            fetchServerInfo();

            // Connect WebSocket
            connectWebSocket();

            // Update status and RAM every 5 seconds
            handler.post(statusUpdaterRunnable);

            btnStartStop.setOnClickListener(v -> handleStartStop());
            btnReboot.setOnClickListener(v -> handleReboot());
            btnLogs.setOnClickListener(v -> fetchServerLogs());
        }

        // Set up button click listeners
        btnServerList.setOnClickListener(v -> goToServerList());
        btnPlayerList.setOnClickListener(v -> goToPlayerList());
        btnCreditPool.setOnClickListener(v -> goToCreditPool());
        btnconsole.setOnClickListener(v -> goToConsole());
        btnoption.setOnClickListener(v -> goToOption());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(statusUpdaterRunnable);
        closeWebSockets();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private void goToServerList() {
        finish();
    }

    private void closeWebSockets() {
        if (webSocket != null) {
            webSocket.close(1000, "Closed by the user.");
        }
    }

    private void fetchServerInfo() {
        Request request = new Request.Builder()
                .url(API_URL + serverId)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainManager", "Failed to fetch server info", e);
                runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_fetching_server_info, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleServerInfoResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_fetching_server_info, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void handleServerInfoResponse(String result) {
        try {
            JSONObject jsonResponse = new JSONObject(result);
            JSONObject serverData = jsonResponse.getJSONObject("data");

            tvServerAddress.setText(serverData.getString("address"));

            // Get server status
            int status = serverData.getInt("status");
            updateServerStatus(status);

            // Fetch MOTD and RAM/CPU info
            fetchServerMOTD();
            fetchServerRamAndCpu();

        } catch (Exception e) {
            Log.e("MainManager", "Error parsing server info response", e);
        }
    }

    private void fetchServerStatus() {
        Request request = new Request.Builder()
                .url(API_URL + serverId)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainManager", "Failed to fetch server status", e);
                runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_fetching_server_status, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleServerStatusResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_fetching_server_status, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void handleServerStatusResponse(String result) {
        try {
            JSONObject jsonResponse = new JSONObject(result);
            JSONObject serverData = jsonResponse.getJSONObject("data");

            // Get server status
            int status = serverData.getInt("status");
            updateServerStatus(status);

        } catch (Exception e) {
            Log.e("MainManager", "Error parsing server status response", e);
        }
    }

    private void fetchServerMOTD() {
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/options/motd/")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainManager", "Failed to fetch MOTD", e);
                runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_fetching_motd, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleServerMOTDResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_fetching_motd, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void handleServerMOTDResponse(String result) {
        try {
            JSONObject jsonResponse = new JSONObject(result);
            String motd = jsonResponse.getJSONObject("data").getString("motd");
            tvMOTD.setText(motd);
        } catch (Exception e) {
            Log.e("MainManager", "Error parsing MOTD response", e);
        }
    }

    private void fetchServerRamAndCpu() {
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/options/ram/")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainManager", "Failed to fetch RAM and CPU info", e);
                runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_fetching_ram_cpu, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleServerRamAndCpuResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_fetching_ram_cpu, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void handleServerRamAndCpuResponse(String result) {
        try {
            JSONObject jsonResponse = new JSONObject(result);
            JSONObject ramData = jsonResponse.getJSONObject("data");

            // Get RAM and calculate CPU
            int ram = ramData.getInt("ram");
            int cpu = ram / 2;
            tvServerRam.setText(getString(R.string.server_ram_cpu, ram, cpu));

        } catch (Exception e) {
            Log.e("MainManager", "Error parsing RAM and CPU response", e);
        }
    }

    private void connectWebSocket() {
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/websocket")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                // Subscribe to stats stream
                String subscribeStats = "{\"stream\":\"stats\",\"type\":\"start\"}";
                webSocket.send(subscribeStats);
            }

            @SuppressLint("StringFormatMatches")
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    if ("stats".equals(json.optString("stream"))) {
                        JSONObject data = json.getJSONObject("data");
                        JSONObject memory = data.getJSONObject("memory");
                        double ramPercentage = memory.getDouble("percent");
                        runOnUiThread(() -> tvRamPercentage.setText(getString(R.string.ram_usage, ramPercentage)));
                    } else if ("status".equals(json.optString("stream"))) {
                        JSONObject data = json.getJSONObject("data");
                        int status = data.getInt("status");
                        runOnUiThread(() -> updateServerStatus(status));
                    }
                } catch (Exception e) {
                    Log.e("MainManager", "Error processing WebSocket message", e);
                }
            }
        });
    }

    private void updateServerStatus(int status) {
        String[] statusMessages = {
                "OFFLINE", "ONLINE", "STARTING", "STOPPING", "RESTARTING",
                "SAVING", "LOADING", "CRASHED", "PENDING",
                "TRANSFERRING", "PREPARING"
        };
        tvServerStatus.setText(statusMessages[status]);
        btnStartStop.setText(status == 0 ? "Start" : "Stop");
        btnReboot.setVisibility(status == 1 ? View.VISIBLE : View.GONE);

        if (status == 0) {
            tvRamPercentage.setText(R.string.server_offline);
        } else {
            // Force RAM usage update on startup
            fetchServerStatusAndRam();
        }
    }

    private void fetchServerStatusAndRam() {
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/stats/")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainManager", "Failed to fetch server status and RAM", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleServerStatusAndRamResponse(responseData));
                }
            }
        });
    }

    @SuppressLint("StringFormatMatches")
    private void handleServerStatusAndRamResponse(String result) {
        try {
            JSONObject jsonResponse = new JSONObject(result);
            JSONObject serverData = jsonResponse.getJSONObject("data");

            // Get server status
            int status = serverData.getInt("status");
            updateServerStatus(status);

            // Get RAM info
            JSONObject memory = serverData.getJSONObject("memory");
            double ramPercentage = memory.getDouble("percent");
            tvRamPercentage.setText(getString(R.string.ram_usage, ramPercentage));

        } catch (Exception e) {
            Log.e("MainManager", "Error parsing server status and RAM response", e);
        }
    }

    private void handleStartStop() {
        String action = tvServerStatus.getText().equals("OFFLINE") ? "start" : "stop";
        performServerAction(action);
    }

    private void handleReboot() {
        performServerAction("restart");
    }

    private void performServerAction(String action) {
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/" + action + "/")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainManager", action + " failed", e);
                runOnUiThread(() -> Toast.makeText(MainManager.this, getString(R.string.action_failed, action), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, getString(R.string.action_completed, action), Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, getString(R.string.action_failed, action), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void fetchServerLogs() {
        runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.fetching_logs, Toast.LENGTH_SHORT).show());
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/logs/")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainManager", "Failed to fetch server logs", e);
                runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_fetching_logs, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> uploadLogsToMclogs(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_fetching_logs, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void uploadLogsToMclogs(String logs) {
        runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.uploading_logs, Toast.LENGTH_SHORT).show());
        RequestBody requestBody = RequestBody.create(logs, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/logs/share/")
                .addHeader("Authorization", "Bearer " + token)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainManager", "Failed to upload logs", e);
                runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_uploading_logs, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleLogsUploadResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, R.string.error_uploading_logs, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void handleLogsUploadResponse(String result) {
        try {
            JSONObject jsonResponse = new JSONObject(result);
            JSONObject data = jsonResponse.getJSONObject("data");
            String url = data.getString("url");

            // Open the link in a browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            Log.e("MainManager", "Error parsing logs upload response", e);
        }
    }

    private void goToPlayerList() {
        Intent intent = new Intent(MainManager.this, PlayerList.class);
        intent.putExtra("server_id", serverId);
        startActivity(intent);
    }

    private void goToCreditPool() {
        Intent intent = new Intent(MainManager.this, CreditPool.class);
        intent.putExtra("server_id", serverId);
        startActivity(intent);
    }

    private void goToConsole() {
        Intent intent = new Intent(MainManager.this, Console.class);
        intent.putExtra("server_id", serverId);
        startActivity(intent);
    }

    private void goToOption() {
        Intent intent = new Intent(MainManager.this, Option.class);
        intent.putExtra("server_id", serverId);
        startActivity(intent);
    }
}