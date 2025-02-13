import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AngularMitLicenseComponent } from './angular-mit-license.component';

describe('AngularMitLicenseComponent', () => {
  let component: AngularMitLicenseComponent;
  let fixture: ComponentFixture<AngularMitLicenseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AngularMitLicenseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AngularMitLicenseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
