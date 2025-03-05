package moe.oh64.exarcontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class AllServers extends AppCompatActivity {

    private static final String API_URL = "https://api.exaroton.com/v1/servers/";
    private final Map<String, String> serverIdMap = new HashMap<>();
    private final ArrayList<String> serverNames = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final OkHttpClient client = new OkHttpClient();

    private Button Refresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.allservers);

        // Setup the ListView and Adapter
        ListView listView = findViewById(R.id.server_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(AllServers.this, R.layout.list_item, R.id.list_item_text, serverNames);
        listView.setAdapter(adapter);

        // Retrieve the token from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("ExarotonPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", "");

        Button tokenDel = findViewById(R.id.TokenDel);
        Refresh = findViewById(R.id.Refresh);

        // Fetch the list of servers
        fetchServers(token);

        tokenDel.setOnClickListener(v -> TokenDel());
        Refresh.setOnClickListener(v -> Refreshbtn());

        // Retrieve and display the app version
        TextView infoTextView = findViewById(R.id.Info);
        String versionOption = getString(R.string.Info) + " " + "Unknown";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            versionOption = getString(R.string.Info) + " " + versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("AllServers", "Error fetching package info", e);
        }
        infoTextView.setText(versionOption);

        // Add OnClickListener to the Info TextView
        infoTextView.setOnClickListener(v -> {
            Intent intent = new Intent(AllServers.this, Information.class);
            startActivity(intent);
        });
    }

    private void fetchServers(String token) {
        runOnUiThread(() -> Refresh.setEnabled(false));

        if ("OwO".equals(token)) {
            // Simulate response with fake servers
            String fakeResponse = createFakeServerResponse();
            runOnUiThread(() -> handleServerResponse(fakeResponse));
            runOnUiThread(() -> Refresh.setEnabled(false));
        } else {
            // Create a request to the API
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("AllServers", "Failed to fetch servers", e);
                    // Display a toast message on failure
                    runOnUiThread(() -> {
                        Refresh.setEnabled(true);
                        Toast.makeText(AllServers.this, "Failed to fetch servers", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Parse the response data
                        assert response.body() != null;
                        String responseData = response.body().string();
                        runOnUiThread(() -> handleServerResponse(responseData));
                    } else {
                        // Display a toast message on unauthorized or server error
                        runOnUiThread(() -> {
                            Refresh.setEnabled(true);
                            Toast.makeText(AllServers.this, "Unauthorized or server error", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }
    }

    private String createFakeServerResponse() {
        JSONObject jsonResponse = new JSONObject();
        try {
            jsonResponse.put("success", true);
            jsonResponse.put("error", JSONObject.NULL);
            JSONArray serversArray = new JSONArray();

            String[] Names = {
                    "Exemple", "IT'S DESIGNED FOR TESTING NOTHING REALLY WORKS. JUST WRITING TEXT FOR NOTHING TO KNOW IF IT RENDERS LIKE IT SHOULD OR IF IT CRASHES",
                    "Pizza", "Friendz", "Bruh", "ServerOfSomeone",
                    "GalaxyHub", "EchoChamber", "NexusPoint", "QuantumRealm", "PixelPals",
                    "MysticVoyage", "CyberNest", "StellarStream", "WhisperWind", "FusionFortress",
                    "InfinityLoop", "NovaNetwork", "ElysianFields", "ChronoCove", "AstralArena",
                    "VortexVista", "HarmonyHub", "ZenithZone", "LunarLounge", "PrismPeak"
            };
            for (String name : Names) {
                JSONObject server = new JSONObject();
                server.put("id", name);
                server.put("name", name);
                server.put("address", name.toLowerCase().replace(" ", "-") + ".exaroton.me");
                server.put("motd", "Welcome to the " + name + "!");
                server.put("status", 0);
                server.put("host", JSONObject.NULL);
                server.put("port", JSONObject.NULL);
                server.put("players", new JSONObject()
                        .put("max", 20)
                        .put("count", 0)
                        .put("list", new JSONArray()));
                server.put("software", new JSONObject()
                        .put("id", "kb4p09ABvLjxzedx")
                        .put("name", "Vanilla")
                        .put("version", "1.16.5"));
                server.put("shared", false);
                serversArray.put(server);
            }

            jsonResponse.put("data", serversArray);
        } catch (Exception e) {
            Log.e("AllServers", "Error creating fake server response", e);
        }
        return jsonResponse.toString();
    }

    private void handleServerResponse(String result) {
        // Clear the existing list before adding new items
        serverNames.clear();
        serverIdMap.clear();

        try {
            JSONObject jsonResponse = new JSONObject(result);
            // Check for success and handle unauthorized error
            if (!jsonResponse.optBoolean("success", false) || "Unauthorized".equals(jsonResponse.optString("error", ""))) {
                Intent intent = new Intent(AllServers.this, InvalidToken.class);
                startActivity(intent);
                finish();
                return;
            }

            // Parse the server data from the response
            JSONArray serversArray = jsonResponse.getJSONArray("data");
            for (int i = 0; i < serversArray.length(); i++) {
                JSONObject server = serversArray.getJSONObject(i);
                String serverName = server.getString("name");
                String serverId = server.getString("id");
                serverNames.add(serverName);
                serverIdMap.put(serverName, serverId);
            }
        } catch (Exception e) {
            Log.e("AllServers", "Error parsing server response", e);
        }

        runOnUiThread(() -> {
            Refresh.setEnabled(true);

            // Setup the ListView and Adapter
            ListView listView = findViewById(R.id.server_list);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(AllServers.this, android.R.layout.simple_list_item_1, serverNames);
            listView.setAdapter(adapter);

            // Handle item clicks on the ListView
            listView.setOnItemClickListener((AdapterView<?> parent, android.view.View view, int position, long id) -> {
                String selectedServerName = serverNames.get(position);
                if (!selectedServerName.isEmpty()) {
                    // Normal behavior for server items
                    String selectedServerId = serverIdMap.get(selectedServerName);
                    Intent intent = new Intent(AllServers.this, MainManager.class);
                    intent.putExtra("server_id", selectedServerId);
                    startActivity(intent);
                }
            });

            // Notify the adapter that data has changed to refresh the ListView
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown the executor service when the activity is destroyed
        executorService.shutdown();
    }

    private void Refreshbtn() {
        Toast.makeText(AllServers.this, "Refreshing server list...", Toast.LENGTH_SHORT).show();
        SharedPreferences sharedPreferences = getSharedPreferences("ExarotonPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("token", "");
        fetchServers(token);
        Toast.makeText(AllServers.this, "Done!", Toast.LENGTH_SHORT).show();
    }

    private void TokenDel() {
        Intent intent = new Intent(AllServers.this, InvalidToken.class);
        startActivity(intent);
    }
}