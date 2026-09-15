package xyz.wiseoldblock.companion.api;

public record ApiResponse(int statusCode, String body, boolean success, boolean cooldownActive, String errorMessage) {
    public static ApiResponse success(int statusCode, String body) {
        return new ApiResponse(statusCode, body, true, false, null);
    }

    public static ApiResponse cooldown(int statusCode, String body) {
        return new ApiResponse(statusCode, body, false, true, null);
    }

    public static ApiResponse error(int statusCode, String errorMessage, String body) {
        return new ApiResponse(statusCode, body, false, false, errorMessage);
    }
}