package moe.oh64.exarcontrol;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CreditPool extends AppCompatActivity {

    private static final String API_URL = "https://api.exaroton.com/v1/billing/pools/";
    private String token;
    private TextView tvCreditPoolInfo;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.credit_pool);

        tvCreditPoolInfo = findViewById(R.id.tv_credit_pool_info);

        // Retrieve the token from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("ExarotonPrefs", MODE_PRIVATE);
        token = sharedPreferences.getString("token", "");

        // Fetch credit pool information
        fetchCreditPools();

        // Set up click listener for the back button
        Button backButton = findViewById(R.id.back);
        backButton.setOnClickListener(v -> finish());
    }

    private void fetchCreditPools() {
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> tvCreditPoolInfo.setText("Failed to fetch credit pools"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> handleCreditPoolsResponse(responseData));
                } else {
                    runOnUiThread(() -> tvCreditPoolInfo.setText("Error fetching credit pools"));
                }
            }
        });
    }

    private void handleCreditPoolsResponse(String result) {
        try {
            JSONObject jsonResponse = new JSONObject(result);
            JSONArray pools = jsonResponse.getJSONArray("data");

            if (pools.length() == 0) {
                tvCreditPoolInfo.setText("There is no Credit Pool");
            } else {
                StringBuilder poolInfo = new StringBuilder();
                for (int i = 0; i < pools.length(); i++) {
                    JSONObject pool = pools.getJSONObject(i);
                    poolInfo.append(pool.getString("name"))
                            .append("\n\nCredits: ").append(pool.getInt("credits"))
                            .append("\nServers: ").append(pool.getInt("servers"))
                            .append("\nMembers: ").append(pool.getInt("members"))
                            .append("\n\n");
                }
                tvCreditPoolInfo.setText(poolInfo.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            tvCreditPoolInfo.setText("Error parsing credit pools data");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
