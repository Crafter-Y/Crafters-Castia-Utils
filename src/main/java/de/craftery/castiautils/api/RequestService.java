package de.craftery.castiautils.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.craftery.castiautils.CastiaUtils;
import de.craftery.castiautils.CastiaUtilsException;
import de.craftery.castiautils.config.CastiaConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Jankson;
import com.google.gson.JsonElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.Nullable;
import oshi.annotation.concurrent.NotThreadSafe;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

public class RequestService {
    public static void put(String route, Object data) throws CastiaUtilsException {
        CastiaConfig config = CastiaUtils.getConfig();
        HttpPut put = new HttpPut(config.apiUrl + route);
        perform(put, false, null, data);
    }

    public static JsonElement get(String route, Object data) throws CastiaUtilsException {
        CastiaConfig config = CastiaUtils.getConfig();
        HttpGetWithBody get = new HttpGetWithBody(config.apiUrl + route);
        return perform(get, true, null, data);
    }

    public static JsonElement get(String route) throws CastiaUtilsException {
        return get(route, null);
    }

    public static void post(String route, JsonObject uniqueIdentifier, Object data) throws CastiaUtilsException {
        CastiaConfig config = CastiaUtils.getConfig();
        HttpPost post = new HttpPost(config.apiUrl + route);
        perform(post, false, uniqueIdentifier, data);
    }

    public static void delete(String route, JsonObject uniqueIdentifier) throws CastiaUtilsException {
        CastiaConfig config = CastiaUtils.getConfig();
        HttpDeleteWithBody delete = new HttpDeleteWithBody(config.apiUrl + route);
        perform(delete, false, uniqueIdentifier, null);
    }

    private static @Nullable JsonElement perform(HttpEntityEnclosingRequestBase request, boolean expectResponse, JsonObject uniqueIdentifier, Object data) throws CastiaUtilsException {
        CastiaConfig config = CastiaUtils.getConfig();
        if (config.apiUrl.isEmpty() || config.token.isEmpty()) throw new CastiaUtilsException("API token or URL empty");

        Gson gson = new Gson();

        try {
            if (uniqueIdentifier != null) {
                JsonObject requestBody = new JsonObject();
                requestBody.add("uniqueIdentifier", uniqueIdentifier);
                if (data != null) {
                    JsonElement inputJson = gson.toJsonTree(data);
                    requestBody.add("data", inputJson);
                }
                StringEntity requestBodyString = new StringEntity(requestBody.toString());
                request.setEntity(requestBodyString);
            } else if (data != null) {
                Jankson jankson = Jankson.builder().build();
                me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.JsonElement inputJson = jankson.toJson(data);
                StringEntity postingString = new StringEntity(inputJson.toJson());
                request.setEntity(postingString);
            }
        } catch (UnsupportedEncodingException e) {
            throw new CastiaUtilsException("Could not encode request: " + e.getMessage());
        }

        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        request.setHeader("Authorization","Bearer " + config.token);

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response = httpClient.execute(request);
            String responseString = new BasicResponseHandler().handleResponse(response);
            ApiDefaultResponse json = gson.fromJson(responseString, RequestService.ApiDefaultResponse.class);

            if (json.success && !expectResponse) {
                return null;
            }

            ApiResponseWithData responseObject = gson.fromJson(responseString, RequestService.ApiResponseWithData.class);

            if (!responseObject.success) {
                throw new CastiaUtilsException(responseObject.data.toString());
            }

            return responseObject.data;
        } catch (IOException e) {
            throw new CastiaUtilsException("Could not execute request: " + e.getMessage());
        }
    }

    private static class ApiDefaultResponse {
        private boolean success;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ApiResponseWithData {
        private boolean success;
        private JsonElement data;
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
