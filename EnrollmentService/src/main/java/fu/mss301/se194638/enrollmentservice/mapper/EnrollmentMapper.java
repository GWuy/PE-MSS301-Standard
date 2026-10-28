package fu.mss301.se194638.enrollmentservice.mapper;

import fu.mss301.se194638.enrollmentservice.dto.EnrollmentDTO;
import fu.mss301.se194638.enrollmentservice.dto.EnrollmentDetailDTO;
import fu.mss301.se194638.enrollmentservice.entity.Enrollment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    EnrollmentDTO toEnrollmentDto(Enrollment enrollment);

    EnrollmentDetailDTO toEnrollDetailDto(Enrollment enrollment);
}
