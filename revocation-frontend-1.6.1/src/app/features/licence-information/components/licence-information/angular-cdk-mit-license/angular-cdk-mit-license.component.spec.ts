import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AngularCdkMitLicenseComponent } from './angular-cdk-mit-license.component';

describe('AngularCdkMitLicenseComponent', () => {
  let component: AngularCdkMitLicenseComponent;
  let fixture: ComponentFixture<AngularCdkMitLicenseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AngularCdkMitLicenseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AngularCdkMitLicenseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
