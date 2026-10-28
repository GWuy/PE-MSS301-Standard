package fu.mss301.se194638.enrollmentservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Date;

public record EnrollmentDTO(
        Long enrollmentId,
        @NotNull
        Long studentId,
        @NotNull
        String studentName,
        @NotNull
        String studentEmail,
        @NotNull
        Date enrollmentDate,
        @NotNull
        String status,
        String grade,
        Long courseId
) {
}
