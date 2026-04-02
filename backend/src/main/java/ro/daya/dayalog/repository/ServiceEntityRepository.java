package ro.daya.dayalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.daya.dayalog.entity.ServiceEntity;

public interface ServiceEntityRepository extends JpaRepository<ServiceEntity, UUID> {

    List<ServiceEntity> findByStudioIdOrderByNameAsc(UUID studioId);

    List<ServiceEntity> findByStudioIdAndActiveOrderByNameAsc(UUID studioId, Boolean active);

    Optional<ServiceEntity> findByIdAndStudioId(UUID id, UUID studioId);

    boolean existsByStudioIdAndNameIgnoreCase(UUID studioId, String name);

    boolean existsByStudioIdAndNameIgnoreCaseAndIdNot(UUID studioId, String name, UUID id);

    @Query("""
        select s
        from ServiceEntity s
        where s.studio.id = :studioId
          and (:active is null or s.active = :active)
          and (
                lower(s.name) like lower(concat('%', :query, '%'))
             or lower(coalesce(s.description, '')) like lower(concat('%', :query, '%'))
          )
        order by s.name asc
    """)
    List<ServiceEntity> searchByStudioIdAndFilters(UUID studioId, String query, Boolean active);
    
    long countByStudioIdAndActive(UUID studioId, Boolean active);
}