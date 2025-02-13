import { HttpErrorResponse, HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { UserSessionService } from '../services/user-session.service';

export const SESSION_ID_HEADER_NAME = 'X-Session-ID';

const USERS_URL = '/api/users';
const AUTH_LOGGED_IN_URL = '/eid/auth-loggedin';
const AUTH_REFRESH_URL = '/eid/auth-refresh';
const AUTH_LOGOUT_URL = '/eid/auth-drop';

/** Pass second authentication token to api requests */
export const sessionIdInterceptor: HttpInterceptorFn = (request, next) => {
  const userSessionService = inject(UserSessionService);

  function isInterceptingUrl(url: string): boolean {
    return url.includes(USERS_URL) || url.includes(AUTH_LOGGED_IN_URL) || url.includes(AUTH_REFRESH_URL) || url.includes(AUTH_LOGOUT_URL);
  }

  if (request.url.includes(USERS_URL) && !userSessionService.isAliveSession()) {
    throw new HttpErrorResponse({ status: HttpStatusCode.Unauthorized });
  }
  const sessionId = userSessionService.sessionId;
  if (isInterceptingUrl(request.url) && !!sessionId) {
    const authenticatedRequest = request.clone({
      headers: request.headers.set(SESSION_ID_HEADER_NAME, sessionId)
    });
    return next(authenticatedRequest);
  }

  return next(request);
};
