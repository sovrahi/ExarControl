package moe.oh64.exarcontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText editTextToken;
    private Button buttonGo;
    private int clickCount = 0;
    private static final String PREFS_NAME = "ExarotonPrefs";
    private final String[] toastMessages = {
            ">w<",
            ">wO",
            ">o<"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Token
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = sharedPreferences.getString("token", "");

        editTextToken = findViewById(R.id.editTextTextPassword);
        buttonGo = findViewById(R.id.button);
        TextView textView5 = findViewById(R.id.textView5);

        // Check token
        if (!token.isEmpty()) {
            navigateToAllServer();
        }

        Intent intent = new Intent(MainActivity.this, NotificationPerm.class);
        startActivity(intent);

        // Disable Button
        buttonGo.setEnabled(false);

        editTextToken.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Enable the button only if the token length is greater than 10
                buttonGo.setEnabled(s.length() > 10);
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

        // Set click listener for textView5
        textView5.setOnClickListener(v -> {
            clickCount++;
            if (clickCount >= 3 && clickCount <= 5) {
                Toast.makeText(MainActivity.this, toastMessages[clickCount - 3], Toast.LENGTH_SHORT).show();
            }
            if (clickCount == 5) {
                saveToken("OwO");
                navigateToAllServer();
            }
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
