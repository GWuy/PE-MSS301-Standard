package fu.mss301.se194638.courseservice.controller;

import fu.mss301.se194638.courseservice.common.ApiResponseDTO;
import fu.mss301.se194638.courseservice.dto.CourseDTO;
import fu.mss301.se194638.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/courses")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CourseController {

    CourseService courseService;

    @PostMapping
    public CourseDTO createCourse(
            @RequestBody CourseDTO courseDTO
    ) {
        return courseService.createCourse(courseDTO);
    }

    @GetMapping("/{courseId}")
    public CourseDTO getCourseById(
            @PathVariable Integer courseId
    ) {
        return courseService.getCourseById(courseId);
    }

    @PutMapping("/{courseId}")
    public CourseDTO updateCourse(
            @PathVariable Integer courseId,
            @RequestBody CourseDTO courseDTO
    ) {
        return courseService.updateCourse(courseId, courseDTO);
    }

    @GetMapping
    public Object getAllCourses(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status
    ) {
        return courseService.getAllCourses(page, size, title, status);
    }
}
