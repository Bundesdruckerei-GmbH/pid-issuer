import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TslibBsdLicenseComponent } from './tslib-bsd-license.component';

describe('TslibBsdLicenseComponent', () => {
  let component: TslibBsdLicenseComponent;
  let fixture: ComponentFixture<TslibBsdLicenseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TslibBsdLicenseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TslibBsdLicenseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
