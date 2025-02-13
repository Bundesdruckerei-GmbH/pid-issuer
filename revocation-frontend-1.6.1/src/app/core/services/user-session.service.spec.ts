import { discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';

import { UserSessionService } from './user-session.service';
import { Subject, take } from 'rxjs';
import { NavigationEnd, NavigationExtras, Router, RouterEvent } from '@angular/router';
import { UserSessionStoreService } from './user-session-store.service';
import { SessionData } from '../models/session-data';

describe('UserSessionService', () => {
  let service: UserSessionService;

  const routerEventSubject = new Subject<RouterEvent>();

  const routerMock = {
    events: routerEventSubject.asObservable(),
    navigate: (_commands: any[], _extras?: NavigationExtras): Promise<boolean> => {
      return Promise.resolve(true);
    }
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {provide: Router, useValue: routerMock}
      ]
    });
    spyOn(routerMock, 'navigate').and.returnValue(Promise.resolve(true));
    service = TestBed.inject(UserSessionService);
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should initially not be logged in', done => {
    const loggedIn = service.loggedIn;
    loggedIn.subscribe(result => {
      expect(result).toBeFalse();
      done();
    });
  });

  describe("prepare()", () => {
    it("should initialize local storage", () => {

      const session: SessionData = new SessionData("efg", 10);
      service.prepare(session);

      expect(sessionStorage.getItem(UserSessionStoreService.SESSION_ID_KEY)).toBeDefined();
    });

    it("should not start session", () => {

      const session: SessionData = new SessionData("efg", 10);
      service.prepare(session);

      service.loggedIn.pipe(take(1)).subscribe(result => {
        expect(result).toBeFalse();
      });
    });
  });

  describe('initialize()', () => {
    it('should be alive with valid cookies', fakeAsync(() => {

      const session: SessionData = new SessionData("efg", 10);
      service.initialize(session);

      const loggedIn = service.loggedIn;
      service.isAlive();

      loggedIn.pipe(take(1)).subscribe(result => {
        expect(result).toBeTrue();
      });

      discardPeriodicTasks();
    }));

    it('should initialize local storage', fakeAsync(() => {

      const session: SessionData = new SessionData("efg", 10);
      service.initialize(session);

      const loggedIn = service.loggedIn;
      service.isAlive();

      loggedIn.pipe(take(1)).subscribe(_ => {
        expect(sessionStorage.getItem(UserSessionStoreService.SESSION_ID_KEY)).toBeDefined();
        expect(sessionStorage.getItem(UserSessionStoreService.SESSION_EXPIRY_KEY)).toBeDefined();
      });

      discardPeriodicTasks();
    }));

    it('should not be alive with expired session loggedInUntil', done => {
      const session: SessionData = new SessionData("efg", 10);
      service.initialize(session);

      const dateNow = new Date();
      const dateExpire = new Date();
      dateExpire.setTime(dateNow.getTime() - 1000); // go  milliseconds to the past
      sessionStorage.setItem(UserSessionStoreService.SESSION_EXPIRY_KEY, dateExpire.toISOString());
      const loggedIn = service.loggedIn;

      service.isAlive();

      loggedIn.subscribe(result => {
        expect(result).toBeFalse();
        done();
      });
    });

    it('should not be alive with expired second session cookie', done => {
      const session: SessionData = new SessionData("efg", -1);
      service.initialize(session);

      service.isAlive();

      service.loggedIn.subscribe(result => {
        expect(result).toBeFalse();
        done();
      })
    });

    it('should not be alive with invalid second session cookie', done => {

      // this test case cannot happen, the class provides some validation
      const session: SessionData = new SessionData("efg", 0);
      service.initialize(session);

      sessionStorage.setItem(UserSessionStoreService.SESSION_ID_KEY, 'efg"');

      service.isAlive();

      service.loggedIn.subscribe(result => {
        expect(result).toBeFalse();
        done();
      })
    });

    it('should not be alive without second session cookie', done => {

      // this test case cannot happen, the class provides some validation
      const session: SessionData = new SessionData("efg", 10);
      service.initialize(session);

      sessionStorage.removeItem(UserSessionStoreService.SESSION_ID_KEY);
      service.isAlive();

      service.loggedIn.subscribe(result => {
        expect(result).toBeFalse();
        done();
      })
    });

    it('should update session duration from second cookie', fakeAsync(() => {
      service.remainingDuration.pipe(take(1)).subscribe(duration => {
        expect(duration).toBe(0);
      });
      const session: SessionData = new SessionData("efg", 1000);
      service.initialize(session);
      service.remainingDuration.pipe(take(1)).subscribe(duration => {
        expect(duration).toBeGreaterThan(0);
      });

      discardPeriodicTasks();
    }));

    it('should start timer on remaining duration which should decrease', fakeAsync(() => {
      service.remainingDuration.pipe(take(1)).subscribe(duration => {
        expect(duration).toBe(0);
      });
      const session: SessionData = new SessionData("efg", 1000);
      service.initialize(session);
      let previousRemainingDuration = 0;
      service.remainingDuration.pipe(take(1)).subscribe(duration => {
        expect(duration).toBeGreaterThan(0);
        previousRemainingDuration = duration;
      });

      tick(UserSessionService.MILLISECONDS_IN_A_SECOND + 5);
      service.remainingDuration.pipe(take(1)).subscribe(duration => {
        expect(duration).toBeGreaterThan(0);
        expect(duration).toBeLessThan(previousRemainingDuration);
      });

      discardPeriodicTasks();
    }));

  });

  it('should update session duration on isAlive', fakeAsync(() => {
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBe(0);
    });

    const now = new Date();
    const loggedInUntil = new Date(now.getTime() + 600);
    sessionStorage.setItem(UserSessionStoreService.SESSION_EXPIRY_KEY, loggedInUntil.toISOString());
    sessionStorage.setItem(UserSessionStoreService.SESSION_ID_KEY, 'efg');
    sessionStorage.setItem(UserSessionStoreService.SESSION_DURATION_KEY, '600');
    service.isAlive();
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBe(600);
    });

    discardPeriodicTasks();
  }));

  describe('extendDurationTo(newDuration)', () => {
    it('should extend session duration correctly', fakeAsync(() => {
      const now = new Date();
      sessionStorage.setItem(UserSessionStoreService.SESSION_EXPIRY_KEY, now.toISOString());
      expect(sessionStorage.getItem(UserSessionStoreService.SESSION_EXPIRY_KEY)).toBe(now.toISOString());

      const duration = 600 * UserSessionService.MILLISECONDS_IN_A_SECOND;
      const newExpiry = new Date(now.getTime() + duration);
      service.update(new SessionData('efg', 600));
      expect(sessionStorage.getItem(UserSessionStoreService.SESSION_EXPIRY_KEY)).toBe(newExpiry.toISOString());

      service.isAlive()
      service.remainingDuration.pipe(take(1)).subscribe(duration => {
        expect(duration).toBe(duration);
      });

      discardPeriodicTasks();
    }));
  });

  it('should read the session id', () => {

    sessionStorage.setItem(UserSessionStoreService.SESSION_ID_KEY, 'efg');
    const result = service.sessionId;

    expect(result).toBe('efg');
  });

  it('should stop duration timer on session delete', fakeAsync(() => {
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBe(0);
    });
    const session: SessionData = new SessionData("efg", 600);
    service.initialize(session);
    let previousRemainingDuration = 0;
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBeGreaterThan(0);
      previousRemainingDuration = duration;
    });
    tick(UserSessionService.MILLISECONDS_IN_A_SECOND);
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBeGreaterThan(0);
      expect(duration).toBeLessThan(previousRemainingDuration);
    });
    service.delete();
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBe(0);
    });
    tick(UserSessionService.MILLISECONDS_IN_A_SECOND);
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBe(0);
    });
  }));

  it('should stop duration timer on timeout', fakeAsync(() => {
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBe(0);
    });

    const session: SessionData = new SessionData("efg", 10);
    service.initialize(session);
    let previousRemainingDuration = 0;
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBeGreaterThan(0);
      previousRemainingDuration = duration;
    });

    tick(UserSessionService.MILLISECONDS_IN_A_SECOND + 4);
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBeGreaterThan(0);
      expect(duration).toBeLessThan(previousRemainingDuration);
    });

    tick(10 * UserSessionService.MILLISECONDS_IN_A_SECOND);
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBe(0);
    });
    tick(UserSessionService.MILLISECONDS_IN_A_SECOND);
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBe(0);
    });
    discardPeriodicTasks();
  }));

  it('should time out after session duration and not before login', fakeAsync(() => {
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBe(0);
    });
    service.loggedIn.pipe(take(1)).subscribe(loggedIn => {
      expect(loggedIn).toBe(false);
    });
    service.timedOut.pipe(take(1)).subscribe(timedOut => {
      expect(timedOut).toBe(false);
    });

    const session: SessionData = new SessionData("efg", 1);
    service.initialize(session);
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBe(1000);
    });
    service.loggedIn.pipe(take(1)).subscribe(loggedIn => {
      expect(loggedIn).toBe(true);
    });
    service.timedOut.pipe(take(1)).subscribe(timedOut => {
      expect(timedOut).toBe(false);
    });

    tick(UserSessionService.MILLISECONDS_IN_A_SECOND);
    service.remainingDuration.pipe(take(1)).subscribe(duration => {
      expect(duration).toBe(0);
    });
    service.timedOut.pipe(take(1)).subscribe(timedOut => {
      expect(timedOut).withContext('timedOut').toBe(true);
    });
    discardPeriodicTasks();
  }));

  it('should reset time out after navigation', fakeAsync(() => {
    service.timedOut.pipe(take(1)).subscribe(timedOut => {
      expect(timedOut).toBe(false);
    });
    const session: SessionData = new SessionData("efg", 1);
    service.initialize(session);
    service.timedOut.pipe(take(1)).subscribe(timedOut => {
      expect(timedOut).toBe(false);
    });
    tick(UserSessionService.MILLISECONDS_IN_A_SECOND * 5);
    service.timedOut.pipe(take(1)).subscribe(timedOut => {
      expect(timedOut).toBe(true);
    });
    // reset would now be triggered by logged-in-bar
    service.resetTimedOutOnNextNavigationEvent();
    routerEventSubject.next(new NavigationEnd(1, 'someUrl', 'someRedirectUrl'));
    service.timedOut.pipe(take(1)).subscribe(timedOut => {
      expect(timedOut).toBe(false);
    });
    discardPeriodicTasks();
  }));

});
