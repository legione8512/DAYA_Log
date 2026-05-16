package ro.daya.dayalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.daya.dayalog.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

	@EntityGraph(attributePaths = { "actorUser" })
	@Query("""
			    select al
			    from AuditLog al
			    where al.studio.id = :studioId
			    order by al.createdAt desc
			""")
	List<AuditLog> findForList(UUID studioId);

	@EntityGraph(attributePaths = { "actorUser", "studio" })
	Optional<AuditLog> findByIdAndStudioId(UUID id, UUID studioId);
}