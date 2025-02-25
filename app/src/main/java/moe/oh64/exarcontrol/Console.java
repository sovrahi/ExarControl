package moe.oh64.exarcontrol;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class Console extends AppCompatActivity {

    private static final String API_URL = "https://api.exaroton.com/v1/servers/";
    private String serverId;
    private String token;

    private WebSocket webSocket;
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private TextView consoleOutput;
    private EditText commandInput;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.console);

        consoleOutput = findViewById(R.id.console);
        commandInput = findViewById(R.id.text_input);
        Button sendButton = findViewById(R.id.send);
        Button backButton = findViewById(R.id.back);
        scrollView = findViewById(R.id.scroll_view);



        // Retrieve server ID and token
        serverId = getIntent().getStringExtra("server_id");
        SharedPreferences sharedPreferences = getSharedPreferences("ExarotonPrefs", MODE_PRIVATE);
        token = sharedPreferences.getString("token", "");

        // Connect to WebSocket
        connectWebSocket();

        // Send command via the "Send" button
        sendButton.setOnClickListener(v -> sendCommand());

        // Return to the previous menu
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.close(1000, "Console closed by the user.");
        }
        executorService.shutdown();
    }

    private void connectWebSocket() {
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/websocket")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull okhttp3.Response response) {
                // Subscribe to the console stream
                String subscribeConsole = "{\"stream\":\"console\",\"type\":\"start\",\"data\":{\"tail\":50}}";
                webSocket.send(subscribeConsole);
                runOnUiThread(() -> consoleOutput.append("Connected to console\n"));
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    if ("console".equals(json.optString("stream")) && "line".equals(json.optString("type"))) {
                        // Retrieve the data line
                        String line = json.getString("data");

                        // Clean the line to remove ANSI codes and command prefixes
                        String cleanedLine = removeAnsiCodes(line);
                        cleanedLine = removeCommandPrefix(cleanedLine);

                        // Display the cleaned line in the console
                        final String finalLine = cleanedLine;

                        runOnUiThread(() -> {
                            consoleOutput.append(finalLine + "\n");
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        });
                    }
                } catch (Exception e) {
                    Log.e("Console", "Error processing WebSocket message", e);
                }
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, okhttp3.Response response) {
                Log.e("Console", "WebSocket connection error", t);
                runOnUiThread(() -> consoleOutput.append("Connection error: " + t.getMessage() + "\n"));
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                runOnUiThread(() -> consoleOutput.append("Disconnected from console\n"));
            }
        });
    }

    private void sendCommand() {
        String command = commandInput.getText().toString().trim();
        if (!command.isEmpty()) {
            String commandJson = "{\"stream\":\"console\",\"type\":\"command\",\"data\":\"" + command + "\"}";
            webSocket.send(commandJson);
            consoleOutput.append("> " + command + "\n");
            commandInput.setText("");
        } else {
            Toast.makeText(this, "Please enter a command.", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to remove ANSI codes
    private String removeAnsiCodes(String text) {
        // Regular expression to remove ANSI codes
        String regex = "\u001B\\[[;\\d]*m";
        return text.replaceAll(regex, "");
    }

    // Method to remove command prefixes
    private String removeCommandPrefix(String text) {
        // Remove prefixes ">...."
        return text.replaceAll("^>....", "");
    }
}
