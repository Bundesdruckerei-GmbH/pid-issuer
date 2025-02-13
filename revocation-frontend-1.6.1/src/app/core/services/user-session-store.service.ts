import { Injectable } from '@angular/core';
import { SessionData } from '../models/session-data';

@Injectable({
  providedIn: 'root'
})
export class UserSessionStoreService {
  public static readonly SESSION_EXPIRY_KEY = 'SESSIONEXPIRY';
  public static readonly SESSION_ID_KEY = 'SESSIONHANDLE';
  public static readonly SESSION_DURATION_KEY = 'SESSIONDURATION';
  public static readonly MILLISECONDS_IN_A_SECOND = 1000;

  constructor() {
    this.clearOutdatedSession();
  }

  get sessionId(): string | null {
    const value = sessionStorage.getItem(UserSessionStoreService.SESSION_ID_KEY);
    if (!value) {
      console.warn("Session ID not found in session storage");
    }
    return value;
  }

  get duration(): number {
    const duration = sessionStorage.getItem(UserSessionStoreService.SESSION_DURATION_KEY);
    return duration ? Number.parseInt(duration) : 0;
  }

  get loggedInUntil(): Date | null {
    const isoDate = sessionStorage.getItem(UserSessionStoreService.SESSION_EXPIRY_KEY);
    const date = isoDate ? new Date(isoDate) : null;
    return date?.getTime() ? date : null;
  }

  storeSession(session: SessionData): void {
    sessionStorage.setItem(UserSessionStoreService.SESSION_ID_KEY, session.sessionId);
    sessionStorage.setItem(UserSessionStoreService.SESSION_DURATION_KEY, `${session.duration}`);
    this.storeLoggedInUntil(session.duration);
  }

  private storeLoggedInUntil(duration: number): Date {
    const now = new Date();
    const loggedInUntil = new Date();

    loggedInUntil.setTime(now.getTime() + (duration * UserSessionStoreService.MILLISECONDS_IN_A_SECOND));

    sessionStorage.setItem(UserSessionStoreService.SESSION_EXPIRY_KEY, loggedInUntil.toISOString());
    return loggedInUntil;
  }

  isDataStored(): boolean {
    const sessionId = sessionStorage.getItem(UserSessionStoreService.SESSION_ID_KEY);
    const sessionDuration = sessionStorage.getItem(UserSessionStoreService.SESSION_DURATION_KEY);
    const sessionExpiry = sessionStorage.getItem(UserSessionStoreService.SESSION_EXPIRY_KEY);
    return !!sessionId && !!sessionDuration && !!sessionExpiry;
  }

  clear(): void {
    sessionStorage.removeItem(UserSessionStoreService.SESSION_ID_KEY);
    sessionStorage.removeItem(UserSessionStoreService.SESSION_DURATION_KEY);
    sessionStorage.removeItem(UserSessionStoreService.SESSION_EXPIRY_KEY);
  }

  clearOutdatedSession(): void {
    const now = new Date();
    if (this.isDataStored()) {
      const loggedInUntil = this.loggedInUntil;
      if (loggedInUntil && loggedInUntil < now) {
        this.clear();
      }
    }
  }

}
