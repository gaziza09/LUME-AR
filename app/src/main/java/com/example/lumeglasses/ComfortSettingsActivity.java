package com.example.lumeglasses;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

public class ComfortSettingsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private SeekBar brightnessSeekBar;
    private TextView brightnessValueTextView;
    private WindowManager.LayoutParams layoutParams;
    private MaterialButton emergencyButton;
    private long lastEmergencyPress = 0;
    private static final long EMERGENCY_PRESS_INTERVAL = 3000; // 3 секунды между нажатиями
    private LocationService locationService;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comfort_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        brightnessValueTextView = findViewById(R.id.brightnessValueTextView);
        emergencyButton = findViewById(R.id.emergencyButton);

        // Инициализация сервиса геолокации
        locationService = new LocationService(this);
        
        // Проверяем разрешение на геолокацию и запускаем обновления
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationService.startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Получаем текущие параметры окна
        layoutParams = getWindow().getAttributes();

        // Устанавливаем начальные значения
        int currentBrightness = getCurrentBrightness();
        brightnessSeekBar.setProgress(currentBrightness);
        brightnessValueTextView.setText(String.format("Яркость: %d%%", currentBrightness));

        // Настройка обработчиков событий
        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setBrightness(progress);
                    brightnessValueTextView.setText(String.format("Яркость: %d%%", progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Настройка кнопки экстренной помощи
        emergencyButton.setOnClickListener(v -> handleEmergencyButtonPress());
    }

    private void handleEmergencyButtonPress() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEmergencyPress < EMERGENCY_PRESS_INTERVAL) {
            // Если прошло меньше 3 секунд с последнего нажатия, показываем диалог подтверждения
            new AlertDialog.Builder(this)
                .setTitle("Вызов экстренной помощи")
                .setMessage("Вы уверены, что хотите вызвать экстренную помощь?")
                .setPositiveButton("Да", (dialog, which) -> {
                    checkLocationPermissionAndCall();
                })
                .setNegativeButton("Нет", null)
                .show();
        } else {
            // Если прошло больше 3 секунд, просто обновляем время последнего нажатия
            lastEmergencyPress = currentTime;
            Toast.makeText(this, "Нажмите еще раз для вызова экстренной помощи", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLocationPermissionAndCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            callEmergency();
        }
    }

    private void callEmergency() {
        Log.d("ComfortSettings", "Starting emergency call process");
        
        // Отправляем SOS-запрос с текущими координатами
        locationService.sendSOSRequest();
        Log.d("ComfortSettings", "SOS request sent");
        
        // Показываем уведомление пользователю
        Toast.makeText(this, "SOS-сигнал отправлен", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callEmergency();
            } else {
                Toast.makeText(this, "Для работы экстренной помощи необходим доступ к геолокации",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationService != null) {
            locationService.stopLocationUpdates();
        }
    }

    private int getCurrentBrightness() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return 50;
        }
    }

    private void setBrightness(int brightness) {
        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
            layoutParams.screenBrightness = brightness / 255f;
            getWindow().setAttributes(layoutParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        } else if (id == R.id.nav_comfort) {
            // Уже на странице настроек удобства
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
} 