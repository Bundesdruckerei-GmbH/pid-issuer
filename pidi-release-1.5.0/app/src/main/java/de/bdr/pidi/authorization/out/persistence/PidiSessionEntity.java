/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.out.persistence;


import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.domain.Requests;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "pidi_session")
public class PidiSessionEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column
    @NotNull
    @Enumerated(EnumType.STRING)
    private FlowVariant flow;
    private String session;
    private String authorizationCode;
    private String issuerState;
    private String requestUri;
    private String accessToken;
    private String refreshTokenDigest;
    private String pidIssuerSessionId;
    private Instant expires;
    @Column(insertable = false, updatable = false)
    private Instant created;

    @Column
    @Enumerated(EnumType.STRING)
    private Requests nextExpectedRequest;

}
