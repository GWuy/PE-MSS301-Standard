package fu.mss301.se194638.enrollmentservice.repository;

import fu.mss301.se194638.enrollmentservice.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Enrollment findEnrollmentByEnrollmentId(Long enrollmentId);

    @Query("""
    SELECT e FROM Enrollment e
    WHERE (:studentEmail IS NULL OR LOWER(e.studentEmail) = LOWER(:studentEmail))
      AND (:status IS NULL OR LOWER(e.status) = LOWER(:status))
    """)
    Page<Enrollment> findAllFiltered(
            @Param("studentEmail") String studentEmail,
            @Param("status") String status,
            Pageable pageable
    );
}
