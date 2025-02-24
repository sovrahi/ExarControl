package moe.oh64.exarcontrol;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class NetworkMonitor {
    private final ConnectivityManager connectivityManager;
    private final ConnectivityManager.NetworkCallback networkCallback;

    public NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // Okay
            }

            @Override
            public void onLost(@NonNull Network network) {
                // Connection Lost
                Toast.makeText(context, "Pas de connexion Internet !", Toast.LENGTH_SHORT).show();
                Intent noInternetIntent = new Intent(context, NoInternet.class);
                noInternetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(noInternetIntent);
            }
        };
    }

    public void register() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    public void unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }
}
