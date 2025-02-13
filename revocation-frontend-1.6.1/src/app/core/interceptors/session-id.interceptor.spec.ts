import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpInterceptorFn, HttpStatusCode, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SESSION_ID_HEADER_NAME, sessionIdInterceptor } from './session-id.interceptor';
import { UserSessionService } from '../services/user-session.service';

describe('sessionIdInterceptor', () => {
  const interceptor: HttpInterceptorFn = (req, next) =>
    TestBed.runInInjectionContext(() => sessionIdInterceptor(req, next));

  let userSessionService: UserSessionService;
  let httpTestingController: HttpTestingController;
  let httpClient: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([sessionIdInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    userSessionService = TestBed.inject(UserSessionService);
    httpTestingController = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });

  it('should add a X-Session-Id header', () => {
    const url = '/eid/auth-loggedin';

    spyOn(userSessionService, 'isAliveSession').and.returnValue(true);
    spyOnProperty(userSessionService, 'sessionId').and.returnValue('mock-session-id')

    httpClient.get(url).subscribe({
      next: response => expect(response).toBeTruthy(),
      error: () => fail('should not fail')
    });

    const httpRequest = httpTestingController.expectOne(url);
    expect(httpRequest.request.headers.has(SESSION_ID_HEADER_NAME)).toBeTrue();
    expect(httpRequest.request.headers.get(SESSION_ID_HEADER_NAME)).toEqual('mock-session-id');
    expect(httpRequest.request.headers.getAll(SESSION_ID_HEADER_NAME)).toHaveSize(1);
  });

  it('should throw an exception on not alive session for smarteids', () => {
    const url = '/api/users/RqmT-zkvHpUNEqmUXI7VkQOH-9FsMk789S1RrWrtYNw%3D/issuances/count';

    spyOn(userSessionService, 'isAliveSession').and.returnValue(false);

    httpClient.get(url).subscribe({
      next: () => fail('should not succeed'),
      error: (err) => expect(err.status).toBe(HttpStatusCode.Unauthorized)
    });
  });
});
