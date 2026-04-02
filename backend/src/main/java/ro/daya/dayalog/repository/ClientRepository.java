package ro.daya.dayalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.daya.dayalog.entity.Client;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByIdAndStudioId(UUID id, UUID studioId);

    boolean existsByStudioIdAndEmailIgnoreCase(UUID studioId, String email);

    boolean existsByStudioIdAndPhone(UUID studioId, String phone);

    boolean existsByStudioIdAndEmailIgnoreCaseAndIdNot(UUID studioId, String email, UUID id);

    boolean existsByStudioIdAndPhoneAndIdNot(UUID studioId, String phone, UUID id);

    List<Client> findTop20ByStudioIdAndActiveOrderByLastNameAscFirstNameAsc(UUID studioId, Boolean active);

    @Query("""
        select c
        from Client c
        where c.studio.id = :studioId
          and (:active is null or c.active = :active)
          and (
                lower(c.firstName) like lower(concat('%', :query, '%'))
             or lower(c.lastName) like lower(concat('%', :query, '%'))
             or lower(c.email) like lower(concat('%', :query, '%'))
             or lower(c.phone) like lower(concat('%', :query, '%'))
          )
        order by c.lastName asc, c.firstName asc
    """)
    List<Client> searchByStudioIdAndFilters(@Param("studioId") UUID studioId,
                                            @Param("query") String query,
                                            @Param("active") Boolean active);

    Optional<Client> findByUserIdAndStudioId(UUID userId, UUID studioId);

    long countByStudioIdAndActive(UUID studioId, Boolean active);

    List<Client> findTop20ByStudioIdOrderByLastNameAscFirstNameAsc(UUID studioId);
}