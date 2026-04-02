package ro.daya.dayalog.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.daya.dayalog.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Query("""
        select rt
        from RefreshToken rt
        where rt.tokenHash = :tokenHash
          and rt.revokedAt is null
          and rt.expiresAt > :now
    """)
    Optional<RefreshToken> findActiveTokenByHash(@Param("tokenHash") String tokenHash,
                                                 @Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
        update RefreshToken rt
        set rt.revokedAt = :revokedAt
        where rt.id = :tokenId
          and rt.revokedAt is null
    """)
    int revokeById(@Param("tokenId") UUID tokenId,
                   @Param("revokedAt") OffsetDateTime revokedAt);

    @Modifying
    @Query("""
        update RefreshToken rt
        set rt.revokedAt = :revokedAt
        where rt.user.id = :userId
          and rt.revokedAt is null
    """)
    int revokeAllActiveTokensForUser(@Param("userId") UUID userId,
                                     @Param("revokedAt") OffsetDateTime revokedAt);
}