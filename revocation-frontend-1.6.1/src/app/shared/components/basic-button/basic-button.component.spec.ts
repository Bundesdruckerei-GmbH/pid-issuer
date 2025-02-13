import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BasicButtonComponent } from './basic-button.component';
import { RouterModule } from '@angular/router';

describe('BasicButtonComponent', () => {
  let component: BasicButtonComponent;
  let fixture: ComponentFixture<BasicButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BasicButtonComponent,
        RouterModule.forRoot([])
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BasicButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
