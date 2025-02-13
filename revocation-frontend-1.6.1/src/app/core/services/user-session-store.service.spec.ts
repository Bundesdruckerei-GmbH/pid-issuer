import { TestBed } from '@angular/core/testing';

import { UserSessionStoreService } from './user-session-store.service';
import { SessionData } from '../models/session-data';

describe('UserSessionStoreService', () => {
  const SESSION_ID: string = 'session-id';
  let service: UserSessionStoreService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(UserSessionStoreService);
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should store a session in sessionStorage', () => {
    service.storeSession(new SessionData(SESSION_ID, 1234));

    expect(sessionStorage.getItem(UserSessionStoreService.SESSION_ID_KEY)).toEqual('session-id');
    expect(sessionStorage.getItem(UserSessionStoreService.SESSION_DURATION_KEY)).toEqual('1234');
    expect(sessionStorage.getItem(UserSessionStoreService.SESSION_EXPIRY_KEY)).toMatch(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\w*/);
  });

  describe('clearOutdatedSession()', () => {
    it('should not affect a not outdated session', () => {
      service.storeSession(new SessionData(SESSION_ID, 1000));

      service.clearOutdatedSession();

      expect(service.isDataStored()).toBeTrue();
    })

    it('should remove an outdated session', () => {
      const startDate = new Date();
      const endDate = new Date(startDate.getTime() + 4000);

      jasmine.clock().install();
      jasmine.clock().mockDate(startDate);

      service.storeSession(new SessionData(SESSION_ID, 1));

      jasmine.clock().mockDate(endDate);
      service.clearOutdatedSession();

      expect(service.isDataStored()).toBeFalse();
      jasmine.clock().uninstall();
    })

    it('should remove an outdated session on construction', () => {
      sessionStorage.clear();
      sessionStorage.setItem(UserSessionStoreService.SESSION_ID_KEY, 'abc.1');
      const dateInThePast = new Date(Date.UTC(2020, 1, 1)).toISOString();
      sessionStorage.setItem(UserSessionStoreService.SESSION_EXPIRY_KEY, dateInThePast);
      const newService = new UserSessionStoreService();
      expect(newService.isDataStored()).toBeFalse();
    })
  });

  describe('getLoggendInUntil()', () => {
    it('should return the correct loggedInUntil', () => {
      const duration = 1;
      const startingTime = Date.now();
      jasmine.clock().install();
      jasmine.clock().mockDate(new Date(startingTime));

      service.storeSession(new SessionData(SESSION_ID, duration));

      expect(service.loggedInUntil).toEqual(new Date(startingTime + duration * UserSessionStoreService.MILLISECONDS_IN_A_SECOND));
      jasmine.clock().uninstall();
    })
  });

  describe('isDataStored()', () => {
    it('no data is stored initially', () => {
      expect(service.isDataStored()).toBeFalse();
    })

    it('should return true if data is stored', () => {
      service.storeSession(new SessionData(SESSION_ID, 1234));

      expect(service.isDataStored()).toBeTrue();
    })

    it('should remove entries from local storage with clear', () => {
      service.storeSession(new SessionData(SESSION_ID, 1234));

      service.clear();

      expect(service.isDataStored()).toBeFalse();
    })
  });

  describe('getSessionId()', () => {
    it('should return a zero duration on uninitialized store', () => {
      service.clear();
      expect(service.duration).toEqual(0);
    })

    it('should return the stored duration', () => {
      service.storeSession(new SessionData(SESSION_ID, 1234));

      expect(service.duration).toEqual(1234);
    })

    it('should return the stored sessionId', () => {
      service.storeSession(new SessionData(SESSION_ID, 1234));

      expect(service.sessionId).toEqual(SESSION_ID);
    })
  });

});
