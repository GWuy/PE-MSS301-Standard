package fu.mss301.se194638.enrollmentservice.service.impl;

import feign.FeignException;
import fu.mss301.se194638.enrollmentservice.common.AppException;
import fu.mss301.se194638.enrollmentservice.dto.EnrollmentDTO;
import fu.mss301.se194638.enrollmentservice.dto.EnrollmentDetailDTO;
import fu.mss301.se194638.enrollmentservice.dto.PageDTO;
import fu.mss301.se194638.enrollmentservice.entity.Enrollment;
import fu.mss301.se194638.enrollmentservice.entity.EnrollmentStatus;
import fu.mss301.se194638.enrollmentservice.mapper.EnrollmentMapper;
import fu.mss301.se194638.enrollmentservice.openfiegn.controller.CourseFeignClient;
import fu.mss301.se194638.enrollmentservice.openfiegn.dto.CourseDTO;
import fu.mss301.se194638.enrollmentservice.repository.EnrollmentRepository;
import fu.mss301.se194638.enrollmentservice.service.EnrollmentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnrollmentServiceImpl implements EnrollmentService {

    EnrollmentRepository enrollmentRepository;

    CourseFeignClient courseFeignClient;

    EnrollmentMapper enrollmentMapper;

    @Override
    public EnrollmentDTO createEnrollment(EnrollmentDTO request) {

        validateRequest(request);

        if (request.courseId() == null) {
            throw new AppException(2, "Course ID is require");
        }

        CourseDTO courseDTO;

        try {
            courseDTO = courseFeignClient.getCourseById(request.courseId().intValue());
        } catch (FeignException.NotFound e) {
            throw new AppException(2, "Course not found");
        } catch (FeignException e) {
            throw new AppException(2, "Course Service is temporarily unavailable");
        }

        if (courseDTO == null) {
            throw new AppException(2, "Course not found");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(request.studentId());
        enrollment.setStudentName(request.studentName());
        enrollment.setStudentEmail(request.studentEmail());
        enrollment.setEnrollmentDate(request.enrollmentDate());
        enrollment.setStatus(EnrollmentStatus.ACTIVE.getValue());
        enrollment.setGrade(enrollment.getGrade());
        enrollment.setCourseId(courseDTO.courseId());

        enrollmentRepository.save(enrollment);

        return enrollmentMapper.toEnrollmentDto(enrollment);
    }

    @Override
    public EnrollmentDTO updateEnrollment(Long enrollmentId, EnrollmentDTO request) {

        Enrollment enrollmentUpdate = enrollmentRepository.findEnrollmentByEnrollmentId(enrollmentId);
        if (enrollmentUpdate == null) {
            throw new AppException(4, "Enrollment not found");
        }

        validateRequest(request);

        CourseDTO courseDTO;

        try {
            courseDTO = courseFeignClient.getCourseById(request.courseId().intValue());
        } catch (FeignException.NotFound e) {
            throw new AppException(2, "Course not found");
        } catch (FeignException e) {
            throw new AppException(2, "Course Service is temporarily unavailable");
        }

        if (courseDTO == null) {
            throw new AppException(2, "Course not found");
        }

        enrollmentUpdate.setStudentId(request.studentId());
        enrollmentUpdate.setStudentName(request.studentName());
        enrollmentUpdate.setStudentEmail(request.studentEmail());
        enrollmentUpdate.setEnrollmentDate(request.enrollmentDate());
        enrollmentUpdate.setStatus(EnrollmentStatus.ACTIVE.getValue());
        enrollmentUpdate.setGrade(enrollmentUpdate.getGrade());
        enrollmentUpdate.setCourseId(courseDTO.courseId());

        enrollmentRepository.save(enrollmentUpdate);

        return enrollmentMapper.toEnrollmentDto(enrollmentUpdate);
    }

    @Override
    public EnrollmentDTO cancelEnrollment(Long enrollmentId) {

        Enrollment enrollmentCancel = enrollmentRepository.findEnrollmentByEnrollmentId(enrollmentId);
        if (enrollmentCancel == null) {
            throw new AppException(4, "Enrollment not found");
        }

        enrollmentCancel.setStatus(EnrollmentStatus.CANCELLED.getValue());

        enrollmentRepository.save(enrollmentCancel);

        return enrollmentMapper.toEnrollmentDto(enrollmentCancel);
    }

    @Override
    public EnrollmentDetailDTO getEnrollmentById(Long enrollmentId) {

        Enrollment enrollmentDetail = enrollmentRepository.findEnrollmentByEnrollmentId(enrollmentId);
        if (enrollmentDetail == null) {
            throw new AppException(4, "Enrollment not found");
        }

        return enrollmentMapper.toEnrollDetailDto(enrollmentDetail);
    }

    @Override
    public PageDTO<EnrollmentDTO> getEnrollmentList(Integer page, Integer size, String studentName, String status) {

        int pageNumber = (page == null || page < 0) ? 0 : page;
        int pageSize = (size == null || size <= 0) ? 10 : Math.min(size, 100);

        String normalizedStudentName =
                studentName == null || studentName.isBlank() ? null : studentName.trim();

        String normalizedStatus =
                status == null || status.isBlank() ? null : status.trim();

        Pageable pageable =  PageRequest.of(pageNumber, pageSize);

        Page<Enrollment> roomPage = enrollmentRepository.findAllFiltered(
                normalizedStudentName,
                normalizedStatus,
                pageable
        );

        List<EnrollmentDTO> content = roomPage.getContent()
                .stream()
                .map(enrollmentMapper::toEnrollmentDto)
                .toList();

        return PageDTO.<EnrollmentDTO>builder()
                .page(roomPage.getNumber())
                .size(roomPage.getSize())
                .totalPages(roomPage.getTotalPages())
                .totalElements(roomPage.getTotalElements())
                .first(roomPage.isFirst())
                .last(roomPage.isLast())
                .content(content)
                .build();
    }

    private void validateRequest(EnrollmentDTO request) {
        if (request.studentId() == null || request.studentId() < 0) {
            throw new AppException(2, "Student ID is require");
        }

        if (request.studentName().isEmpty() || request.studentName().isBlank()) {
            throw new AppException(2, "Student Name is require");
        }

        if (request.studentEmail().isEmpty() || request.studentEmail().isBlank()) {
            throw new AppException(2, "Student email is require");
        }

        if (request.enrollmentDate() == null) {
            throw new AppException(2, "Enrollment date is require");
        }

        if ((request.grade() != null && !request.grade().matches("^a|A|b|B|c|C|d|D|f|F$")) || Objects.requireNonNull(request.grade()).isBlank()) {
            throw new AppException(2, "Grade must be A, B, C, D, F");
        }

    }
}
