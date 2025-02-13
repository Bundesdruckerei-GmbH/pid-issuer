import { CanActivateFn, createUrlTreeFromSnapshot } from '@angular/router';
import { inject } from '@angular/core';
import { UserSessionService } from '../services/user-session.service';

export const authGuard: CanActivateFn = (route, _state) => {
  return inject(UserSessionService).isAliveSession() || createUrlTreeFromSnapshot(route.root, ['login']);
};
