import { TestBed } from '@angular/core/testing';

import { AuthenticationService } from './authentication.service';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UserSessionService } from './user-session.service';
import { DeviceDetectorService, DeviceInfo } from 'ngx-device-detector';
import { HttpErrorResponse, HttpResponse, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { LoggedInResponse } from '../../api/models/loggedInResponse';
import { IdentificationService } from '../../api/services/identification.service';
import { of, throwError } from 'rxjs';
import { AuthenticationUrlResponse } from '../../api/models/authenticationUrlResponse';
import createSpyObj = jasmine.createSpyObj;
import SpyObj = jasmine.SpyObj;

const BAD_GATEWAY = 'Bad Gateway';
const SUCCESS = 'Success';
const UNAUTHORIZED = 'Unauthorized';
const UNKNOWN_ERROR = 'Unknown Error';

describe('AuthenticationService', () => {
  let service: AuthenticationService;
  let httpMock: HttpTestingController;
  const userSessionServiceSpy: SpyObj<UserSessionService> = createSpyObj(UserSessionService, ['delete', 'initialize', 'prepare', 'update', 'isAliveSession']);
  const deviceDetectorServiceSpy: SpyObj<DeviceDetectorService> = createSpyObj(DeviceDetectorService, ['isTablet', 'getDeviceInfo']);
  const identificationServiceSpy: SpyObj<IdentificationService> = createSpyObj(IdentificationService, ['getAuthenticationUrl', 'finishLogin', 'refreshSession', 'terminateAuthentication']);

  const deviceInfo = new class implements DeviceInfo {
    browser: string = 'MyBrowser';
    browser_version: string = '1.0';
    device: string = 'MyDevice';
    deviceType: string = 'MyDeviceType';
    orientation: string = 'MyOrientation';
    os: string = 'MyOS';
    os_version: string = 'MyOSVersion';
    userAgent: string = 'MyUserAgent';
  };

  deviceDetectorServiceSpy.getDeviceInfo.and.returnValue(deviceInfo);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: UserSessionService, useValue: userSessionServiceSpy },
        { provide: DeviceDetectorService, useValue: deviceDetectorServiceSpy },
        { provide: IdentificationService, useValue: identificationServiceSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AuthenticationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    userSessionServiceSpy.delete.calls.reset();
    userSessionServiceSpy.initialize.calls.reset();
    userSessionServiceSpy.prepare.calls.reset();
    userSessionServiceSpy.update.calls.reset();
    userSessionServiceSpy.isAliveSession.calls.reset();
    deviceDetectorServiceSpy.isTablet.calls.reset();
    deviceDetectorServiceSpy.getDeviceInfo.calls.reset();
    identificationServiceSpy.getAuthenticationUrl.calls.reset();
    identificationServiceSpy.finishLogin.calls.reset();
    identificationServiceSpy.refreshSession.calls.reset();
    identificationServiceSpy.terminateAuthentication.calls.reset();
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('logout()', () => {
    it('should delete user session and call api successfully', () => {
      userSessionServiceSpy.isAliveSession.and.returnValue(true);
      identificationServiceSpy.terminateAuthentication.and.returnValue(of<any>([]));

      service.logout().subscribe();
      expect(userSessionServiceSpy.delete).toHaveBeenCalledTimes(1);
      expect(identificationServiceSpy.terminateAuthentication).toHaveBeenCalledTimes(1);
    });

    it('should delete user session on failed api call', (done) => {
      userSessionServiceSpy.isAliveSession.and.returnValue(true);
      identificationServiceSpy.terminateAuthentication.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 401, statusText: UNAUTHORIZED })));

      service.logout().subscribe({
        next: () => fail('should not succeed'),
        error: err => {
          expect(err.status).toBe(401);
          expect(err.statusText).toEqual(UNAUTHORIZED);
          done();
        }
      });

      expect(userSessionServiceSpy.delete).toHaveBeenCalledTimes(1);
    });

    it('should not call api but attempt deletion if session is not active', () => {
      userSessionServiceSpy.isAliveSession.and.returnValue(false);
      service.logout().subscribe();
      expect(userSessionServiceSpy.delete).toHaveBeenCalled();
      expect(identificationServiceSpy.terminateAuthentication).not.toHaveBeenCalled();
    });
  });

  describe('loggedIn()', () => {
    const token = "foo123";

    it('should call logged-in api with parameter', (done) => {
      const response: LoggedInResponse = { sessionId: "abc456", duration: 345 };
      identificationServiceSpy.finishLogin.and.returnValue(of<any>(response));

      service.loggedIn(token).subscribe({
        next: ()=> done(),
        error: ()=> fail('should not fail')
      });

      expect(identificationServiceSpy.finishLogin).toHaveBeenCalledWith(token);
    });

    it('should handle Unauthorized response', (done) => {
      identificationServiceSpy.finishLogin.and.returnValue(throwError(() => new HttpResponse({
        body: { code: 401, message: UNAUTHORIZED },
        status: 401,
        statusText: UNAUTHORIZED
      })));

      service.loggedIn(token).subscribe({
        next: () => fail('should not succeed'),
        error: err => {
          expect(err.status).toBe(401);
          expect(err.statusText).toEqual(UNAUTHORIZED);
          done();
        }
      });

      expect(identificationServiceSpy.finishLogin).toHaveBeenCalledWith(token);
    });

    it('should handle Connection Error', (done) => {
      identificationServiceSpy.finishLogin.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 0, statusText: UNKNOWN_ERROR })));

      service.loggedIn(token).subscribe({
        next: () => fail('should not succeed'),
        error: err => {
          expect(err.status).toBe(0);
          expect(err.statusText).toEqual(UNKNOWN_ERROR);
          done();
        }
      });
    });
  });

  describe('retrieveAuthToken()', () => {
    const response: AuthenticationUrlResponse = { tcTokenUrl: 'Water and CD', sessionId: 'abcdefg', duration: 1234 };

    it('should return proper result on Connection Success', (done: DoneFn) => {
      identificationServiceSpy.getAuthenticationUrl.and.returnValue(of<any>(response));
      service.retrieveAuthToken().subscribe(_result => {
        expect(_result).toBe("Water and CD");
        expect(userSessionServiceSpy.prepare).toHaveBeenCalledTimes(1);
        done();
      });
    });

    it('should handle Server Error', (done: DoneFn) => {
      identificationServiceSpy.getAuthenticationUrl.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 502, statusText: BAD_GATEWAY })));

      service.retrieveAuthToken().subscribe({
        next: () => fail('should not succeed'),
        error: err => {
          expect(err.status).toBe(502);
          expect(err.statusText).toEqual(BAD_GATEWAY);
          expect(userSessionServiceSpy.prepare).not.toHaveBeenCalled();
          done();
        }
      });
    });

    it('should return CONNECTION_PROBLEM on Connection Error', (done: DoneFn) => {
      identificationServiceSpy.getAuthenticationUrl.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 0, statusText: UNKNOWN_ERROR })));

      service.retrieveAuthToken().subscribe({
        next: () => fail('should not succeed'),
        error: err => {
          expect(err.status).toBe(0);
          expect(err.statusText).toEqual(UNKNOWN_ERROR);
          expect(userSessionServiceSpy.prepare).not.toHaveBeenCalled();
          done();
        }
      });
    });
  });

  const EID_STATUS_URL = 'http://127.0.0.1:24727/eID-Client?Status';

  describe('retrieveEidClientStatus()', () => {
    it('should return false on http error', () => {
      service.retrieveEidClientStatus().subscribe(_result => {
        expect(_result).toBe(false);
      });
      const request = httpMock.expectOne(EID_STATUS_URL);
      request.flush("", { status: 502, statusText: BAD_GATEWAY });
    });

    it('should return false on communication error', () => {
      service.retrieveEidClientStatus().subscribe(_result => {
        expect(_result).toBe(false);
      });
      const request = httpMock.expectOne(EID_STATUS_URL);
      request.flush("", { status: 0, statusText: UNKNOWN_ERROR });
    });

    it('should return true on success', () => {
      service.retrieveEidClientStatus().subscribe(_result => {
        expect(_result).toBe(true);
      });
      const request = httpMock.expectOne(EID_STATUS_URL);
      request.flush("EID-Client is there", { status: 200, statusText: SUCCESS });
    });
  });

  describe('extendSession()', () => {
    it('should handle success', (done: DoneFn) => {
      const response: LoggedInResponse = { sessionId: "abcd1234", duration: 1000 };
      identificationServiceSpy.refreshSession.and.returnValue(of<any>(response));
      userSessionServiceSpy.update.and.returnValue();

      service.extendSession().subscribe({
        next: () => {
          expect(userSessionServiceSpy.update).toHaveBeenCalled();
          done();
        },
        error: () => fail('should not fail')
      });
    });

    it('should handle Unauthorized response', (done: DoneFn) => {
      identificationServiceSpy.refreshSession.and.returnValue(throwError(() => new HttpResponse({
        body: { code: 401, message: UNAUTHORIZED },
        status: 401,
        statusText: UNAUTHORIZED
      })));

      service.extendSession().subscribe({
        next: () => fail('should not succeed'),
        error: err => {
          expect(err.status).toBe(401);
          expect(err.statusText).toEqual(UNAUTHORIZED);
          done();
        }
      });
    });

    it('should handle Server Error', (done: DoneFn) => {
      identificationServiceSpy.refreshSession.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 502, statusText: BAD_GATEWAY })));

      service.extendSession().subscribe({
        next: () => fail('should not succeed'),
        error: err => {
          expect(err.status).toBe(502);
          expect(err.statusText).toEqual(BAD_GATEWAY);
          done();
        }
      });
    });

    it('should handle Connection Error', (done: DoneFn) => {
      identificationServiceSpy.refreshSession.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 0, statusText: UNKNOWN_ERROR })));

      service.extendSession().subscribe({
        next: () => fail('should not succeed'),
        error: err => {
          expect(err.status).toBe(0);
          expect(err.statusText).toEqual(UNKNOWN_ERROR);
          done();
        }
      });
    });

    it('should return CONNECTION_PROBLEM on unparsable response', (done: DoneFn) => {
      identificationServiceSpy.refreshSession.and.returnValue(of<any>("unparsable"));

      service.extendSession().subscribe({
        next: () => {
          expect(userSessionServiceSpy.update).not.toHaveBeenCalled();
          done();
        },
        error: () => () => fail('should not fail')
      });
    });
  });

  describe('isTablet()', () => {
    it('should call device detector service', () => {
      service.isTablet();
      expect(deviceDetectorServiceSpy.isTablet).toHaveBeenCalled();
    });
  });

  describe('deviceOs()', () => {
    it('should call device detector service', () => {
      expect(service.deviceOS()).toBe('MyOS');
    });
  });

});
