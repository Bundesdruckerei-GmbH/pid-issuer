import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RevocationComponent } from './revocation.component';
import { RouterModule } from '@angular/router';
import { IssuanceService } from '../../../../api/services/issuance.service';
import { of } from 'rxjs';
import { Dialog, DialogRef } from '@angular/cdk/dialog';
import { ConfirmDialogResult } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import SpyObj = jasmine.SpyObj;
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { BASE_PATH } from '../../../../api/variables';

describe('RevocationComponent', () => {
  let component: RevocationComponent;
  let fixture: ComponentFixture<RevocationComponent>;

  const issuanceMock: SpyObj<IssuanceService> = jasmine.createSpyObj(IssuanceService, ['serveIssuanceCount', 'revokePIDs']);
  const dialogMock: SpyObj<Dialog> = jasmine.createSpyObj(Dialog, ['open']);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RevocationComponent,
        RouterModule.forRoot([])
      ],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        { provide: IssuanceService, useValue: issuanceMock },
        { provide: Dialog, useValue: dialogMock },
        { provide: BASE_PATH, useValue: '/api' }
      ]
    })
      .compileComponents();

    issuanceMock.serveIssuanceCount.and.returnValue(of<any>({ issued: 5, revocable: 3 }));
    issuanceMock.serveIssuanceCount.calls.reset();
    issuanceMock.revokePIDs.calls.reset();

    fixture = TestBed.createComponent(RevocationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should revoke PIDs when confirmed', () => {
    dialogMock.open.and.returnValue({ closed: of(ConfirmDialogResult.ok) } as DialogRef);
    issuanceMock.revokePIDs.and.returnValue(of<any>(null));

    component.handleRevocation();

    expect(issuanceMock.revokePIDs).toHaveBeenCalled();
  });

  it('should not revoke PIDs when not confirmed', () => {
    dialogMock.open.and.returnValue({ closed: of(ConfirmDialogResult.cancel) } as DialogRef);

    component.handleRevocation();

    expect(issuanceMock.revokePIDs).not.toHaveBeenCalled();
  });
});
