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
 * The app previously had no network at all. This adds one HTTPS GET per refresh (Home start,
 * at most every 10 minutes, and every 10 minutes while open), returning a temperature and a WMO
 * weather code. When weather_aqi is on, one extra tiny request to Open-Meteo's air-quality API
 * adds the US AQI to the same line; it is a single small JSON object, no image and no persistent
 * bitmap. Nothing is uploaded. If a request fails the last good reading stays on screen ("--" if
 * there has never been one).
 */
final class Weather {

    interface Callback {
        void onWeather(String text, int code);
    }

    /** Last good line, so returning Home does not flash an empty widget. */
    private static volatile String cached;
    private static volatile int cachedCode = -1;
    private static volatile long cachedAt;

    private Weather() {
    }

    static String cached() {
        return cached;
    }

    /** WMO weather code for {@link #cached()}, or -1 when there is no reading yet. */
    static int cachedCode() {
        return cachedCode;
    }

    static void refresh(Context context, Callback callback) {
        final Context app = context.getApplicationContext();
        final String city = app.getString(R.string.weather_city).trim();
        if (city.isEmpty()) return;

        // Returning Home repeatedly must not mean one request per bounce.
        if (cached != null && System.currentTimeMillis() - cachedAt < 10 * 60_000L) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onWeather(cached, cachedCode));
            return;
        }

        final String lat = app.getString(R.string.weather_lat);
        final String lon = app.getString(R.string.weather_lon);
        final String units = app.getString(R.string.weather_units);
        final boolean withAqi = app.getResources().getBoolean(R.bool.weather_aqi);

        new Thread(() -> {
            final String text = fetch(app, city, lat, lon, units, withAqi);
            new Handler(Looper.getMainLooper()).post(() -> callback.onWeather(text, cachedCode));
        }, "weather").start();
    }

    /** Plain-language condition for the WMO code, or null when the code is unknown. */
    private static String condition(Context context, int code) {
        int res;
        if (code == 0) res = R.string.weather_clear;
        else if (code == 1) res = R.string.weather_mainly_clear;
        else if (code == 2) res = R.string.weather_partly_cloudy;
        else if (code == 3) res = R.string.weather_overcast;
        else if (code == 45 || code == 48) res = R.string.weather_fog;
        else if (code == 51 || code == 53 || code == 55) res = R.string.weather_drizzle;
        else if (code == 56 || code == 57) res = R.string.weather_freezing_drizzle;
        else if (code == 61 || code == 63 || code == 65) res = R.string.weather_rain;
        else if (code == 66 || code == 67) res = R.string.weather_freezing_rain;
        else if (code >= 71 && code <= 77) res = R.string.weather_snow;
        else if (code == 80 || code == 81 || code == 82) res = R.string.weather_rain_showers;
        else if (code == 85 || code == 86) res = R.string.weather_snow_showers;
        else if (code == 95) res = R.string.weather_thunderstorm;
        else if (code == 96 || code == 99) res = R.string.weather_thunderstorm_hail;
        else return null;
        return context.getString(res);
    }

    private static String fetch(Context context, String city, String lat, String lon, String units, boolean withAqi) {
        HttpURLConnection connection = null;
        try {
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat
                    + "&longitude=" + lon
                    + "&current=temperature_2m,weather_code"
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

                JSONObject current = new JSONObject(body.toString()).getJSONObject("current");
                double temp = current.getDouble("temperature_2m");
                cachedCode = current.optInt("weather_code", -1);
                String condition = condition(context, cachedCode);
                int aqi = withAqi ? fetchAqi(lat, lon) : -1;
                String text = city + " " + Math.round(temp)
                        + ("fahrenheit".equalsIgnoreCase(units) ? "\u00B0F" : "\u00B0C")
                        + (condition == null ? "" : " \u00B7 " + condition)
                        + (aqi >= 0 ? " \u00B7 AQI " + aqi : "");
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

    /** US AQI from Open-Meteo's air-quality endpoint; -1 if it is unavailable. */
    private static int fetchAqi(String lat, String lon) {
        HttpURLConnection connection = null;
        try {
            String url = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + lat
                    + "&longitude=" + lon
                    + "&current=us_aqi&timezone=auto";
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
                JSONObject current = new JSONObject(body.toString()).optJSONObject("current");
                return current == null ? -1 : current.optInt("us_aqi", -1);
            }
        } catch (Exception e) {
            return -1;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
