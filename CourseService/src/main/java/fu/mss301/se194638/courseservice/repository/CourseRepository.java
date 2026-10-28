package fu.mss301.se194638.courseservice.repository;

import fu.mss301.se194638.courseservice.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    boolean existsCourseByCourseCode(String courseCode);

    Course findCourseById(Integer id);

    @Query("""
    SELECT c FROM Course c
    WHERE (:title IS NULL OR LOWER(c.title) = LOWER(:title))
      AND (:status IS NULL OR LOWER(c.status) = LOWER(:status))
    """)
    Page<Course> findAllFiltered(
            @Param("title") String title,
            @Param("status") String status,
            Pageable pageable
    );
}
