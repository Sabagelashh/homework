package bot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        String weatherAPIKey = "dd8caba2bc465130ee1f203a0995297";
        String exchangeAPIKey = "0296e5c773eca2e1f4627b74";

        String[] location = getLocation();
        System.out.println();
        String[] exchangeRates = getExchangeRates();

        System.out.println();
        handleWeatherData(location, weatherAPIKey);
        System.out.println();
        handleExchangeData(exchangeRates[0], exchangeRates[1], exchangeAPIKey);
    }

    private static  getApiData(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            System.out.println("Error: " + conn.getResponseMessage());
            throw new Exception("Failed to fetch data from API! - " + status);

        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        conn.disconnect();

        JsonElement jsonElement = JsonParser.parseString(content.toString());
        JsonObject jsonObject;

        if(jsonElement.isJsonArray()){
            jsonObject = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
        } else {
            jsonObject = jsonElement.getAsJsonObject();
        }

        return jsonObject;
    }

    private static String[] getCoordinates(String api, String location) throws Exception {
        JsonObject coordinates = getApiData("https://api.openweathermap.org/geo/1.0/direct?q=" + location + "&appid=" + api);

        String lat = coordinates.get("lat").getAsString();
        String lon = coordinates.get("lon").getAsString();

        String[] answer = {lat, lon};

        return answer;
    }

    private static String[] getLocation() {
        String[] location = new String[2];

        Scanner scanner = new Scanner(System.in);
        System.out.print("City: ");
        location[0] = scanner.nextLine();
        System.out.print("(Only for US, you can skip it otherwise) State Code: ");
        location[1] = scanner.nextLine();

        if (location[1].isEmpty()) {
            System.out.println("Location: " + location[0]);
        } else {
            System.out.println("Location: " + location[0] + ", " + location[1]);
        }

        return location;
    }

    private static String[] getExchangeRates() {
        String[] exchangeRates = new String[2];

        Scanner scanner = new Scanner(System.in);
        System.out.print("Exchange rates, From: ");
        exchangeRates[0] = scanner.nextLine();
        System.out.print("To: ");
        exchangeRates[1] = scanner.nextLine();

        return exchangeRates;
    }

    private static void handleWeatherData(String[] location, String weatherAPIKey) throws Exception {

        String[] userCoordinates;

        String unit = "metric";
        if (location[1].isEmpty()) {
            // Without State Code
            userCoordinates = getCoordinates(weatherAPIKey,location[0]);
        } else {
            // With State Code for US only
            userCoordinates = getCoordinates(weatherAPIKey, location[0] + "," + location[1]);
            unit = "imperial";
        }

        String weatherAPI = "https://api.openweathermap.org/data/3.0/onecall?lat=" + userCoordinates[0] + "&lon=" + userCoordinates[1] + "&appid=" + weatherAPIKey + "&units=" + unit;
        JsonObject weatherData = getApiData(weatherAPI);

        JsonObject currentWeather = weatherData.get("current").getAsJsonObject();
        System.out.println("Current Weather: " + currentWeather.get("weather").getAsJsonArray().get(0).getAsJsonObject().get("description").getAsString());

        // Format dates
        long sunriseTimestamp = currentWeather.get("sunrise").getAsLong();
        long sunsetTimestamp = currentWeather.get("sunset").getAsLong();

        LocalDateTime sunrise = LocalDateTime.ofInstant(Instant.ofEpochSecond(sunriseTimestamp), ZoneId.systemDefault());
        LocalDateTime sunset = LocalDateTime.ofInstant(Instant.ofEpochSecond(sunsetTimestamp), ZoneId.systemDefault());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("Sunrise: " + sunrise.format(formatter) + "\t" + " Sunset: " + sunset.format(formatter));

        // Use correct unit for temperatures
        if (unit.equals("metric")) {
            System.out.println("Temperature: " + currentWeather.get("temp").getAsString() + "°C\t" + "Feels Like: " + currentWeather.get("feels_like").getAsString() + "°C");
        } else {
            System.out.println("Temperature: " + currentWeather.get("temp").getAsString() + "°F\t" + "Feels Like: " + currentWeather.get("feels_like").getAsString() + "°F");
        }

        System.out.println("Humidity: " + currentWeather.get("humidity").getAsString() + "%");
        System.out.println("Wind Speed: " + currentWeather.get("wind_speed").getAsString() + "m/s");
    }

    private static void handleExchangeData( String from , String to, String exchangeAPIKey) throws Exception {
        String exchangeAPI = "https://v6.exchangerate-api.com/v6/" + exchangeAPIKey + "/latest/" + from;
        JsonObject exchangeData = getApiData(exchangeAPI);

        double usdToGel = exchangeData.get("conversion_rates").getAsJsonObject().get(to).getAsDouble();
        System.out.println("Exchange rate from " + from + " to " + to + ": 1" + from + " = " + usdToGel + to);
    }
}
