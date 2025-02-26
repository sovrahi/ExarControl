package moe.oh64.exarcontrol;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class Information extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information);

        TextView versionTextView = findViewById(R.id.version);
        TextView librariesListTextView = findViewById(R.id.libraries_list);
        Button backButton = findViewById(R.id.back);

        // Retrieve and display the app version
        String versionOption = "Version: Unknown";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            versionOption = "Version: " + versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("information", "Error fetching package info", e);
        }
        versionTextView.setText(versionOption);

        // Display the list of libraries used
        String libraries = "appcompat: 1.7.0\n" +
                "material: 1.12.0\n" +
                "activity: 1.8.0\n" +
                "constraintlayout: 2.2.0\n" +
                "okhttp: 4.11.0\n" +
                "glide: 4.13.2\n" +
                "junit: 4.13.2\n" +
                "ext-junit: 1.2.1\n" +
                "espresso-core: 3.6.1";
        librariesListTextView.setText(libraries);

        // Handle back button click
        backButton.setOnClickListener(v -> finish());
    }
}
