package moe.oh64.exarcontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private static final String API_URL = "https://api.exaroton.com/v1/servers/";
    private String serverId;
    private String token;

    private TextView tvServerAddress, tvServerStatus, tvServerRam, tvRamPercentage, tvMOTD;
    private Button btnStartStop, btnReboot, btnServerList, btnLogs, btnPlayerList, btnCreditPool, btnconsole, btnoption;
    private ImageView ivServerIcon;

    private WebSocket webSocket;
    private final OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler();
    private final Runnable statusUpdaterRunnable = this::fetchServerStatus;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

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
        btnServerList = findViewById(R.id.btn_server_list);
        btnPlayerList = findViewById(R.id.playerlist);
        btnCreditPool = findViewById(R.id.creditpool);
        btnLogs = findViewById(R.id.btn_logs);
        btnconsole = findViewById(R.id.btn_console);
        btnoption = findViewById(R.id.option);
        ivServerIcon = findViewById(R.id.server_icon);

        // Retrieve server ID and token
        serverId = getIntent().getStringExtra("server_id");
        SharedPreferences sharedPreferences = getSharedPreferences("ExarotonPrefs", MODE_PRIVATE);
        token = sharedPreferences.getString("token", "");

        // Fetch server info
        fetchServerInfo();

        // Connect WebSocket
        connectWebSocket();

        // Update status and RAM every 5 seconds
        handler.post(statusUpdaterRunnable);

        // Set up button click listeners
        btnStartStop.setOnClickListener(v -> handleStartStop());
        btnReboot.setOnClickListener(v -> handleReboot());
        btnServerList.setOnClickListener(v -> goToServerList());
        btnLogs.setOnClickListener(v -> fetchServerLogs());
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
        executorService.shutdown();
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
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainManager.this, "Failed to fetch server info", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleServerInfoResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, "Error fetching server info", Toast.LENGTH_SHORT).show());
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

            // Fetch MOTD
            fetchServerMOTD();

            // Fetch RAM and CPU info
            fetchServerRamAndCpu();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchServerStatus() {
        Request request = new Request.Builder()
                .url(API_URL + serverId)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainManager.this, "Failed to fetch server status", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleServerStatusResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, "Error fetching server status", Toast.LENGTH_SHORT).show());
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
            e.printStackTrace();
        }
    }

    private void fetchServerMOTD() {
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/options/motd/")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainManager.this, "Failed to fetch MOTD", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleServerMOTDResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, "Error fetching MOTD", Toast.LENGTH_SHORT).show());
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
            e.printStackTrace();
        }
    }

    private void fetchServerRamAndCpu() {
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/options/ram/")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainManager.this, "Failed to fetch RAM and CPU info", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleServerRamAndCpuResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, "Error fetching RAM and CPU info", Toast.LENGTH_SHORT).show());
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
            tvServerRam.setText("RAM: " + ram + " GB\nCPU: " + cpu + " Shared Cores");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectWebSocket() {
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/websocket")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                // Subscribe to stats stream
                String subscribeStats = "{\"stream\":\"stats\",\"type\":\"start\"}";
                webSocket.send(subscribeStats);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    if ("stats".equals(json.optString("stream"))) {
                        JSONObject data = json.getJSONObject("data");
                        JSONObject memory = data.getJSONObject("memory");
                        double ramPercentage = memory.getDouble("percent");
                        runOnUiThread(() -> tvRamPercentage.setText("RAM Usage: " + ramPercentage + "%"));
                    } else if ("status".equals(json.optString("stream"))) {
                        JSONObject data = json.getJSONObject("data");
                        int status = data.getInt("status");
                        runOnUiThread(() -> updateServerStatus(status));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
            tvRamPercentage.setText("Server OFFLINE");
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
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Avoid error toast loop
                //runOnUiThread(() -> Toast.makeText(MainManager.this, "Failed to fetch server status and RAM", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleServerStatusAndRamResponse(responseData));
                } else {
                    // Avoid error toast loop
                    //runOnUiThread(() -> Toast.makeText(MainManager.this, "Error fetching server status and RAM", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

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
            tvRamPercentage.setText("RAM Usage: " + ramPercentage + "%");

        } catch (Exception e) {
            e.printStackTrace();
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
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainManager.this, action + " failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, action + " completed", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, action + " failed", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void fetchServerLogs() {
        runOnUiThread(() -> Toast.makeText(MainManager.this, "Fetching logs...", Toast.LENGTH_SHORT).show());
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/logs/")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainManager.this, "Failed to fetch server logs", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> uploadLogsToMclogs(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, "Error fetching server logs", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void uploadLogsToMclogs(String logs) {
        runOnUiThread(() -> Toast.makeText(MainManager.this, "Uploading logs...", Toast.LENGTH_SHORT).show());
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), logs);
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/logs/share/")
                .addHeader("Authorization", "Bearer " + token)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainManager.this, "Failed to upload logs", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleLogsUploadResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainManager.this, "Error uploading logs", Toast.LENGTH_SHORT).show());
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
            e.printStackTrace();
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
