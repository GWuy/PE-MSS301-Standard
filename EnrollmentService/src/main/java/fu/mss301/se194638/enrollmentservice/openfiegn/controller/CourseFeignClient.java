
package fu.mss301.se194638.enrollmentservice.openfiegn.controller;

import fu.mss301.se194638.enrollmentservice.openfiegn.dto.CourseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "CourseService",
        url = "${services.course.url}"
)
public interface CourseFeignClient {

    @GetMapping("/api/courses/{courseId}")
    CourseDTO getCourseById(@PathVariable Integer courseId);

}
