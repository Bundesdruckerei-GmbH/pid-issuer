import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ZoneJsMitLicenseComponent } from './zone-js-mit-license.component';

describe('ZoneJsMitLicenseComponent', () => {
  let component: ZoneJsMitLicenseComponent;
  let fixture: ComponentFixture<ZoneJsMitLicenseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZoneJsMitLicenseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ZoneJsMitLicenseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
