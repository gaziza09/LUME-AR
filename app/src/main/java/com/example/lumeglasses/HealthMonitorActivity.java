package com.example.lumeglasses;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class HealthMonitorActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private TextView pulseTextView, stressTextView, heartRateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_monitor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        pulseTextView = findViewById(R.id.pulseTextView);
        stressTextView = findViewById(R.id.stressTextView);
        heartRateTextView = findViewById(R.id.heartRateTextView);

        updateHealthData(75, 65, 80);
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
            // Уже на странице мониторинга здоровья
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

    private void updateHealthData(int pulse, int stress, int heartRate) {
        pulseTextView.setText(String.format("Пульс: %d уд/мин", pulse));
        stressTextView.setText(String.format("Уровень стресса: %d%%", stress));
        heartRateTextView.setText(String.format("Частота сердечных сокращений: %d уд/мин", heartRate));
    }
} 