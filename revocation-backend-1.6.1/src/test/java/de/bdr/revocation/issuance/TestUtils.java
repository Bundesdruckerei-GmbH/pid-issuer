/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.issuance;

import de.bdr.revocation.issuance.adapter.out.persistence.IssuanceEntity;
import de.bdr.revocation.issuance.adapter.out.rest.api.model.Reference;
import de.bdr.revocation.issuance.app.domain.Issuance;
import de.bdr.revocation.issuance.app.domain.IssuanceCount;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // Utility class
public class TestUtils {
    private static final AtomicInteger NEXT_INDEX = new AtomicInteger(120);

    public static Issuance createIssuance() {
        return createIssuance("pseudonym");
    }

    public static Issuance createIssuance(final String pseudonym) {
        return createIssuance(pseudonym, "listID");
    }

    public static Issuance createIssuance(final String pseudonym, final String listID) {
        val issuance = new Issuance();
        issuance.setPseudonym(pseudonym);
        issuance.setListID(listID);
        issuance.setIndex(NEXT_INDEX.getAndIncrement());
        issuance.setExpirationTime(getExpirationTime());
        return issuance;
    }

    public static IssuanceEntity createIssuanceEntity(final String pseudonym, final Reference reference, final boolean revoked) {
        val issuance = new IssuanceEntity();
        issuance.setPseudonym(pseudonym);
        issuance.setRevoked(revoked);
        issuance.setListId(reference.getUri());
        issuance.setListIndex(reference.getIndex());
        issuance.setExpirationTime(getExpirationTime());
        return issuance;
    }

    public static IssuanceCount createIssuanceCount() {
        return new IssuanceCount(5, 3);
    }

    private static Instant getExpirationTime() {
        return Instant.now().plusSeconds(3600L).truncatedTo(ChronoUnit.MICROS); // postresql saves timestamps not with nano's
    }
}
