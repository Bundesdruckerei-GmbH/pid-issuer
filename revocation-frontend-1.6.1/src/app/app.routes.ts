import { Routes } from '@angular/router';
import { LoginComponent } from './features/login/components/login/login.component';
import { RevocationComponent } from './features/revocation/components/revocation/revocation.component';
import { authGuard } from './core/guards/auth.guard';
import { LicenceInformationComponent } from './features/licence-information/components/licence-information/licence-information.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'logged-in',
    component: LoginComponent
  },
  {
    path: 'revocation',
    component: RevocationComponent,
    canActivate: [authGuard]
  },
  {
    path: 'licences',
    component: LicenceInformationComponent
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
