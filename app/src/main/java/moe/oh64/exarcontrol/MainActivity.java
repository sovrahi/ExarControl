package moe.oh64.exarcontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText editTextToken;
    private Button buttonGo;
    private static final String PREFS_NAME = "ExarotonPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Token
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = sharedPreferences.getString("token", "");

        editTextToken = findViewById(R.id.editTextTextPassword);
        buttonGo = findViewById(R.id.button);

        // Check token
        if (!token.isEmpty()) {
            navigateToAllServer();
        }

        // Disable Button
        buttonGo.setEnabled(false);

        editTextToken.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buttonGo.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Save token and redirect
        buttonGo.setOnClickListener(v -> {
            String token1 = editTextToken.getText().toString().trim();
            saveToken(token1);
            navigateToAllServer();
        });
    }

    private void saveToken(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", token);
        editor.apply();
    }

    private void navigateToAllServer() {
        Intent intent = new Intent(MainActivity.this, AllServers.class);
        startActivity(intent);
        finish();
    }
}
