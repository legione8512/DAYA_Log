package ro.daya.dayalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.daya.dayalog.entity.WaitlistEntry;
import ro.daya.dayalog.entity.enums.WaitlistStatus;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {

    @EntityGraph(attributePaths = { "client" })
    List<WaitlistEntry> findByAppointmentIdAndStudioIdAndStatusOrderByPositionAsc(UUID appointmentId,
                                                                                  UUID studioId,
                                                                                  WaitlistStatus status);

    boolean existsByAppointmentIdAndClientIdAndStatus(UUID appointmentId,
                                                      UUID clientId,
                                                      WaitlistStatus status);

    Optional<WaitlistEntry> findByIdAndAppointmentIdAndStudioIdAndStatus(UUID id,
                                                                         UUID appointmentId,
                                                                         UUID studioId,
                                                                         WaitlistStatus status);

    @Query("""
        select coalesce(max(w.position), 0)
        from WaitlistEntry w
        where w.appointment.id = :appointmentId
          and w.status = :status
    """)
    Integer findMaxPositionByAppointmentIdAndStatus(UUID appointmentId, WaitlistStatus status);
}