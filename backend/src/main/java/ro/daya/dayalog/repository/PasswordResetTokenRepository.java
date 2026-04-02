package ro.daya.dayalog.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.daya.dayalog.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Query("""
        select prt
        from PasswordResetToken prt
        where prt.tokenHash = :tokenHash
          and prt.usedAt is null
          and prt.expiresAt > :now
    """)
    Optional<PasswordResetToken> findActiveTokenByHash(@Param("tokenHash") String tokenHash,
                                                       @Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
        update PasswordResetToken prt
        set prt.usedAt = :usedAt
        where prt.id = :tokenId
          and prt.usedAt is null
    """)
    int markUsed(@Param("tokenId") UUID tokenId,
                 @Param("usedAt") OffsetDateTime usedAt);

    @Modifying
    @Query("""
        delete from PasswordResetToken prt
        where prt.user.id = :userId
          and prt.usedAt is null
    """)
    int deleteUnusedTokensForUser(@Param("userId") UUID userId);
}