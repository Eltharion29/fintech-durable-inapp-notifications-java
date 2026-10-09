package cc.infrai.fintech.infra;

public class InfraiException extends RuntimeException {
    private final String code;
    private final int status;

    public InfraiException(String code, String message, int status) {
        super(message == null ? "Infrai request failed" : message);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
