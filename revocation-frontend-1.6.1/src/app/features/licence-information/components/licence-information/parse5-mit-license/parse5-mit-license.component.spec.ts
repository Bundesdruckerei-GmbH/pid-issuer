import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Parse5MitLicenseComponent } from './parse5-mit-license.component';

describe('Parse5MitLicenseComponent', () => {
  let component: Parse5MitLicenseComponent;
  let fixture: ComponentFixture<Parse5MitLicenseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Parse5MitLicenseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Parse5MitLicenseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
