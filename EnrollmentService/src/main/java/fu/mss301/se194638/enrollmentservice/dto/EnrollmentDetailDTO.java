package fu.mss301.se194638.enrollmentservice.dto;

import fu.mss301.se194638.enrollmentservice.openfiegn.dto.CourseDTO;

import java.util.Date;

public record EnrollmentDetailDTO(
        Long enrollmentId,
        Long studentId,
        String studentName,
        String studentEmail,
        Date enrollmentDate,
        String status,
        String grade,
        CourseDTO courseDTO
) {
}
