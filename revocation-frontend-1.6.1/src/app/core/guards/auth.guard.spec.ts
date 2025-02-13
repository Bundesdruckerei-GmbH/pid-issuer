import { TestBed } from '@angular/core/testing';
import { CanActivateFn, provideRouter, Router, Routes } from '@angular/router';

import { Component } from '@angular/core';
import { authGuard } from './auth.guard';
import { UserSessionService } from '../services/user-session.service';
import createSpyObj = jasmine.createSpyObj;

@Component({
  selector: 'empty-mock',
  template: ''
})
class EmptyMockComponent {}

describe('authGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
      TestBed.runInInjectionContext(() => authGuard(...guardParameters));

  const userSessionServiceSpy = createSpyObj(UserSessionService, ['isAliveSession']);

  const routes: Routes = [{
    path: 'revocation',
    component: EmptyMockComponent,
    canActivate: [authGuard]
  }, {
    path: 'login',
    component: EmptyMockComponent
  }];

  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: UserSessionService, useValue: userSessionServiceSpy },
        provideRouter(routes)
      ]
    });
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    userSessionServiceSpy.isAliveSession.calls.reset();
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });

  it('should not pass when not logged in', async () => {
    userSessionServiceSpy.isAliveSession.and.returnValue(false);

    await router.navigate(['revocation']);

    expect(router.url.includes('login')).toBeTrue();
  });

  it('should pass when logged in', async () => {
    userSessionServiceSpy.isAliveSession.and.returnValue(true);

    await router.navigate(['revocation']);

    expect(router.url.includes('revocation')).toBeTrue();
  });
});
