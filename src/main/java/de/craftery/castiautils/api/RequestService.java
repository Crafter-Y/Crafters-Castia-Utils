package de.craftery.castiautils.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.config.CastiaConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Jankson;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.JsonElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import oshi.annotation.concurrent.NotThreadSafe;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

public class RequestService {
    public static Optional<String> put(String route, Object data) {
        if (!checkApiPrerequisites()) return Optional.of("API token or URL empty");
        CastiaConfig config = CastiaUtils.getConfig();
        Gson gson = new Gson();

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPut put = new HttpPut(config.apiUrl + route);
            Jankson jankson = Jankson.builder().build();
            JsonElement inputJson = jankson.toJson(data);
            StringEntity postingString = new StringEntity(inputJson.toJson());
            put.setEntity(postingString);
            put.setHeader("Content-type", "application/json");
            put.setHeader("Authorization","Bearer " + config.token);
            HttpResponse response = httpClient.execute(put);
            String responseString = new BasicResponseHandler().handleResponse(response);
            ApiDefaultResponse json = gson.fromJson(responseString, RequestService.ApiDefaultResponse.class);
            if (json.success) {
                return Optional.empty();
            }
            ApiResponseWithData errorJson = gson.fromJson(responseString, RequestService.ApiResponseWithData.class);

            return Optional.of(gson.toJson(errorJson.data));
        } catch (IOException e) {
            return Optional.of(e.getMessage());
        }
    }

    public static ApiResponseWithData get(String route, Object data) {
        Gson gson = new Gson();
        if (!checkApiPrerequisites()) {
            JsonObject jso = new JsonObject();
            jso.addProperty("error", "API token or URL empty");
            return new ApiResponseWithData(false, jso);
        }
        CastiaConfig config = CastiaUtils.getConfig();

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGetWithBody get = new HttpGetWithBody(config.apiUrl + route);
            Jankson jankson = Jankson.builder().build();
            JsonElement inputJson = jankson.toJson(data);
            StringEntity postingString = new StringEntity(inputJson.toJson());
            get.setEntity(postingString);
            get.setHeader("Accept", "application/json");
            get.setHeader("Content-type", "application/json");
            get.setHeader("Authorization","Bearer " + config.token);
            HttpResponse response = httpClient.execute(get);
            String responseString = new BasicResponseHandler().handleResponse(response);
            return gson.fromJson(responseString, RequestService.ApiResponseWithData.class);
        } catch (IOException e) {
            JsonObject jso = new JsonObject();
            jso.addProperty("error", e.getMessage());
            return new ApiResponseWithData(false, jso);
        }
    }

    public static ApiResponseWithData get(String route) {
        Gson gson = new Gson();
        if (!checkApiPrerequisites()) {
            JsonObject jso = new JsonObject();
            jso.addProperty("error", "API token or URL empty");
            return new ApiResponseWithData(false, jso);
        }
        CastiaConfig config = CastiaUtils.getConfig();

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet(config.apiUrl + route);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization","Bearer " + config.token);
            HttpResponse response = httpClient.execute(get);
            String responseString = new BasicResponseHandler().handleResponse(response);
            return gson.fromJson(responseString, RequestService.ApiResponseWithData.class);
        } catch (IOException e) {
            JsonObject jso = new JsonObject();
            jso.addProperty("error", e.getMessage());
            return new ApiResponseWithData(false, jso);
        }
    }

    public static Optional<String> post(String route, JsonObject uniqueIdentifier, Object data) {
        if (!checkApiPrerequisites()) return Optional.of("API token or URL empty");
        CastiaConfig config = CastiaUtils.getConfig();
        Gson gson = new Gson();

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(config.apiUrl + route);
            com.google.gson.JsonElement inputJson = gson.toJsonTree(data);

            JsonObject object = new JsonObject();
            object.add("uniqueIdentifier", uniqueIdentifier);
            object.add("data", inputJson);

            StringEntity postingString = new StringEntity(object.toString());
            post.setEntity(postingString);
            post.setHeader("Content-type", "application/json");
            post.setHeader("Authorization","Bearer " + config.token);
            HttpResponse response = httpClient.execute(post);
            String responseString = new BasicResponseHandler().handleResponse(response);
            ApiDefaultResponse json = gson.fromJson(responseString, RequestService.ApiDefaultResponse.class);
            if (json.success) {
                return Optional.empty();
            }
            ApiResponseWithData errorJson = gson.fromJson(responseString, RequestService.ApiResponseWithData.class);

            return Optional.of(gson.toJson(errorJson.data));
        } catch (IOException e) {
            return Optional.of(e.getMessage());
        }
    }

    public static Optional<String> delete(String route, JsonObject uniqueIdentifier) {
        if (!checkApiPrerequisites()) return Optional.of("API token or URL empty");
        CastiaConfig config = CastiaUtils.getConfig();
        Gson gson = new Gson();

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpDeleteWithBody delete = new HttpDeleteWithBody(config.apiUrl + route);

            JsonObject object = new JsonObject();
            object.add("uniqueIdentifier", uniqueIdentifier);

            StringEntity postingString = new StringEntity(object.toString());
            delete.setEntity(postingString);
            delete.setHeader("Content-type", "application/json");
            delete.setHeader("Authorization","Bearer " + config.token);
            HttpResponse response = httpClient.execute(delete);
            String responseString = new BasicResponseHandler().handleResponse(response);
            ApiDefaultResponse json = gson.fromJson(responseString, RequestService.ApiDefaultResponse.class);
            if (json.success) {
                return Optional.empty();
            }
            ApiResponseWithData errorJson = gson.fromJson(responseString, RequestService.ApiResponseWithData.class);

            return Optional.of(gson.toJson(errorJson.data));
        } catch (IOException e) {
            return Optional.of(e.getMessage());
        }
    }

    private static boolean checkApiPrerequisites() {
        CastiaConfig config = CastiaUtils.getConfig();

        if (config.apiUrl.isEmpty()) return false;
        return !config.token.isEmpty();
    }

    private static class ApiDefaultResponse {
        private boolean success;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ApiResponseWithData {
        private boolean success;
        private com.google.gson.JsonElement data;
    }

    // https://daweini.wordpress.com/2013/12/20/apache-httpclient-send-entity-body-in-a-http-delete-request/
    @NotThreadSafe
    private static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        public String getMethod() {
            return "DELETE";
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }
    }

    @NotThreadSafe
    private static class HttpGetWithBody extends HttpEntityEnclosingRequestBase {
        public String getMethod() {
            return "GET";
        }

        public HttpGetWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }
    }
}
