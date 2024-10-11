/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "pidi_nonce")
public class PidiNonceEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String nonce;

    @Column(nullable = false)
    private Instant expires;

    @Column(nullable = false)
    private boolean used = false;
}
