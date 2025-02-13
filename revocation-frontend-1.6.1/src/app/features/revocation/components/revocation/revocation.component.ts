import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { BasicButtonComponent } from '../../../../shared/components/basic-button/basic-button.component';
import { NavigationContainerComponent } from '../../../../shared/layout/navigation-container/navigation-container.component';
import { NotificationsService } from '../../../../core/services/notifications.service';
import { ContentContainerComponent } from '../../../../shared/layout/content-container/content-container.component';
import { AuthenticationService } from '../../../../core/services/authentication.service';
import { SummaryComponent } from '../summary/summary.component';
import { ConfirmDialogComponent, ConfirmDialogContent, dialogConfirmed } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { Dialog } from '@angular/cdk/dialog';
import { filter, tap } from 'rxjs';
import { IssuanceService } from '../../../../api/services/issuance.service';
import { IssuanceCount } from '../../../../api/models/issuanceCount';
import { HttpErrorResponse } from '@angular/common/http';

const REVOKE_DIALOG_CONTENT: ConfirmDialogContent = {
  title: 'Revoke all valid PIDs?',
  content: 'Are you sure that <i>all</i> valid PIDs shall get revoked?<br>This operation cannot be undone.',
  okButtonLabel: 'Revoke'
}

@Component({
  selector: 'revocation',
  standalone: true,
  imports: [
    BasicButtonComponent,
    ContentContainerComponent,
    NavigationContainerComponent,
    SummaryComponent
  ],
  templateUrl: './revocation.component.html',
  styleUrl: './revocation.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RevocationComponent {
  private readonly dialog = inject(Dialog);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationsService);
  private readonly issuanceService = inject(IssuanceService);
  private readonly authenticationService = inject(AuthenticationService);
  protected readonly issuanceCount = signal<IssuanceCount | undefined>(undefined);
  protected readonly revocable = computed<boolean>(() => Boolean(this.issuanceCount()?.revocable));


  constructor() {
    this.refreshSummary();
  }

  refreshSummary(): void {
    this.notifications.clearNotifications();
    this.fetchIssuanceCount();
  }

  handleRevocation(): void {
    const conf = ConfirmDialogComponent.getDefaultConfig();
    this.dialog.open(ConfirmDialogComponent, { ...conf, data: REVOKE_DIALOG_CONTENT }).closed
      .pipe(filter(dialogConfirmed), tap(() => this.notifications.clearNotifications()))
      .subscribe(() => this.revoke());
  }

  private fetchIssuanceCount(): void {
    this.issuanceService.serveIssuanceCount()
      .subscribe(this.issuanceCount.set);
  }

  private revoke(): void {
    this.issuanceService.revokePIDs()
      .pipe(
        tap({
          error: (err: HttpErrorResponse) => {
            if (err.status !== 0) {
              this.fetchIssuanceCount();
            }
          }
        })
      )
      .subscribe(() => {
        this.notifications.addSuccess('Revocation successful', `${ this.issuanceCount()?.revocable } PIDs were revoked.`);
        this.fetchIssuanceCount();
      });
  }

  logout(): void {
    this.notifications.clearNotifications();
    this.authenticationService.logout().subscribe({
      next: () => {
        this.notifications.addSuccess('Logout successful', 'You have been successfully logged out.');
        this.router.navigate(['/login']).catch(console.error);
      }
    });
  }
}
