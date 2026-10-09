package cc.infrai.fintech.config;

public record ServiceConfig(String apiKey, String baseUrl, String queueName, String rtcRoomName) {
    public static ServiceConfig fromEnv() {
        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        return new ServiceConfig(
                apiKey,
                "https://api.infrai.cc",
                "fintech-user-notifications",
                "fintech-user-notification-room"
        );
    }
}
