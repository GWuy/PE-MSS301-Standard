package fu.mss301.se194638.courseservice.mapper;

import fu.mss301.se194638.courseservice.dto.CourseDTO;
import fu.mss301.se194638.courseservice.entity.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "courseId", source = "id")
    @Mapping(target = "instructorId", source = "instructorId")
    CourseDTO toCourseDTO(Course course);
}
