package app.subenoeva.roadsync;

import android.location.Location;
import com.drivemode.sab.common.weather.entities.Weather;
import com.drivemode.sab.common.weather.entities.WeatherForecast;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Replacement for RoadSync's Honda-hosted weather lookup, backed by Open-Meteo (no API key).
 * Results are mapped onto the OpenWeatherMap condition ids the app's icon logic expects.
 */
public final class OpenMeteoWeather {
    private OpenMeteoWeather() {}

    public static Single current(final Location location) {
        return Single.k(new SingleOnSubscribe() {
            @Override
            public void a(SingleEmitter emitter) {
                try {
                    JSONObject root = fetch(location);
                    JSONObject current = root.getJSONObject("current");
                    emitter.onSuccess(build(
                        current.getInt("weather_code"),
                        (float) current.getDouble("temperature_2m"),
                        current.getInt("is_day") == 1,
                        placeName(location)));
                } catch (Throwable t) {
                    emitter.a(t);
                }
            }
        }).U(Schedulers.d());
    }

    public static Single forecast(final Location location, final List intervals) {
        return Single.k(new SingleOnSubscribe() {
            @Override
            public void a(SingleEmitter emitter) {
                try {
                    JSONObject root = fetch(location);
                    JSONObject hourly = root.getJSONObject("hourly");
                    JSONArray times = hourly.getJSONArray("time");
                    JSONArray temps = hourly.getJSONArray("temperature_2m");
                    JSONArray codes = hourly.getJSONArray("weather_code");
                    JSONArray days = hourly.getJSONArray("is_day");

                    // "current.time" is local, e.g. 2026-10-25T14:15; hourly entries are on the hour.
                    String now = root.getJSONObject("current").getString("time").substring(0, 13);
                    int base = 0;
                    for (int i = 0; i < times.length(); i++) {
                        if (times.getString(i).startsWith(now)) {
                            base = i;
                            break;
                        }
                    }

                    String place = placeName(location);
                    List result = new ArrayList();
                    for (Object interval : intervals) {
                        int ahead = ((Number) interval).intValue();
                        int index = Math.min(base + ahead, times.length() - 1);
                        Weather weather = build(
                            codes.getInt(index),
                            (float) temps.getDouble(index),
                            days.getInt(index) == 1,
                            place);
                        result.add(new WeatherForecast(weather, ahead));
                    }
                    emitter.onSuccess(result);
                } catch (Throwable t) {
                    emitter.a(t);
                }
            }
        }).U(Schedulers.d());
    }

    private static JSONObject fetch(Location location) throws Exception {
        String url = String.format(Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                + "&current=temperature_2m,weather_code,is_day"
                + "&hourly=temperature_2m,weather_code,is_day"
                + "&forecast_days=3&timezone=auto",
            location.getLatitude(), location.getLongitude());
        return new JSONObject(get(url));
    }

    /** Best effort; an empty name is fine, the app only shows it as a label. */
    private static String placeName(Location location) {
        try {
            String url = String.format(Locale.US,
                "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%.4f&longitude=%.4f&localityLanguage=en",
                location.getLatitude(), location.getLongitude());
            JSONObject json = new JSONObject(get(url));
            String city = json.optString("city");
            if (city == null || city.isEmpty()) city = json.optString("locality");
            return city == null ? "" : city;
        } catch (Throwable t) {
            return "";
        }
    }

    private static String get(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", "RoadSync-OpenMeteo-Patch");
        try {
            InputStream stream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }

    private static Weather build(int wmo, float celsius, boolean day, String place) {
        int id;
        String main;
        String description;
        switch (wmo) {
            case 0:  id = 800; main = "Clear";        description = "clear sky"; break;
            case 1:  id = 801; main = "Clouds";       description = "mainly clear"; break;
            case 2:  id = 802; main = "Clouds";       description = "partly cloudy"; break;
            case 3:  id = 804; main = "Clouds";       description = "overcast"; break;
            case 45: id = 741; main = "Fog";          description = "fog"; break;
            case 48: id = 741; main = "Fog";          description = "rime fog"; break;
            case 51: id = 300; main = "Drizzle";      description = "light drizzle"; break;
            case 53: id = 301; main = "Drizzle";      description = "drizzle"; break;
            case 55: id = 302; main = "Drizzle";      description = "heavy drizzle"; break;
            case 56: id = 511; main = "Rain";         description = "freezing drizzle"; break;
            case 57: id = 511; main = "Rain";         description = "heavy freezing drizzle"; break;
            case 61: id = 500; main = "Rain";         description = "light rain"; break;
            case 63: id = 501; main = "Rain";         description = "rain"; break;
            case 65: id = 502; main = "Rain";         description = "heavy rain"; break;
            case 66: id = 511; main = "Rain";         description = "freezing rain"; break;
            case 67: id = 511; main = "Rain";         description = "heavy freezing rain"; break;
            case 71: id = 600; main = "Snow";         description = "light snow"; break;
            case 73: id = 601; main = "Snow";         description = "snow"; break;
            case 75: id = 602; main = "Snow";         description = "heavy snow"; break;
            case 77: id = 600; main = "Snow";         description = "snow grains"; break;
            case 80: id = 500; main = "Rain";         description = "light showers"; break;
            case 81: id = 501; main = "Rain";         description = "showers"; break;
            case 82: id = 502; main = "Rain";         description = "violent showers"; break;
            case 85: id = 620; main = "Snow";         description = "light snow showers"; break;
            case 86: id = 621; main = "Snow";         description = "snow showers"; break;
            case 95: id = 200; main = "Thunderstorm"; description = "thunderstorm"; break;
            case 96: id = 201; main = "Thunderstorm"; description = "thunderstorm with hail"; break;
            case 99: id = 202; main = "Thunderstorm"; description = "thunderstorm with heavy hail"; break;
            default: id = 804; main = "Clouds";       description = "unknown"; break;
        }
        return new Weather(id, celsius, place, description, main, day);
    }
}
