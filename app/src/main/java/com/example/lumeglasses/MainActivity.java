package com.example.lumeglasses;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import android.widget.TextView;
import org.json.JSONObject;
import androidx.annotation.NonNull;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private TextView textView;
    private TextView naturalSoundsTextView;
    private WebSocket webSocket;
    private WebSocket naturalSoundsWebSocket;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        naturalSoundsTextView = findViewById(R.id.naturalSoundsTextView);
        drawerLayout = findViewById(R.id.drawer_layout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(""); // Убираем заголовок

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        connectWebSocket();
        connectNaturalSoundsWebSocket();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        System.out.println("Menu inflated!");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_menu) {
            System.out.println("Menu clicked!");
            drawerLayout.openDrawer(GravityCompat.END);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            // Уже на главной странице
        } else if (id == R.id.nav_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        } else if (id == R.id.nav_comfort) {
            startActivity(new Intent(this, ComfortSettingsActivity.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.END);
        return true;
    }

    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                    }
            };
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void connectWebSocket() {
        OkHttpClient client = getUnsafeOkHttpClient();
        Request request = new Request.Builder()
                .url("wss://easywork.kz/proxy")
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                runOnUiThread(() -> textView.setText("Подключено к серверу!"));
                webSocket.send("Test message");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(text);
                        String messageText = json.getString("text");
                        textView.setText(messageText);
                    } catch (Exception e) {
                        textView.setText(text);
                    }
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                runOnUiThread(() -> textView.setText(bytes.hex()));
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                runOnUiThread(() -> textView.setText("Ошибка подключения: " + t.getMessage()));
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                runOnUiThread(() -> textView.setText("Соединение закрыто: " + reason));
            }
        });
    }

    private void connectNaturalSoundsWebSocket() {
        OkHttpClient client = getUnsafeOkHttpClient();
        Request request = new Request.Builder()
                .url("wss://easywork.kz/natural_sounds")
                .build();

        naturalSoundsWebSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                runOnUiThread(() -> naturalSoundsTextView.setText("Подключено к серверу оповещений!"));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(text);
                        String messageText = json.getString("sound");
                        naturalSoundsTextView.setText(messageText);
                    } catch (Exception e) {
                        naturalSoundsTextView.setText(text);
                    }
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                runOnUiThread(() -> naturalSoundsTextView.setText("Ошибка подключения: " + t.getMessage()));
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                runOnUiThread(() -> naturalSoundsTextView.setText("Соединение закрыто: " + reason));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.close(1000, "Activity destroyed");
        }
        if (naturalSoundsWebSocket != null) {
            naturalSoundsWebSocket.close(1000, "Activity destroyed");
        }
    }
}