import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EntitiesBsd2ClauseComponent } from './entities-bsd-2-clause.component';

describe('EntitiesBsd2ClauseComponent', () => {
  let component: EntitiesBsd2ClauseComponent;
  let fixture: ComponentFixture<EntitiesBsd2ClauseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EntitiesBsd2ClauseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EntitiesBsd2ClauseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
