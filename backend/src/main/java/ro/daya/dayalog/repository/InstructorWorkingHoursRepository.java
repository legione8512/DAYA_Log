package ro.daya.dayalog.repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.daya.dayalog.entity.InstructorWorkingHours;

public interface InstructorWorkingHoursRepository extends JpaRepository<InstructorWorkingHours, UUID> {

    List<InstructorWorkingHours> findByInstructorIdAndStudioIdAndActiveTrue(UUID instructorId, UUID studioId);

    long deleteByInstructorIdAndStudioId(UUID instructorId, UUID studioId);

    @Query("""
        select iwh
        from InstructorWorkingHours iwh
        where iwh.studio.id = :studioId
          and iwh.instructor.id = :instructorId
          and iwh.active = true
          and iwh.dayOfWeek = :dayOfWeek
        order by iwh.startTime asc
    """)
    List<InstructorWorkingHours> findActiveForDay(UUID studioId,
                                                  UUID instructorId,
                                                  DayOfWeek dayOfWeek);
}