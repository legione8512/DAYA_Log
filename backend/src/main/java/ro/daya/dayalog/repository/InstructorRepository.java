package ro.daya.dayalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.daya.dayalog.entity.Instructor;

public interface InstructorRepository extends JpaRepository<Instructor, UUID> {

    List<Instructor> findByStudioIdOrderByLastNameAscFirstNameAsc(UUID studioId);

    List<Instructor> findByStudioIdAndActiveOrderByLastNameAscFirstNameAsc(UUID studioId, Boolean active);

    Optional<Instructor> findByIdAndStudioId(UUID id, UUID studioId);

    boolean existsByStudioIdAndEmailIgnoreCase(UUID studioId, String email);

    boolean existsByStudioIdAndEmailIgnoreCaseAndIdNot(UUID studioId, String email, UUID id);

    @Query("""
        select i
        from Instructor i
        where i.studio.id = :studioId
          and (:active is null or i.active = :active)
          and (
                lower(i.firstName) like lower(concat('%', :query, '%'))
             or lower(i.lastName) like lower(concat('%', :query, '%'))
             or lower(coalesce(i.email, '')) like lower(concat('%', :query, '%'))
             or lower(coalesce(i.phone, '')) like lower(concat('%', :query, '%'))
          )
        order by i.lastName asc, i.firstName asc
    """)
    List<Instructor> searchByStudioIdAndFilters(UUID studioId, String query, Boolean active);
    
    long countByStudioIdAndActive(UUID studioId, Boolean active);
}