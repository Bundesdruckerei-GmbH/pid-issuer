import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, filter, interval, Observable, Subscription, take } from 'rxjs';
import { NavigationEnd, Router } from '@angular/router';
import { UserSessionStoreService } from './user-session-store.service';
import { SessionData } from '../models/session-data';

@Injectable({
  providedIn: 'root'
})
export class UserSessionService {
  public static readonly MILLISECONDS_IN_A_SECOND: number = 1000;
  public static readonly SESSION_RENEW_THRESHOLD_SECONDS: number = 90;
  private readonly loggedIn$ = new BehaviorSubject<boolean>(false);
  private readonly timedOut$ = new BehaviorSubject<boolean>(false);
  private readonly sessionRenew$ = new BehaviorSubject<boolean>(false);
  private readonly remainingDuration$ = new BehaviorSubject<number>(0);
  private sessionDurationSubscription: Subscription | undefined;
  private routerNavigationSubscription: Subscription | undefined;
  private loggedInUntil: Date | null = null;
  private readonly router = inject(Router);
  private readonly userSessionStore = inject(UserSessionStoreService);

  get loggedIn(): Observable<boolean> {
    return this.loggedIn$ as Observable<boolean>;
  }

  get timedOut(): Observable<boolean> {
    return this.timedOut$ as Observable<boolean>;
  }

  get sessionRenew(): Observable<boolean> {
    return this.sessionRenew$ as Observable<boolean>;
  }

  get remainingDuration(): Observable<number> {
    return this.remainingDuration$ as Observable<number>;
  }

  prepare(session: SessionData): void {
    this.userSessionStore.storeSession(session);
  }

  initialize(session: SessionData): void {
    this.userSessionStore.storeSession(session);
    const loggedIn = this.validateSessionByInitialDuration();
    this.loggedIn$.next(loggedIn);
    if (loggedIn) {
      this.startTimer();
    } else {
      this.delete();
    }
  }

  isAlive(): void {
    const isAlive = this.isAliveSession();
    this.loggedIn$.next(isAlive);
    if (isAlive) {
      this.startTimer();
    }
  }

  public isAliveSession(): boolean {
    const storeValueExist = this.userSessionStore.isDataStored();

    this.loggedInUntil = this.userSessionStore.loggedInUntil;

    return storeValueExist
      && this.loggedInUntil !== null
      && this.loggedInUntil > new Date();
  }

  delete(): void {
    this.userSessionStore.clear();
    this.loggedInUntil = null;
    this.loggedIn$.next(false);
    this.remainingDuration$.next(0);
    this.stopTimer();
  }

  private startTimer() {
    this.updateTimer();
    this.sessionDurationSubscription = interval(UserSessionService.MILLISECONDS_IN_A_SECOND)
      .subscribe(() => {
        this.updateTimer();
      });
  }

  private updateTimer() {
    const now = Date.now();
    if (this.loggedInUntil) {
      const timeDifference = this.loggedInUntil.getTime() - now;
      const remainingDuration = timeDifference > 0 ? timeDifference : 0;
      const sessionRenewThreshold = UserSessionService.SESSION_RENEW_THRESHOLD_SECONDS * UserSessionService.MILLISECONDS_IN_A_SECOND;
      if (remainingDuration == 0) {
        this.timeout();
      } else if (remainingDuration <= sessionRenewThreshold && !this.sessionRenew$.getValue()) {
        this.notifySessionRenewChanged();
      } else if (remainingDuration > sessionRenewThreshold && this.sessionRenew$.getValue()) {
        this.notifySessionRenewChanged();
      }
      this.remainingDuration$.next(remainingDuration);
    }
  }

  update(session: SessionData) {
    this.userSessionStore.storeSession(session);
    this.loggedInUntil = this.userSessionStore.loggedInUntil
  }

  private timeout() {
    this.timedOut$.next(true);
  }

  private notifySessionRenewChanged() {
    this.sessionRenew$.next(!this.sessionRenew$.getValue());
  }

  private stopTimer() {
    this.sessionDurationSubscription?.unsubscribe();
    this.sessionDurationSubscription = undefined;
  }

  resetTimedOutOnNextNavigationEvent() {
    this.routerNavigationSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd), take(1))
      .subscribe(() => {
        this.timedOut$.next(false);
      });
  }

  private validateSessionByInitialDuration(): boolean {
    const duration = this.userSessionStore.duration;
    if (duration >= 0) {
      this.loggedInUntil = this.userSessionStore.loggedInUntil;
      return true;
    }
    return false;
  }

  get sessionId(): string | null {
    return this.userSessionStore.sessionId;
  }

}
