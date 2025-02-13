/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.adapter.out.persistence;

import de.bdr.revocation.identification.core.model.Authentication;
import de.bdr.revocation.identification.core.model.AuthenticationState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "authentication")
public class AuthenticationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(unique = true)
    private String referenceId;

    @Column
    private String pseudonym;

    @Column
    @NotNull
    private Instant created;

    @Column
    @NotNull
    private Instant validUntil;

    public AuthenticationEntity(Authentication authentication) {
        this.authenticationState = authentication.getAuthenticationState();
        this.sessionId = authentication.getSessionId();
        this.tokenId = authentication.getTokenId();
        this.samlId = authentication.getSamlId();
        this.referenceId = authentication.getReferenceId();
        this.pseudonym = authentication.getPseudonym();
        this.created = authentication.getCreated();
        this.validUntil = authentication.getValidUntil();
    }
}
