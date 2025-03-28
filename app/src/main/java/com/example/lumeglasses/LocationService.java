package com.example.lumeglasses;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.json.JSONException;
import androidx.annotation.NonNull;

public class LocationService {
    private static final String TAG = "LocationService";
    private Context context;
    private LocationManager locationManager;
    private static final String SERVER_URL = "https://easywork.kz/add_coordinates/";
    private static final String SOS_URL = "https://easywork.kz/add_sos/";
    private LocationListener locationListener;
    private Handler handler;
    private Runnable locationUpdateRunnable;

    public LocationService(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.handler = new Handler();
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

    public void startLocationUpdates() {
        Log.d(TAG, "Starting location updates");
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        // Создаем LocationListener для получения обновлений местоположения
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "Location changed: " + location.getLatitude() + ", " + location.getLongitude());
                // Сохраняем последнее известное местоположение
                sendLocationToServer(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(TAG, "Location provider status changed: " + provider + ", status: " + status);
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(TAG, "Location provider enabled: " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.e(TAG, "Location provider disabled: " + provider);
            }
        };

        // Запрашиваем обновления местоположения
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                60000, // Обновление каждую минуту
                10,    // Минимальное расстояние в метрах
                locationListener
            );
            Log.d(TAG, "Successfully requested location updates");
        } catch (Exception e) {
            Log.e(TAG, "Error requesting location updates: " + e.getMessage());
        }

        // Отправляем текущее местоположение сразу
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation != null) {
            Log.d(TAG, "Got last known location: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
            sendLocationToServer(lastKnownLocation);
        } else {
            Log.w(TAG, "No last known location available");
        }

        // Запускаем периодическую отправку координат
        startPeriodicLocationUpdates();
    }

    private void startPeriodicLocationUpdates() {
        Log.d(TAG, "Starting periodic location updates");
        // Останавливаем предыдущий периодический обновления, если они есть
        if (locationUpdateRunnable != null) {
            handler.removeCallbacks(locationUpdateRunnable);
            Log.d(TAG, "Removed previous periodic updates");
        }

        locationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Periodic update triggered");
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation != null) {
                        Log.d(TAG, "Got location for periodic update: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
                        sendLocationToServer(lastKnownLocation);
                    } else {
                        Log.w(TAG, "No location available for periodic update");
                    }
                } else {
                    Log.e(TAG, "Location permission not granted for periodic update");
                }
                // Планируем следующее обновление
                handler.postDelayed(this, 60000); // Повторяем каждую минуту
                Log.d(TAG, "Scheduled next periodic update");
            }
        };
        // Запускаем периодические обновления
        handler.post(locationUpdateRunnable);
        Log.d(TAG, "Posted initial periodic update");
    }

    private void sendLocationToServer(Location location) {
        Log.d(TAG, "Preparing to send location to server");
        OkHttpClient client = getUnsafeOkHttpClient();
        
        // Создаем JSON с координатами
        JSONObject json = new JSONObject();
        try {
            json.put("latitude", location.getLatitude());
            json.put("longitude", location.getLongitude());
            Log.d(TAG, "Created JSON: " + json.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), json.toString()
        );

        Request request = new Request.Builder()
            .url(SERVER_URL)
            .post(body)
            .build();

        Log.d(TAG, "Sending request to: " + SERVER_URL);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to send location: " + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "Server response: " + response.code() + ", body: " + responseBody);
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Server error: " + response.code());
                } else {
                    Log.d(TAG, "Successfully sent location to server");
                }
                response.close();
            }
        });
    }

    public void sendSOSRequest() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Создаем временный LocationListener для получения однократного обновления местоположения
            LocationListener oneTimeListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.d(TAG, "Got location for SOS: " + location.getLatitude() + ", " + location.getLongitude());
                    // Отправляем координаты на основной эндпоинт
                    sendLocationToServer(location);
                    // Отправляем SOS-запрос
                    sendSOSRequestToServer(location);
                    // Удаляем временный listener
                    locationManager.removeUpdates(this);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            try {
                // Запрашиваем обновление местоположения
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,    // Минимальное время между обновлениями
                    0,    // Минимальное расстояние в метрах
                    oneTimeListener
                );
                Log.d(TAG, "Requested location update for SOS");
            } catch (Exception e) {
                Log.e(TAG, "Error requesting location update for SOS: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Location permission not granted for SOS request");
        }
    }

    private void sendSOSRequestToServer(Location location) {
        OkHttpClient client = getUnsafeOkHttpClient();
        
        // Создаем JSON с данными SOS-запроса
        JSONObject json = new JSONObject();
        try {
            if (location != null) {
                json.put("latitude", location.getLatitude());
                json.put("longitude", location.getLongitude());
            }
            json.put("message", "SOS");
        } catch (Exception e) {
            Log.e(TAG, "Error creating SOS JSON: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), json.toString()
        );

        Request request = new Request.Builder()
            .url(SOS_URL)
            .post(body)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to send SOS request: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Server error while sending SOS: " + response.code());
                }
                response.close();
            }
        });
    }

    public void stopLocationUpdates() {
        if (locationListener != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates(locationListener);
            }
        }
        
        if (handler != null && locationUpdateRunnable != null) {
            handler.removeCallbacks(locationUpdateRunnable);
        }
    }
} 