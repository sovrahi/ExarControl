package moe.oh64.exarcontrol;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class BgSU extends Service {

    private static final String API_URL = "https://api.exaroton.com/v1/servers/";
    private String serverId;
    private String token;
    private WebSocket webSocket;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .pingInterval(10, TimeUnit.SECONDS)
            .build();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        connectWebSocket();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serverId = intent.getStringExtra("server_id");
        token = intent.getStringExtra("token");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "WebSocketServiceChannel",
                    "WebSocket Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void connectWebSocket() {
        Request request = new Request.Builder()
                .url(API_URL + serverId + "/websocket")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                // Subscribe to stats stream
                String subscribeStats = "{\"stream\":\"stats\",\"type\":\"start\"}";
                webSocket.send(subscribeStats);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    if ("status".equals(json.optString("stream"))) {
                        JSONObject data = json.getJSONObject("data");
                        int status = data.getInt("status");
                        sendNotification(getStatusMessage(status));
                    }
                } catch (Exception e) {
                    Log.e("BgSU", "Error processing WebSocket message", e);
                }
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                // Reconnect logic can be added here
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                // Handle failure and reconnect logic
                Log.e("BgSU", "WebSocket failure", t);
            }
        });
    }

    private void sendNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "WebSocketServiceChannel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Server Status Changed")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }

    private String getStatusMessage(int status) {
        String[] statusMessages = {
                "OFFLINE", "ONLINE", "STARTING", "STOPPING", "RESTARTING",
                "SAVING", "LOADING", "CRASHED", "PENDING",
                "TRANSFERRING", "PREPARING"
        };
        return statusMessages[status];
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.close(1000, "Service is being destroyed");
        }
    }
}
