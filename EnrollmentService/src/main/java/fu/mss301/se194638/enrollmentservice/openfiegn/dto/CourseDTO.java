package fu.mss301.se194638.enrollmentservice.openfiegn.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Date;

public record CourseDTO(
        long courseId,
        @NotNull(message = "Course code cannot be null")
        String courseCode,
        @NotNull(message = "Title cannot be null")
        String title,
        @NotNull(message = "Description cannot be null")
        String description,
        @Min(value = 1)
        @Max(value = 10)
        @NotNull(message = "Credits cannot be null")
        Integer credits,
        String status,
        @NotNull(message = "Start date cannot be null")
        Date startDate,
        @NotNull(message = "End date cannot be null")
        Date endDate,
        long instructorId) {
}
