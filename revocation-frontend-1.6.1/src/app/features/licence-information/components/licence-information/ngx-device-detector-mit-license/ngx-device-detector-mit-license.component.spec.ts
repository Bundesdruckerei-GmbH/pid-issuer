import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NgxDeviceDetectorMitLicenseComponent } from './ngx-device-detector-mit-license.component';

describe('NgxDeviceDetectorMitLicenseComponent', () => {
  let component: NgxDeviceDetectorMitLicenseComponent;
  let fixture: ComponentFixture<NgxDeviceDetectorMitLicenseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NgxDeviceDetectorMitLicenseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NgxDeviceDetectorMitLicenseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
