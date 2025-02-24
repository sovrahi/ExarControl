package moe.oh64.exarcontrol;

import android.app.Application;
import android.content.Intent;

public class App extends Application {
    private NetworkMonitor networkMonitor;

    @Override
    public void onCreate() {
        super.onCreate();

        // ⚡ Vérification immédiate de la connexion Internet au démarrage
        if (!NetworkUtil.isConnected(this)) {
            redirectToNoInternet();
        }

        networkMonitor = new NetworkMonitor(this);
        networkMonitor.register();
    }

    // Méthode de redirection vers l'activité NoInternet
    private void redirectToNoInternet() {
        Intent noInternetIntent = new Intent(this, NoInternet.class);
        noInternetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(noInternetIntent);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (networkMonitor != null) {
            networkMonitor.unregister();
        }
    }
}
