package fu.mss301.se194638.enrollmentservice.service;

import fu.mss301.se194638.enrollmentservice.dto.EnrollmentDTO;
import fu.mss301.se194638.enrollmentservice.dto.EnrollmentDetailDTO;
import fu.mss301.se194638.enrollmentservice.dto.PageDTO;
import org.springframework.stereotype.Service;

@Service
public interface EnrollmentService {

    EnrollmentDTO createEnrollment(EnrollmentDTO request);

    EnrollmentDTO updateEnrollment(Long enrollmentId, EnrollmentDTO request);

    EnrollmentDTO cancelEnrollment(Long enrollmentId);

    EnrollmentDetailDTO getEnrollmentById(Long enrollmentId);

    PageDTO<EnrollmentDTO> getEnrollmentList(Integer page, Integer size, String studentName, String status);
}
