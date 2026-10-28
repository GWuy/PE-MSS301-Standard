package fu.mss301.se194638.enrollmentservice.common;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponseDTO> handleAppException(
            AppException ex) {

        int httpCode = switch (ex.getStatus()) {
            case 2 -> 406;
            case 3 -> 226;
            case 4 -> 404;
            case 5 -> 400;
            default -> 500;
        };

        return ResponseEntity
                .status(httpCode)
                .body(ApiResponseDTO.error(
                        ex.getStatus(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO> handleException(Exception ex) {

        return ResponseEntity
                .status(500)
                .body(ApiResponseDTO.error(
                        0,
                        "Internal server error"
                ));
    }
}