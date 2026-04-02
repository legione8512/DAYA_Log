package ro.daya.dayalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.daya.dayalog.entity.ResourceEntity;

public interface ResourceEntityRepository extends JpaRepository<ResourceEntity, UUID> {

    List<ResourceEntity> findByStudioIdOrderByNameAsc(UUID studioId);

    List<ResourceEntity> findByStudioIdAndActiveOrderByNameAsc(UUID studioId, Boolean active);

    Optional<ResourceEntity> findByIdAndStudioId(UUID id, UUID studioId);

    boolean existsByStudioIdAndNameIgnoreCase(UUID studioId, String name);

    boolean existsByStudioIdAndNameIgnoreCaseAndIdNot(UUID studioId, String name, UUID id);

    @Query("""
        select r
        from ResourceEntity r
        where r.studio.id = :studioId
          and (:active is null or r.active = :active)
          and (
                lower(r.name) like lower(concat('%', :query, '%'))
             or lower(coalesce(r.notes, '')) like lower(concat('%', :query, '%'))
          )
        order by r.name asc
    """)
    List<ResourceEntity> searchByStudioIdAndFilters(UUID studioId, String query, Boolean active);
    
    long countByStudioIdAndActive(UUID studioId, Boolean active);
}