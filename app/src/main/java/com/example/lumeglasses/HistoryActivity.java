package com.example.lumeglasses;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import java.io.IOException;

public class HistoryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ListView listView;
    private Spinner dateSpinner;
    private WebSocket webSocket;
    private List<WordEntry> wordEntries = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;
    private ArrayAdapter<String> spinnerAdapter;
    private Set<String> uniqueDates = new HashSet<>();
    private Button clearChatButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Инициализация Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(""); // Убираем заголовок

        // Инициализация DrawerLayout и NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Настройка ActionBarDrawerToggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        listView = findViewById(R.id.listView);
        dateSpinner = findViewById(R.id.dateSpinner);
        clearChatButton = findViewById(R.id.clearChatButton);

        // Инициализация адаптеров
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>()) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                android.widget.TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextSize(20);
                textView.setTextColor(getResources().getColor(android.R.color.black));
                return view;
            }
        };
        listView.setAdapter(listAdapter);

        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinner.setAdapter(spinnerAdapter);

        // Фильтрация по дате
        dateSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterWordsByDate(spinnerAdapter.getItem(position));
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Обработчик нажатия кнопки "Удалить чат"
        clearChatButton.setOnClickListener(v -> clearChatHistory());

        connectWebSocket();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_menu) {
            drawerLayout.openDrawer(GravityCompat.END);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else if (id == R.id.nav_history) {
            // Уже на странице истории
        } else if (id == R.id.nav_comfort) {
            startActivity(new Intent(this, ComfortSettingsActivity.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.END);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
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
                .url("wss://easywork.kz/words")
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                runOnUiThread(() -> listAdapter.add("Подключено к серверу истории!"));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(text);
                        JSONArray wordsArray = json.getJSONArray("words");
                        wordEntries.clear();
                        uniqueDates.clear();
                        listAdapter.clear();

                        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));

                        for (int i = 0; i < wordsArray.length(); i++) {
                            JSONObject wordObj = wordsArray.getJSONObject(i);
                            String word = wordObj.getString("word");
                            double timestamp = wordObj.getDouble("timestamp");
                            String humanReadableTime = wordObj.getString("human_readable_time");
                            Date date = new Date((long) (timestamp * 1000));
                            String dateStr = dateFormat.format(date);

                            wordEntries.add(new WordEntry(word, timestamp, humanReadableTime));
                            uniqueDates.add(dateStr);
                        }

                        Collections.sort(wordEntries, (a, b) -> Double.compare(b.timestamp, a.timestamp));

                        spinnerAdapter.clear();
                        spinnerAdapter.add("Все даты");
                        spinnerAdapter.addAll(uniqueDates);
                        spinnerAdapter.notifyDataSetChanged();

                        filterWordsByDate("Все даты");
                    } catch (Exception e) {
                        listAdapter.add("Ошибка обработки данных: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                runOnUiThread(() -> listAdapter.add("Ошибка подключения: " + t.getMessage()));
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                runOnUiThread(() -> listAdapter.add("Соединение закрыто: " + reason));
            }
        });
    }

    private void filterWordsByDate(String selectedDate) {
        listAdapter.clear();
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));
        for (WordEntry entry : wordEntries) {
            Date date = new Date((long) (entry.timestamp * 1000));
            String dateStr = dateFormat.format(date);
            if (selectedDate.equals("Все даты") || dateStr.equals(selectedDate)) {
                listAdapter.add(entry.word + " (" + entry.humanReadableTime + ")");
            }
        }
        listAdapter.notifyDataSetChanged();
    }

    // Метод для отправки запроса на очистку истории
    private void clearChatHistory() {
        OkHttpClient client = getUnsafeOkHttpClient();
        Request request = new Request.Builder()
                .url("https://easywork.kz/clear_words/")
                .post(RequestBody.create(null, new byte[0])) // Пустое тело для POST
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> listAdapter.add("Ошибка очистки чата: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        listAdapter.clear();
                        wordEntries.clear();
                        uniqueDates.clear();
                        spinnerAdapter.clear();
                        spinnerAdapter.add("Все даты");
                        spinnerAdapter.notifyDataSetChanged();
                        listAdapter.add("Чат успешно очищен!");
                        listAdapter.notifyDataSetChanged();
                    });
                } else {
                    runOnUiThread(() -> listAdapter.add("Ошибка сервера: " + response.message()));
                }
                response.close();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.close(1000, "Activity destroyed");
        }
    }

    private static class WordEntry {
        String word;
        double timestamp;
        String humanReadableTime;

        WordEntry(String word, double timestamp, String humanReadableTime) {
            this.word = word;
            this.timestamp = timestamp;
            this.humanReadableTime = humanReadableTime;
        }
    }
}