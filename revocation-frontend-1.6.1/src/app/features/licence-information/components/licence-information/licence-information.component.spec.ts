import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LicenceInformationComponent } from './licence-information.component';
import { RouterModule } from '@angular/router';

describe('LicenceInformationComponent', () => {
  let component: LicenceInformationComponent;
  let fixture: ComponentFixture<LicenceInformationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        LicenceInformationComponent,
        RouterModule.forRoot([])
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LicenceInformationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
