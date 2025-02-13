/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.domain;

import de.bdr.pidi.authorization.FlowVariant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum Requests {
    PUSHED_AUTHORIZATION_REQUEST(Paths.PAR),
    AUTHORIZATION_REQUEST(Paths.AUTHORIZE),
    FINISH_AUTHORIZATION_REQUEST(Paths.FINISH_AUTHORIZATION),
    TOKEN_REQUEST(Paths.TOKEN),
    SEED_TOKEN_REQUEST(Paths.TOKEN),
    SEED_CREDENTIAL_REQUEST(Paths.CREDENTIAL),
    CREDENTIAL_REQUEST(Paths.CREDENTIAL),
    IDENTIFICATION_RESULT(null),
    PRESENTATION_SIGNING_REQUEST(Paths.PRESENTATION_SIGNING);

    private final String path;

    public String getPath(FlowVariant variant) {
        return path == null ? null : variant.urlPath + "/" + path;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Paths {
        public static final String PAR = "par";
        public static final String AUTHORIZE = "authorize";
        public static final String FINISH_AUTHORIZATION = "finish-authorization";
        public static final String TOKEN = "token";
        public static final String CREDENTIAL = "credential";
        public static final String SESSION = "session";
        public static final String PRESENTATION_SIGNING = "presentation-signing";
    }
}
