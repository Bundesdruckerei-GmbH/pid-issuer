import { inject, Injectable } from '@angular/core';
import { catchError, finalize, map, Observable, of, tap } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { UserSessionService } from './user-session.service';
import { DeviceDetectorService } from 'ngx-device-detector';
import { IdentificationService } from '../../api/services/identification.service';
import { SessionData } from '../models/session-data';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {
  private readonly http = inject(HttpClient);
  private readonly userSession = inject(UserSessionService);
  private readonly deviceService = inject(DeviceDetectorService);
  private readonly identificationService = inject(IdentificationService);

  /**
   * Function to call the URL to get the session token to process the workflow in following steps.
   * Returns: The token to use or "NO_TOKEN" when an error occurred.
   */
  retrieveAuthToken(): Observable<string> {
    return this.identificationService.getAuthenticationUrl()
      .pipe(
        tap(response => {
          if (response.sessionId) {
            this.userSession.prepare(response);
          }
        }),
        map(response => response.tcTokenUrl)
      );
  }

  /**
   * Function to call a status of a local running SmartApp2 on Desktop-Device.
   * Returns: Observable(true) if SmartApp2 is running, otherwise false
   */
  retrieveEidClientStatus(): Observable<boolean> {
    // Workaround for MPA-3796: Safari blocking http request
    if (!this.isTablet() && this.deviceOS() === 'Mac') {
      return of(true);
    }
    return this.http.get('http://127.0.0.1:24727/eID-Client?Status', { responseType: 'text' })
      .pipe(
        catchError(error => {
          if (error.status === 0) {
            console.info('SmartApp2 not detected on desktop device.');
          } else {
            console.error('Communication error with SmartApp2: ', error);
          }
          return of(false);
        }),
        map(result => !!result)
      );
  }

  loggedIn(ref: string): Observable<void> {
    return this.identificationService.finishLogin(ref)
      .pipe(
        tap({ error: () => this.userSession.delete() }),
        map(response => {
          if (response.sessionId && Number.isInteger(response.duration)) {
            const session = new SessionData(response.sessionId, response.duration);
            this.userSession.initialize(session);
          }
        })
      );
  }

  extendSession(): Observable<void> {
    return this.identificationService.refreshSession()
      .pipe(
        map(response => {
          if (Number.isInteger(response.duration)) {
            this.userSession.update(response);
          }
        })
      );
  }

  logout(): Observable<void> {
    if (!this.userSession.isAliveSession()) {
      this.userSession.delete();
      return of(undefined);
    }
    return this.identificationService.terminateAuthentication()
      .pipe(finalize(() => this.userSession.delete()));
  }

  isTablet(): boolean {
    return this.deviceService.isTablet();
  }

  deviceOS(): string {
    return this.deviceService.getDeviceInfo().os;
  }
}
