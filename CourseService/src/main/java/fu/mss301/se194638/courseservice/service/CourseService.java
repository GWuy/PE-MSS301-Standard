package fu.mss301.se194638.courseservice.service;

import fu.mss301.se194638.courseservice.common.ApiResponseDTO;
import fu.mss301.se194638.courseservice.common.PageDTO;
import fu.mss301.se194638.courseservice.dto.CourseDTO;
import org.springframework.stereotype.Service;

@Service
public interface CourseService {

    CourseDTO getCourseById(Integer id);
    CourseDTO createCourse(CourseDTO courseDTO);
    CourseDTO updateCourse(Integer id, CourseDTO courseDTO);
    CourseDTO deleteCourse(Integer id);
    PageDTO<CourseDTO> getAllCourses(Integer page, Integer size, String title, String status);
}
