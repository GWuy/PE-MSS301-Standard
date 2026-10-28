package fu.mss301.se194638.enrollmentservice.controller;

import fu.mss301.se194638.enrollmentservice.common.ApiResponseDTO;
import fu.mss301.se194638.enrollmentservice.dto.EnrollmentDTO;
import fu.mss301.se194638.enrollmentservice.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnrollmentController {

    EnrollmentService enrollmentService;

    @PostMapping
    public ApiResponseDTO createEnrollment(@Valid @RequestBody EnrollmentDTO enrollmentDTO) {
        return ApiResponseDTO.success(1, "Create new enrollment successfully", enrollmentService.createEnrollment(enrollmentDTO));
    }

    @GetMapping("/{enrollmentId}")
    public ApiResponseDTO getEnrollmentById(@PathVariable Long enrollmentId) {
        return ApiResponseDTO.success(1, "Get Enrollment by ID", enrollmentService.getEnrollmentById(enrollmentId));
    }

    @PutMapping("/{enrollmentId}")
    public ApiResponseDTO updateEnrollment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody EnrollmentDTO updateEnrollmentDTO
    ) {
        return ApiResponseDTO.success(1, "Update Enrollment successfully", enrollmentService.updateEnrollment(enrollmentId, updateEnrollmentDTO));
    }

    @GetMapping
    public ApiResponseDTO filterEnrollments(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "") String studentEmail,
            @RequestParam(defaultValue = "") String status
    ) {
        return ApiResponseDTO.success(1, "", enrollmentService.getEnrollmentList(page, size, studentEmail, status));
    }
}
