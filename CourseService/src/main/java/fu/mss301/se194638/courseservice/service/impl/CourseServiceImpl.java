package fu.mss301.se194638.courseservice.service.impl;

import fu.mss301.se194638.courseservice.common.ApiResponseDTO;
import fu.mss301.se194638.courseservice.common.AppException;
import fu.mss301.se194638.courseservice.common.PageDTO;
import fu.mss301.se194638.courseservice.dto.CourseDTO;
import fu.mss301.se194638.courseservice.dto.CourseStatus;
import fu.mss301.se194638.courseservice.entity.Course;
import fu.mss301.se194638.courseservice.mapper.CourseMapper;
import fu.mss301.se194638.courseservice.repository.CourseRepository;
import fu.mss301.se194638.courseservice.service.CourseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseServiceImpl implements CourseService {

    CourseRepository courseRepository;

    CourseMapper courseMapper;

    @Override
    public CourseDTO getCourseById(Integer id) {

        Course course = courseRepository.findCourseById(id);

        if (course == null) {
            throw new AppException(2, "Course not found");
        }

        return courseMapper.toCourseDTO(courseRepository.findCourseById(id));
    }

    @Override
    public CourseDTO createCourse(CourseDTO courseDTO) {

        if (courseDTO.courseCode() == null || courseDTO.courseCode().isBlank()) {
            throw new AppException(2, "Course code is required");
        }

        if (courseDTO.title() == null || courseDTO.title().isBlank()) {
            throw new AppException(2, "Title is required");
        }

        if (courseDTO.description() == null || courseDTO.description().isBlank()) {
            throw new AppException(2, "Description is required");
        }

        if (courseDTO.credits() == null || String.valueOf(courseDTO.credits()).isEmpty()) {
            throw new AppException(2, "Credits is required");
        }

        if (courseDTO.startDate() == null) {
            throw new AppException(2, "Start date is required");
        }

        if (courseDTO.endDate() == null) {
            throw new AppException(2, "End date is required");
        }

        if (courseRepository.existsCourseByCourseCode(courseDTO.courseCode().trim())) {
            throw new AppException(3, "Course already exists");
        }

        if (courseDTO.credits() > 10 || courseDTO.credits() < 1) {
            throw new AppException(2, "Credits must be between 1 and 10");
        }

        if (courseDTO.endDate().before(courseDTO.startDate())) {
            throw new AppException(2, "End date must be after start date");
        }

        Course course = new Course();
        course.setCourseCode(courseDTO.courseCode().trim());
        course.setTitle(courseDTO.title());
        course.setDescription(courseDTO.description());
        course.setCredits(courseDTO.credits());
        course.setStartDate(courseDTO.startDate());
        course.setEndDate(courseDTO.endDate());
        course.setInstructorId(courseDTO.instructorId());
        course.setStatus(CourseStatus.DRAFT.getStatus());

        return courseMapper.toCourseDTO(courseRepository.save(course));
    }

    @Override
    public CourseDTO updateCourse(Integer id, CourseDTO courseDTO) {

        Course course = courseRepository.findCourseById(id);

        if (courseDTO.courseCode() == null || courseDTO.courseCode().isBlank()) {
            throw new AppException(2, "Course code is required");
        }

        if (courseDTO.title() == null || courseDTO.title().isBlank()) {
            throw new AppException(2, "Title is required");
        }

        if (courseDTO.description() == null || courseDTO.description().isBlank()) {
            throw new AppException(2, "Description is required");
        }

        if (courseDTO.credits() == null || String.valueOf(courseDTO.credits()).isEmpty()) {
            throw new AppException(2, "Credits is required");
        }

        if (courseDTO.startDate() == null) {
            throw new AppException(2, "Start date is required");
        }

        if (courseDTO.endDate() == null) {
            throw new AppException(2, "End date is required");
        }

        if (courseRepository.existsCourseByCourseCode(courseDTO.courseCode().trim())) {
            throw new AppException(3, "Course already exists");
        }

        if (courseDTO.credits() > 10 || courseDTO.credits() < 1) {
            throw new AppException(2, "Credits must be between 1 and 10");
        }

        if (courseDTO.endDate().before(courseDTO.startDate())) {
            throw new AppException(2, "End date must be after start date");
        }

        course.setCourseCode(courseDTO.courseCode().trim());
        course.setTitle(courseDTO.title());
        course.setDescription(courseDTO.description());
        course.setCredits(courseDTO.credits());
        course.setStartDate(courseDTO.startDate());
        course.setEndDate(courseDTO.endDate());
        course.setInstructorId(courseDTO.instructorId());
        course.setStatus(CourseStatus.DRAFT.getStatus());

        return courseMapper.toCourseDTO(courseRepository.save(course));
    }

    @Override
    public CourseDTO deleteCourse(Integer id) {

        Course course = courseRepository.findCourseById(id);

        if (course == null) {
            throw new AppException(2, "Course not found");
        }

        course.setStatus(CourseStatus.CLOSED.getStatus());

        return courseMapper.toCourseDTO(courseRepository.save(course));
    }

    @Override
    public PageDTO<CourseDTO> getAllCourses(
            Integer page,
            Integer size,
            String title,
            String status
    ) {
        int pageNumber = (page == null || page < 0) ? 0 : page;
        int pageSize = (size == null || size <= 0) ? 10 : Math.min(size, 100);

        String normalizedTitle =
                title == null || title.isBlank() ? null : title.trim();

        String normalizedStatus =
                status == null || status.isBlank() ? null : status.trim();

        Pageable pageable =  PageRequest.of(pageNumber, pageSize);

        Page<Course> coursePage = courseRepository.findAllFiltered(
                normalizedTitle,
                normalizedStatus,
                pageable
        );

        List<CourseDTO> content = coursePage.getContent()
                .stream()
                .map(courseMapper::toCourseDTO)
                .toList();

        return PageDTO.<CourseDTO>builder()
                .page(coursePage.getNumber())
                .size(coursePage.getSize())
                .totalPages(coursePage.getTotalPages())
                .totalElements(coursePage.getTotalElements())
                .first(coursePage.isFirst())
                .last(coursePage.isLast())
                .content(content)
                .build();
    }


}
