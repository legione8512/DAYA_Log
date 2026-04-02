package ro.daya.dayalog.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.daya.dayalog.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @EntityGraph(attributePaths = { "actorUser" })
    @Query(value = """
        select al
        from AuditLog al
        where al.studio.id = :studioId
          and (:entityName is null or al.entityName = :entityName)
          and (:action is null or al.action = :action)
          and (:fromAt is null or al.createdAt >= :fromAt)
          and (:toAt is null or al.createdAt < :toAt)
        """,
        countQuery = """
        select count(al)
        from AuditLog al
        where al.studio.id = :studioId
          and (:entityName is null or al.entityName = :entityName)
          and (:action is null or al.action = :action)
          and (:fromAt is null or al.createdAt >= :fromAt)
          and (:toAt is null or al.createdAt < :toAt)
        """)
    Page<AuditLog> search(UUID studioId,
                          String entityName,
                          String action,
                          OffsetDateTime fromAt,
                          OffsetDateTime toAt,
                          Pageable pageable);

    @EntityGraph(attributePaths = { "actorUser", "studio" })
    Optional<AuditLog> findByIdAndStudioId(UUID id, UUID studioId);
}