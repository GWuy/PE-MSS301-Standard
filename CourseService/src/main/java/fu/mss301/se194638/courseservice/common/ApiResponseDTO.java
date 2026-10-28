package fu.mss301.se194638.courseservice.common;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponseDTO {

    int status;
    String message;
    Object data;

    public static ApiResponseDTO success(int status, String message, Object data) {
        return new ApiResponseDTO(status, message, data);
    }

    public static ApiResponseDTO error(int status) {
        return error(status, (String) null);
    }

    public static ApiResponseDTO error(int status, String message) {
        return new ApiResponseDTO(
                status,
                message != null && !message.isBlank()
                        ? message
                        : resolveMessage(status),
                null
        );
    }

    public static ApiResponseDTO error(int status, String message, Object data) {
        return new ApiResponseDTO(
                status,
                message != null && !message.isBlank()
                        ? message
                        : resolveMessage(status),
                data
        );
    }

    private static String resolveMessage(int status) {
        return switch (status) {
            case 1 -> "Successful";
            case 2 -> "Data validation failed";
            case 3 -> "Code is duplicated";
            case 4 -> "Resource not found";
            case 5 -> "Resource is not available";
            default -> "Internal server error";
        };
    }
}