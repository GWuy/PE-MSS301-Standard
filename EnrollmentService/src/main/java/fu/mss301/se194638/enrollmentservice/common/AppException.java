package fu.mss301.se194638.enrollmentservice.common;


import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final int status;

    public AppException(int status, String message) {
        super(message);
        this.status = status;
    }

    public AppException(int status) {
        super(resolveMessage(status));
        this.status = status;
    }

    private static String resolveMessage(int status) {
        return switch (status) {
            case 2 -> "Data validation failed";
            case 3 -> "Code is duplicated";
            case 4 -> "Resource not found";
            case 5 -> "Resource is not available";
            default -> "Internal server error";
        };
    }
}