package ro.daya.dayalog.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.daya.dayalog.entity.EmailVerificationToken;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    @Query("""
        select evt
        from EmailVerificationToken evt
        where evt.tokenHash = :tokenHash
          and evt.usedAt is null
          and evt.expiresAt > :now
    """)
    Optional<EmailVerificationToken> findActiveTokenByHash(@Param("tokenHash") String tokenHash,
                                                           @Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
        update EmailVerificationToken evt
        set evt.usedAt = :usedAt
        where evt.id = :tokenId
          and evt.usedAt is null
    """)
    int markUsed(@Param("tokenId") UUID tokenId,
                 @Param("usedAt") OffsetDateTime usedAt);

    @Modifying
    @Query("""
        delete from EmailVerificationToken evt
        where evt.user.id = :userId
          and evt.usedAt is null
    """)
    int deleteUnusedTokensForUser(@Param("userId") UUID userId);
}