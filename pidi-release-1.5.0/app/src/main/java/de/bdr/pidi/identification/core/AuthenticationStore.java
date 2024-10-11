/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core;

import de.bdr.pidi.identification.core.exception.AuthenticationNotFoundException;
import de.bdr.pidi.identification.core.model.Authentication;
import de.bdr.pidi.identification.core.model.AuthenticationState;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.time.Instant;
import java.util.Collection;
@SecondaryPort
public interface AuthenticationStore {
    /**
     * Finds an {@link Authentication} by the tokenId.
     *
     * @param tokenId the tokenId to search for
     * @return the found authentication
     * @throws AuthenticationNotFoundException if no {@link Authentication} is found for this tokenId
     */
    Authentication findByTokenId(String tokenId);

    /**
     * Finds an {@link Authentication} by the samlId.
     *
     * @param samlId
     * @return the found authentication
     * @throws AuthenticationNotFoundException if no {@link Authentication} is found for this samlId
     */
    Authentication findBySamlId(String samlId);

    /**
     * Finds an {@link Authentication} by the referenceId.
     *
     * @param referenceId
     * @return the found authentication
     * @throws AuthenticationNotFoundException if no {@link Authentication} is found for this referenceId
     */
    Authentication findByReferenceId(String referenceId);

    /**
     * Finds an {@link Authentication} by the sessionId.
     *
     * @param sessionId
     * @return the found authentication
     * @throws AuthenticationNotFoundException if no {@link Authentication} is found for this sessionId
     */
    Authentication findBySessionId(String sessionId);

    /**
     * delete authentication objects that are in specific states
     *
     * @param authenticationStates relevant states for the search
     * @return number of deleted authentication objects
     */
    int deleteByAuthenticationStateIn(Collection<AuthenticationState> authenticationStates);

    /**
     * update authentication object whose validity has been expired prior to a specific date and not having specified states
     *
     * @param newState             the state to set
     * @param validUntil           the specified date
     * @param authenticationStates states to be filtered out
     * @return number of updated authentication objects
     */
    int updateStateByValidUntilBeforeAndAuthenticationStateNotIn(AuthenticationState newState, Instant validUntil, Collection<AuthenticationState> authenticationStates);

    /**
     * first call to store the Authentication after it was created.
     * Set attributes:
     * <ul>
     *     <li>authenticationState</li>
     *     <li>sessionId</li>
     *     <li>tokenId</li>
     *     <li>validUntil</li>
     *     <li>created</li>
     *     <li>externalId</li>
     *     <li>redirectUrl</li>
     * </ul>
     */
    void createWithSessionAndToken(Authentication authentication);

    /**
     * update the Authentication in the Store to persist a transition to state STARTED.
     * Changed attributes:
     * <ul>
     *     <li>authenticationState</li>
     *     <li>samlId</li>
     * </ul>
     */
    void updateWithSamlIdentifiedByTokenAndFormerState(Authentication authentication, AuthenticationState formerState);

    /**
     * update the Authentication in the Store to persist a transition to state RESPONDED.
     * Changed attributes:
     * <ul>
     *     <li>authenticationState</li>
     *     <li>referenceId</li>
     * </ul>
     */
    void updateWithReferenceIdIdentifiedBySamlAndFormerState(Authentication authentication, AuthenticationState formerState);

    /**
     * update the Authentication in the Store to persist a transition to state AUTHENTICATED.
     * Changed attributes:
     * <ul>
     *     <li>authenticationState</li>
     *     <li>sessionId</li>
     *     <li>validUntil</li>
     * </ul>
     */
    void updateWithChangedSessionValidUntilIdentifiedByReferenceAndFormerState(Authentication authentication, String oldSessionId, AuthenticationState formerState);


    /**
     * update (or delete) the Authentication in the Store to persist a transition to state TERMINATED.
     * Changed attributes:
     * <ul>
     *     <li>authenticationState</li>
     * </ul>
     * In case this method updates the state,
     * a separate process (housekeeping) is responsible for deleting the entries from the store.
     */
    void removeIdentifiedBySessionAndFormerState(Authentication authentication, String sessionId, AuthenticationState formerState);

    long countAuthenticatedSessions();


}
