package moe.oh64.exarcontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class InvalidToken extends AppCompatActivity {

    private static final String PREFS_NAME = "ExarotonPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invalidtoken);

        Button button = findViewById(R.id.button2);
        button.setOnClickListener(v -> {
            clearSavedToken();

            Intent intent = new Intent(InvalidToken.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void clearSavedToken() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("token");
        editor.apply();
    }
}
