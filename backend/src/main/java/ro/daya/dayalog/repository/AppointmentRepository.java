package ro.daya.dayalog.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.daya.dayalog.entity.Appointment;
import ro.daya.dayalog.entity.enums.AppointmentStatus;
import ro.daya.dayalog.entity.enums.AppointmentType;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @EntityGraph(attributePaths = { "service", "instructor", "resource", "participants", "participants.client" })
    Optional<Appointment> findByIdAndStudioId(UUID id, UUID studioId);

    @EntityGraph(attributePaths = { "service", "instructor", "resource", "participants" })
    List<Appointment> findByStudioIdOrderByStartAtAsc(UUID studioId);

    @EntityGraph(attributePaths = { "service", "instructor", "resource" })
    @Query(value = """
        select distinct a
        from Appointment a
        left join a.participants p
        where a.studio.id = :studioId
          and (:fromAt is null or a.startAt >= :fromAt)
          and (:toAt is null or a.startAt < :toAt)
          and (:status is null or a.status = :status)
          and (:appointmentType is null or a.appointmentType = :appointmentType)
          and (:serviceId is null or a.service.id = :serviceId)
          and (:instructorId is null or a.instructor.id = :instructorId)
          and (:clientId is null or p.client.id = :clientId)
        """,
        countQuery = """
        select count(distinct a.id)
        from Appointment a
        left join a.participants p
        where a.studio.id = :studioId
          and (:fromAt is null or a.startAt >= :fromAt)
          and (:toAt is null or a.startAt < :toAt)
          and (:status is null or a.status = :status)
          and (:appointmentType is null or a.appointmentType = :appointmentType)
          and (:serviceId is null or a.service.id = :serviceId)
          and (:instructorId is null or a.instructor.id = :instructorId)
          and (:clientId is null or p.client.id = :clientId)
        """)
    Page<Appointment> searchForList(UUID studioId,
                                    OffsetDateTime fromAt,
                                    OffsetDateTime toAt,
                                    AppointmentStatus status,
                                    AppointmentType appointmentType,
                                    UUID serviceId,
                                    UUID instructorId,
                                    UUID clientId,
                                    Pageable pageable);

    @Query("""
        select a
        from Appointment a
        where a.studio.id = :studioId
          and a.status <> :cancelledStatus
          and a.instructor.id = :instructorId
          and a.startAt < :newEnd
          and a.endAt > :newStart
          and (:ignoreAppointmentId is null or a.id <> :ignoreAppointmentId)
    """)
    List<Appointment> findInstructorOverlaps(UUID studioId,
                                             UUID instructorId,
                                             OffsetDateTime newStart,
                                             OffsetDateTime newEnd,
                                             UUID ignoreAppointmentId,
                                             AppointmentStatus cancelledStatus);

    @Query("""
        select distinct a
        from Appointment a
        join a.participants p
        where a.studio.id = :studioId
          and a.status <> :cancelledStatus
          and p.client.id = :clientId
          and a.startAt < :newEnd
          and a.endAt > :newStart
          and (:ignoreAppointmentId is null or a.id <> :ignoreAppointmentId)
    """)
    List<Appointment> findClientOverlaps(UUID studioId,
                                         UUID clientId,
                                         OffsetDateTime newStart,
                                         OffsetDateTime newEnd,
                                         UUID ignoreAppointmentId,
                                         AppointmentStatus cancelledStatus);

    @Query("""
        select a
        from Appointment a
        where a.studio.id = :studioId
          and a.status <> :cancelledStatus
          and a.resource.id = :resourceId
          and a.startAt < :newEnd
          and a.endAt > :newStart
          and (:ignoreAppointmentId is null or a.id <> :ignoreAppointmentId)
    """)
    List<Appointment> findResourceOverlaps(UUID studioId,
                                           UUID resourceId,
                                           OffsetDateTime newStart,
                                           OffsetDateTime newEnd,
                                           UUID ignoreAppointmentId,
                                           AppointmentStatus cancelledStatus);

    @EntityGraph(attributePaths = { "service", "instructor", "resource", "participants", "participants.client" })
    @Query("""
        select distinct a
        from Appointment a
        left join fetch a.participants p
        left join fetch p.client
        where a.studio.id = :studioId
          and a.appointmentType = :groupType
          and a.status <> :cancelledStatus
          and a.service.id = :serviceId
          and a.instructor.id = :instructorId
          and a.startAt = :startAt
          and a.endAt = :endAt
          and (:ignoreAppointmentId is null or a.id <> :ignoreAppointmentId)
    """)
    List<Appointment> findCompatibleGroupSessions(UUID studioId,
                                                  UUID serviceId,
                                                  UUID instructorId,
                                                  OffsetDateTime startAt,
                                                  OffsetDateTime endAt,
                                                  UUID ignoreAppointmentId,
                                                  AppointmentType groupType,
                                                  AppointmentStatus cancelledStatus);

    @EntityGraph(attributePaths = { "service", "instructor", "resource", "participants", "participants.client" })
    @Query("""
        select distinct a
        from Appointment a
        join a.participants p
        where a.studio.id = :studioId
          and p.client.user.id = :userId
          and a.status <> :cancelledStatus
          and a.startAt >= :now
        order by a.startAt asc
    """)
    List<Appointment> findFutureForClientUser(UUID studioId,
                                              UUID userId,
                                              OffsetDateTime now,
                                              AppointmentStatus cancelledStatus);

    @EntityGraph(attributePaths = { "service", "instructor", "resource", "participants", "participants.client" })
    @Query("""
        select distinct a
        from Appointment a
        join a.participants p
        where a.studio.id = :studioId
          and p.client.user.id = :userId
          and (a.startAt < :now or a.status = :cancelledStatus)
        order by a.startAt desc
    """)
    List<Appointment> findHistoryForClientUser(UUID studioId,
                                               UUID userId,
                                               OffsetDateTime now,
                                               AppointmentStatus cancelledStatus);

    @EntityGraph(attributePaths = { "service", "instructor", "resource", "participants", "participants.client" })
    @Query("""
        select distinct a
        from Appointment a
        join a.participants p
        where a.studio.id = :studioId
          and p.client.id = :clientId
          and a.status <> :cancelledStatus
          and a.startAt >= :now
        order by a.startAt asc
    """)
    List<Appointment> findFutureForClientAdmin(UUID studioId,
                                               UUID clientId,
                                               OffsetDateTime now,
                                               AppointmentStatus cancelledStatus);

    @EntityGraph(attributePaths = { "service", "instructor", "resource", "participants", "participants.client" })
    @Query("""
        select distinct a
        from Appointment a
        join a.participants p
        where a.studio.id = :studioId
          and p.client.id = :clientId
          and (a.startAt < :now or a.status = :cancelledStatus)
        order by a.startAt desc
    """)
    List<Appointment> findHistoryForClientAdmin(UUID studioId,
                                                UUID clientId,
                                                OffsetDateTime now,
                                                AppointmentStatus cancelledStatus);

    @EntityGraph(attributePaths = { "service", "instructor", "resource" })
    @Query("""
        select distinct a
        from Appointment a
        join a.participants p
        where a.studio.id = :studioId
          and p.client.user.id = :userId
          and a.status <> :cancelledStatus
          and a.startAt >= :now
        order by a.startAt asc
    """)
    List<Appointment> findNextForClientUser(UUID studioId,
                                            UUID userId,
                                            OffsetDateTime now,
                                            AppointmentStatus cancelledStatus,
                                            Pageable pageable);

    @Query("""
        select count(distinct a.id)
        from Appointment a
        join a.participants p
        where a.studio.id = :studioId
          and p.client.user.id = :userId
          and a.status <> :cancelledStatus
          and a.startAt >= :now
    """)
    long countFutureForClientUser(UUID studioId,
                                  UUID userId,
                                  OffsetDateTime now,
                                  AppointmentStatus cancelledStatus);

    @Query("""
        select count(distinct a.id)
        from Appointment a
        join a.participants p
        where a.studio.id = :studioId
          and p.client.user.id = :userId
          and (a.startAt < :now or a.status = :cancelledStatus)
    """)
    long countHistoryForClientUser(UUID studioId,
                                   UUID userId,
                                   OffsetDateTime now,
                                   AppointmentStatus cancelledStatus);
    
    @EntityGraph(attributePaths = { "service", "instructor", "resource" })
    @Query("""
        select distinct a
        from Appointment a
        join a.participants p
        where a.studio.id = :studioId
          and p.client.id = :clientId
          and (a.startAt < :now or a.status = :cancelledStatus)
        order by a.startAt desc
    """)
    List<Appointment> findRecentHistoryForClientAdmin(UUID studioId,
                                                      UUID clientId,
                                                      OffsetDateTime now,
                                                      AppointmentStatus cancelledStatus,
                                                      Pageable pageable);
    @Query("""
    	    select count(distinct a.id)
    	    from Appointment a
    	    where a.studio.id = :studioId
    	      and a.status <> :cancelledStatus
    	      and a.startAt >= :fromAt
    	      and a.startAt < :toAt
    	""")
    	long countDashboardAppointmentsBetween(UUID studioId,
    	                                       OffsetDateTime fromAt,
    	                                       OffsetDateTime toAt,
    	                                       AppointmentStatus cancelledStatus);

    	@Query("""
    	    select count(distinct a.id)
    	    from Appointment a
    	    where a.studio.id = :studioId
    	      and a.status <> :cancelledStatus
    	      and a.startAt >= :now
    	""")
    	long countDashboardUpcomingAppointments(UUID studioId,
    	                                        OffsetDateTime now,
    	                                        AppointmentStatus cancelledStatus);
}