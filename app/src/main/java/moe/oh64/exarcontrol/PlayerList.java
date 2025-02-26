package moe.oh64.exarcontrol;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PlayerList extends AppCompatActivity {

    private static final String API_URL = "https://api.exaroton.com/v1/servers/";
    private String serverId;
    private String token;

    private Spinner listSelector;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> players = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playerlist);

        ListView listView = findViewById(R.id.listViewPlayers);
        listSelector = findViewById(R.id.spinnerListSelector);
        Button backButton = findViewById(R.id.Back);

        // Retrieve server ID and token
        serverId = getIntent().getStringExtra("server_id");
        SharedPreferences sharedPreferences = getSharedPreferences("ExarotonPrefs", MODE_PRIVATE);
        token = sharedPreferences.getString("token", "");

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, players);
        listView.setAdapter(adapter);

        // Fetch available player lists
        fetchAvailableLists();

        // Set up item selected listener for the spinner
        listSelector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedList = listSelector.getSelectedItem().toString();
                fetchPlayerList(selectedList);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Set up item click listener for the list view
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String playerName = players.get(position);
            showPlayerActionsDialog(playerName);
        });

        // Set up click listener for the back button
        backButton.setOnClickListener(v -> finish());
    }

    private void showPlayerActionsDialog(String playerName) {
        String[] actions = {"Add", "Remove", "Cancel"};
        String selectedList = listSelector.getSelectedItem().toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Actions for " + playerName);
        builder.setItems(actions, (dialog, which) -> {
            if (which == 0) {
                showAddToListDialog(playerName);
            } else if (which == 1) {
                modifyPlayerList("DELETE", selectedList, playerName);
            }
        });
        builder.show();
    }

    private void showAddToListDialog(String playerName) {
        ArrayList<String> availableLists = new ArrayList<>();
        for (int i = 0; i < listSelector.getAdapter().getCount(); i++) {
            String list = (String) listSelector.getAdapter().getItem(i);
            if (!"Online Players".equals(list)) {
                availableLists.add(list);
            }
        }

        String[] listOptions = availableLists.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add " + playerName + " to list:");
        builder.setItems(listOptions, (dialog, which) -> {
            String targetList = listOptions[which];
            modifyPlayerList("PUT", targetList, playerName);
        });
        builder.show();
    }

    private void fetchAvailableLists() {
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/playerlists/")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("PlayerList", "Failed to fetch lists", e);
                runOnUiThread(() -> Toast.makeText(PlayerList.this, "Failed to fetch lists", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleAvailableListsResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(PlayerList.this, "Error fetching lists", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void handleAvailableListsResponse(String result) {
        try {
            JSONObject jsonResponse = new JSONObject(result);
            if (jsonResponse.getBoolean("success")) {
                JSONArray data = jsonResponse.getJSONArray("data");
                ArrayList<String> availableLists = new ArrayList<>();

                for (int i = 0; i < data.length(); i++) {
                    availableLists.add(data.getString(i));
                }

                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(PlayerList.this,
                        android.R.layout.simple_spinner_item, availableLists);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                listSelector.setAdapter(spinnerAdapter);
            } else {
                Toast.makeText(PlayerList.this, "Error retrieving lists", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("PlayerList", "JSON error retrieving lists", e);
            Toast.makeText(PlayerList.this, "JSON error retrieving lists", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchPlayerList(String listName) {
        String listEndpoint = listName.equals("Online Players") ? "players" : "playerlists/" + listName;
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/" + listEndpoint + "/")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("PlayerList", "Failed to fetch players", e);
                runOnUiThread(() -> Toast.makeText(PlayerList.this, "Failed to fetch players", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handlePlayerListResponse(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(PlayerList.this, "Error fetching players", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void handlePlayerListResponse(String result) {
        try {
            JSONObject jsonResponse = new JSONObject(result);
            if (jsonResponse.getBoolean("success")) {
                JSONArray data = jsonResponse.getJSONArray("data");
                players.clear();
                for (int i = 0; i < data.length(); i++) {
                    players.add(data.getString(i));
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(PlayerList.this, "Error retrieving players", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("PlayerList", "JSON error retrieving players", e);
            Toast.makeText(PlayerList.this, "JSON error retrieving players", Toast.LENGTH_SHORT).show();
        }
    }

    private void modifyPlayerList(String method, String listName, String playerName) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("entries", new JSONArray().put(playerName));
        } catch (Exception e) {
            Log.e("PlayerList", "Error creating payload", e);
        }

        RequestBody requestBody = RequestBody.create(payload.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/playerlists/" + listName + "/")
                .method(method, requestBody)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("PlayerList", "Failed to modify player list", e);
                runOnUiThread(() -> Toast.makeText(PlayerList.this, "Failed to modify player list", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(PlayerList.this, "Operation successful!", Toast.LENGTH_SHORT).show();
                        fetchPlayerList(listName);
                    });
                } else {
                    String errorMessage = "Error modifying player list";
                    try {
                        assert response.body() != null;
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        errorMessage = jsonResponse.optString("error", errorMessage);
                    } catch (Exception e) {
                        Log.e("PlayerList", "Error parsing error response", e);
                    }
                    String finalErrorMessage = errorMessage;
                    runOnUiThread(() -> Toast.makeText(PlayerList.this, "Error: " + finalErrorMessage, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}