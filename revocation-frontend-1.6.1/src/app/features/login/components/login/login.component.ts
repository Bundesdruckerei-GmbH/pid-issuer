import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BasicButtonComponent } from '../../../../shared/components/basic-button/basic-button.component';
import {
  NavigationContainerComponent
} from '../../../../shared/layout/navigation-container/navigation-container.component';
import { ContentContainerComponent } from '../../../../shared/layout/content-container/content-container.component';
import { forkJoin, Observable } from 'rxjs';
import { AuthenticationService } from '../../../../core/services/authentication.service';
import { NotificationsService } from '../../../../core/services/notifications.service';
import { assignWindowLocation } from '../../../../core/utils/navigation-util';

@Component({
  selector: 'login',
  standalone: true,
  imports: [
    BasicButtonComponent,
    ContentContainerComponent,
    NavigationContainerComponent
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  private static readonly ID_PARAM = 'ref';
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notifications = inject(NotificationsService);
  private readonly authService = inject(AuthenticationService);
  protected readonly pidIssuerUrl = 'https://demo.pid-issuer.bundesdruckerei.de';

  ngOnInit(): void {
    const pathSegment = this.route.snapshot.url?.[0]?.path;
    if (pathSegment === 'logged-in') {
      this.handleLoggedInRedirect();
    }
  }

  login(done:()=>void=()=>{}): void {
    this.notifications.clearNotifications();
    /**
     * Make call to get the token and to check the state of availability of the SmartApp2 parallel.
     * Dependent of the result (primary the state) decide to proceed with the desktop or the mobile workflow.
     */
    forkJoin({
      token: this.authService.retrieveAuthToken(),
      status: this.authService.retrieveEidClientStatus()
    })
      .subscribe({
        next: ({token, status}) => {
          const scheme = status ? 'http' : 'eid';
          assignWindowLocation(`${scheme}://127.0.0.1:24727/eID-Client?tcTokenURL=${token}`);
        },
        complete: done
      });
  }

  handleLoggedInRedirect(): void {
    const ref = this.route.snapshot.queryParamMap.get(LoginComponent.ID_PARAM);
    this.confirmLogin(ref ?? '')
      .subscribe(()=> this.navigateToRevocation());
  }

  /**
   * call the backend /api/..../auth-loggedin?ref=...
   * @param ref
   */
  confirmLogin(ref : string) : Observable<void> {
    console.debug('location %s', location.origin );
    console.debug('parameter %s', ref);
    return this.authService.loggedIn(ref);
  }

  navigateToRevocation(): void {
    this.router.navigate(['/revocation'], { replaceUrl : true }).catch(console.error);
  }

}
