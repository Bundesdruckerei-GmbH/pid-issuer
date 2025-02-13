import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfirmDialogComponent, ConfirmDialogContent } from './confirm-dialog.component';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { RouterModule } from '@angular/router';
import SpyObj = jasmine.SpyObj;

describe('ConfirmDialogComponent', () => {
  let component: ConfirmDialogComponent;
  let fixture: ComponentFixture<ConfirmDialogComponent>;
  const dialogMock: SpyObj<DialogRef> = jasmine.createSpyObj(DialogRef, ['close'])

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ConfirmDialogComponent,
        RouterModule.forRoot([])
      ],
      providers: [
        { provide: DIALOG_DATA, useValue: { title: 'test', content: 'test' } as ConfirmDialogContent },
        { provide: DialogRef, useValue: dialogMock }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
