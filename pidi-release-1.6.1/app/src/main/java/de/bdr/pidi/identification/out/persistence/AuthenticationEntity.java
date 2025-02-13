/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.out.persistence;

import de.bdr.pidi.identification.core.model.Authentication;
import de.bdr.pidi.identification.core.model.AuthenticationState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Table(name = "eid_session",
        indexes = {
                @Index(name = "vu_index", columnList = "validUntil"),
                @Index(name = "vu_as_index", columnList = "validUntil, authenticationState")
        }
)
@Entity
@NoArgsConstructor
public class AuthenticationEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column
    @NotNull
    @Enumerated(EnumType.STRING)
    private AuthenticationState authenticationState;

    @Column(unique = true)
    @NotNull
    private String sessionId;
    @Column(unique = true)
    @NotNull
    private String tokenId;
    @Column(unique = true)
    private String samlId;
    @Column
    @NotNull
    private Instant created;
    @Column
    @NotNull
    private Instant validUntil;
    @Column
    private String externalId;
    private String redirectUrl;
    private String referenceId;

    public AuthenticationEntity(Authentication authentication) {
        this.authenticationState = authentication.getAuthenticationState();
        this.sessionId = authentication.getSessionId();
        this.tokenId = authentication.getTokenId();
        this.samlId = authentication.getSamlId();
        this.created = authentication.getCreated();
        this.validUntil = authentication.getValidUntil();
        this.externalId = authentication.getExternalId();
        this.referenceId = authentication.getReferenceId();
        this.redirectUrl = authentication.getRedirectUrl().toString();
    }
}
