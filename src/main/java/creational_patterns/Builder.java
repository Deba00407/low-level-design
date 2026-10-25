package creational_patterns;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

enum AllowedHTTPMethods {
    GET, POST, DELETE, PATCH, PUT, HEAD
}

final class Validator {

    private static final Set<AllowedHTTPMethods> NO_BODY_METHODS = Set.of(AllowedHTTPMethods.GET, AllowedHTTPMethods.HEAD);
    private static final Set<AllowedHTTPMethods> BODY_METHODS = Set.of(AllowedHTTPMethods.POST, AllowedHTTPMethods.PATCH, AllowedHTTPMethods.PUT, AllowedHTTPMethods.DELETE);

    private Validator() {}

    static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("URL must not be blank");
        }
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new IllegalStateException("URL must start with http:// or https://: " + url);
        }
    }

    static void validateTimeout(int timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalStateException("Timeout must be positive, got " + timeoutMs);
        }
    }

    static void validateRetries(int retries) {
        if (retries < 0 || retries > 5) {
            throw new IllegalStateException("Retries must be between 0 and 5, got " + retries);
        }
    }

    static void validateBody(AllowedHTTPMethods method, String body, String contentType) {
        boolean hasBody = body != null && !body.isEmpty();

        if (NO_BODY_METHODS.contains(method) && hasBody) {
            throw new IllegalStateException(method + " request must not have a body");
        }
        if (BODY_METHODS.contains(method) && hasBody
                && (contentType == null || contentType.isBlank())) {
            throw new IllegalStateException(method + " request with a body requires a content type");
        }
    }

    static void validateAuthorizationToken(String authToken){
        if(authToken == null) return;

        if (authToken.isBlank()) {
            throw new IllegalStateException("Authorization token must not be null or empty");
        }

        String[] tokenParts = authToken.split(" ");
        if(tokenParts.length < 2){
            throw new IllegalArgumentException("Invalid authorization token format");
        }

        if(!tokenParts[0].equals("Bearer")){
            throw new IllegalArgumentException("Invalid authorization token format. Bearer prefix is needed");
        }

        if(tokenParts[1].isBlank()){
            throw new IllegalArgumentException("Invalid authorization token format. Token value is missing");
        }
    }
}

final class HttpRequest{
    // Required fields
    private final AllowedHTTPMethods method;
    private final String url;

    // Optional fields
    private final String body;
    private final String contentType;
    private final String authenticationToken;
    private final int timeoutInMilliseconds;
    private final int retries;

    private final Map<String, String> headers;
    private final Map<String, String> queryParams;

    private static boolean validateInput(builder requestBuilder){
        Validator.validateUrl(requestBuilder.url);
        Validator.validateBody(requestBuilder.method, requestBuilder.body, requestBuilder.contentType);
        Validator.validateTimeout(requestBuilder.timeoutInMilliseconds);
        Validator.validateRetries(requestBuilder.retries);
        Validator.validateAuthorizationToken(requestBuilder.authenticationToken);

        return true;
    }

    private HttpRequest(builder requestBuilder){

        if(!validateInput(requestBuilder)){
            throw new IllegalArgumentException("Invalid request inputs");
        }

        this.method = requestBuilder.method;
        this.url = requestBuilder.url;
        this.headers = Map.copyOf(requestBuilder.headers);
        this.queryParams = Map.copyOf(requestBuilder.queryParams);
        this.body = requestBuilder.body;
        this.contentType = requestBuilder.contentType;
        this.timeoutInMilliseconds = requestBuilder.timeoutInMilliseconds;
        this.retries = requestBuilder.retries;
        this.authenticationToken = requestBuilder.authenticationToken;
    }

    static class builder{
        // Mandatory
        private final AllowedHTTPMethods method;
        private final String url;

        // Optional
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String> queryParams = new HashMap<>();
        private String body = "";
        private String contentType = "applicaiton/json";
        private int timeoutInMilliseconds = 30_000;
        private int retries = 0;
        private String authenticationToken = "";

        builder(AllowedHTTPMethods method, String url){
            this.method = method;
            this.url = url;
        }

        public builder headers(Map<String, String> headers){
            for(String key : headers.keySet()){
                this.headers.put(key.toLowerCase(), headers.get(key));
            }

            return this;
        }

        public builder queryParams(Map<String, String> queryParams){
            for(String key : queryParams.keySet()){
                this.queryParams.put(key.toLowerCase(), queryParams.get(key)); // make sure the keys are case-insensitive by converting all to lowercase
            }
            return this;
        }

        public builder body(String body){
            this.body = body;
            return this;
        }

        public builder contentType(String contentType){
            this.contentType = contentType;
            return this;
        }

        public builder authenticationToken(String authenticationToken){
            this.authenticationToken = authenticationToken;
            return this;
        }

        public builder timeoutInMilliseconds(int timeoutInMilliseconds){
            this.timeoutInMilliseconds = timeoutInMilliseconds;
            return this;
        }

        public builder retries(int retries){
            this.retries = retries;
            return this;
        }

        public HttpRequest build(){
            return new HttpRequest(this);
        }
    }

    String getFullUrl() {
        if (queryParams.isEmpty()) return url;

        StringJoiner sj = new StringJoiner("&", "?", "");
        for (String key : queryParams.keySet()) {
            sj.add(URLEncoder.encode(key, StandardCharsets.UTF_8)
                    + "=" + URLEncoder.encode(queryParams.get(key), StandardCharsets.UTF_8));
        }
        return url + sj;
    }

    @Override
    public String toString(){
        return String.format("HttpRequest{method=%s, url=%s, headers=%s, authenticationToken=%s, body=%s, contentType=%s, queryParams=%s, timeoutInMilliseconds=%s, retries=%s}", method, url, headers, authenticationToken, body, contentType, queryParams, timeoutInMilliseconds, retries);
    }
}

public class Builder {

    static void main() {
        HttpRequest request = new HttpRequest.builder(AllowedHTTPMethods.POST, "https://localhost:8080")
                .headers(Map.of("X-forwarded-for", "127.0.0.1"))
                .authenticationToken("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
                .body("This is gonna be a really long body.")
                .contentType("application/json")
                .queryParams(Map.of("id", "123", "name", "John Doe"))
                .timeoutInMilliseconds(1000)
                .retries(3)
                .build();

        System.out.println("The request passed was: " + request.toString());

        System.out.println("Full url is: " + request.getFullUrl());
    }

}
