/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.util;

import de.bdr.openid4vc.vci.service.HttpRequest;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestUtil {
    private static final String MISSING_MSG = "%s missing";

    public static String getParam(HttpRequest<?> request, String paramName) {
        String param = request.getParameters().get(paramName);
        if (StringUtils.isBlank(param)) {
            throw new InvalidRequestException(MISSING_MSG.formatted(paramName));
        }
        return param;
    }
}
