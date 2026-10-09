package cc.infrai.fintech.infra;

public record Envelope(boolean ok, String dataJson, String errorCode, String errorMessage) {
}
