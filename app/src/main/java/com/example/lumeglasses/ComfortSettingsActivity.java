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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

public class ComfortSettingsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private SeekBar brightnessSeekBar, contrastSeekBar;
    private TextView brightnessValueTextView, contrastValueTextView;
    private WindowManager.LayoutParams layoutParams;
    private MaterialButton emergencyButton;
    private long lastEmergencyPress = 0;
    private static final long EMERGENCY_PRESS_INTERVAL = 3000; // 3 секунды между нажатиями

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
        contrastSeekBar = findViewById(R.id.contrastSeekBar);
        brightnessValueTextView = findViewById(R.id.brightnessValueTextView);
        contrastValueTextView = findViewById(R.id.contrastValueTextView);
        emergencyButton = findViewById(R.id.emergencyButton);

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

        contrastSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setContrast(progress);
                    contrastValueTextView.setText(String.format("Контраст: %d%%", progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Установка начальных значений
        contrastSeekBar.setProgress(50);

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
                .setPositiveButton("Да", (dialog, which) -> callEmergency())
                .setNegativeButton("Нет", null)
                .show();
        } else {
            // Если прошло больше 3 секунд, просто обновляем время последнего нажатия
            lastEmergencyPress = currentTime;
            Toast.makeText(this, "Нажмите еще раз для вызова экстренной помощи", Toast.LENGTH_SHORT).show();
        }
    }

    private void callEmergency() {
        // Звоним в экстренную службу
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:112")); // Номер экстренной службы
        startActivity(intent);
        
        // Отправляем SMS с геолокацией (если есть разрешение)
        // TODO: Добавить отправку SMS с геолокацией
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

    private void setContrast(int contrast) {
        // Здесь будет код для установки контрастности
        // В Android нет прямого API для управления контрастностью экрана
        // Это можно реализовать через наложение полупрозрачного слоя или
        // через системные настройки доступности
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
        } else if (id == R.id.nav_health) {
            startActivity(new Intent(this, HealthMonitorActivity.class));
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