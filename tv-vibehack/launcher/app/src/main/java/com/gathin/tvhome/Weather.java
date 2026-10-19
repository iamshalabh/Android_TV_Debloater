package com.gathin.tvhome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * One small Open-Meteo request, no API key and no libraries.
 *
 * The app previously had no network at all. This adds exactly one HTTPS GET per refresh (Home start,
 * at most every 10 minutes, and every 30 minutes while open), returning a single number; nothing is
 * uploaded. If the request fails the last good reading stays on screen ("--" if there has never
 * been one).
 */
final class Weather {

    interface Callback {
        void onWeather(String text);
    }

    /** Last good line, so returning Home does not flash an empty widget. */
    private static volatile String cached;
    private static volatile long cachedAt;

    private Weather() {
    }

    static String cached() {
        return cached;
    }

    static void refresh(Context context, Callback callback) {
        final Context app = context.getApplicationContext();
        final String city = app.getString(R.string.weather_city).trim();
        if (city.isEmpty()) return;

        // Returning Home repeatedly must not mean one request per bounce.
        if (cached != null && System.currentTimeMillis() - cachedAt < 10 * 60_000L) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onWeather(cached));
            return;
        }

        final String lat = app.getString(R.string.weather_lat);
        final String lon = app.getString(R.string.weather_lon);
        final String units = app.getString(R.string.weather_units);

        new Thread(() -> {
            final String text = fetch(city, lat, lon, units);
            new Handler(Looper.getMainLooper()).post(() -> callback.onWeather(text));
        }, "weather").start();
    }

    private static String fetch(String city, String lat, String lon, String units) {
        HttpURLConnection connection = null;
        try {
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat
                    + "&longitude=" + lon
                    + "&current=temperature_2m"
                    + "&timezone=auto";
            if ("fahrenheit".equalsIgnoreCase(units)) url += "&temperature_unit=fahrenheit";

            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "tvhome");

            try (InputStream in = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);

                double temp = new JSONObject(body.toString())
                        .getJSONObject("current")
                        .getDouble("temperature_2m");
                String text = city + "  " + Math.round(temp)
                        + ("fahrenheit".equalsIgnoreCase(units) ? "\u00B0F" : "\u00B0C");
                cached = text;
                cachedAt = System.currentTimeMillis();
                return text;
            }
        } catch (Exception e) {
            return cached;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
