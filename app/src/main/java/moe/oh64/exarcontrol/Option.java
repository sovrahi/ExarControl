package moe.oh64.exarcontrol;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
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

public class Option extends AppCompatActivity {

    private EditText editTextMOTD, editTextRAM;

    private String serverId;
    private String token;

    private final String API_URL = getResources().getString(R.string.api_url) + "servers/";
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.option);

        // Initialize UI elements
        editTextMOTD = findViewById(R.id.editTextMOTD);
        editTextRAM = findViewById(R.id.editTextRAM);
        Button btnSaveMOTD = findViewById(R.id.btn_save_motd);
        Button btnSaveRAM = findViewById(R.id.btn_save_ram);
        Button backButton = findViewById(R.id.back);

        // Retrieve server ID and token
        serverId = getIntent().getStringExtra("server_id");
        SharedPreferences sharedPreferences = getSharedPreferences("ExarotonPrefs", MODE_PRIVATE);
        token = sharedPreferences.getString("token", "");

        // Load current server settings
        fetchCurrentSettings();

        // Save MOTD button click listener
        btnSaveMOTD.setOnClickListener(v -> saveSettings("motd"));

        // Save RAM button click listener
        btnSaveRAM.setOnClickListener(v -> saveSettings("ram"));

        // Return to the previous menu
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void fetchCurrentSettings() {
        // Fetch MOTD
        fetchSetting("motd", API_URL + serverId + "/options/motd/");

        // Fetch RAM
        fetchSetting("ram", API_URL + serverId + "/options/ram/");
    }

    private void fetchSetting(String type, String urlString) {
        Request request = new Request.Builder()
                .url(urlString)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("FetchSetting", "Failed to fetch " + type, e);
                runOnUiThread(() -> Log.e("FetchSetting", "Failed to fetch " + type));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleSettingResponse(type, responseData));
                } else {
                    runOnUiThread(() -> Log.e("FetchSetting", "Error fetching " + type));
                }
            }
        });
    }

    private void handleSettingResponse(String type, String result) {
        try {
            JSONObject jsonResponse = new JSONObject(result);
            JSONObject data = jsonResponse.getJSONObject("data");
            if (type.equals("motd")) {
                String motd = data.getString("motd");
                editTextMOTD.setText(motd);
            } else if (type.equals("ram")) {
                int ram = data.getInt("ram");
                editTextRAM.setText(String.valueOf(ram));
            }
        } catch (Exception e) {
            Log.e("HandleSetting", "Error parsing " + type + " response", e);
        }
    }

    private void saveSettings(String type) {
        if (type.equals("motd")) {
            String motd = editTextMOTD.getText().toString();
            saveSetting("motd", motd);
        } else if (type.equals("ram")) {
            String ram = editTextRAM.getText().toString();
            saveSetting("ram", ram);
        }
    }

    private void saveSetting(String type, String value) {
        String urlString = API_URL + serverId + "/options/" + type + "/";
        String jsonBody = type.equals("motd") ? "{\"" + type + "\":\"" + value + "\"}" : "{\"" + type + "\":" + value + "}";

        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(urlString)
                .addHeader("Authorization", "Bearer " + token)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SaveSetting", "Failed to save " + type, e);
                runOnUiThread(() -> Log.e("SaveSetting", "Failed to save " + type));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Log.i("SaveSetting", type + " saved successfully"));
                } else {
                    runOnUiThread(() -> Log.e("SaveSetting", "Error saving " + type));
                }
            }
        });
    }
}
