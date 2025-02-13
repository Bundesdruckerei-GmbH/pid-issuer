import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RxjsApache2LicenseComponent } from './rxjs-apache-2-license.component';

describe('RxjsApache2LicenseComponent', () => {
  let component: RxjsApache2LicenseComponent;
  let fixture: ComponentFixture<RxjsApache2LicenseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RxjsApache2LicenseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RxjsApache2LicenseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
