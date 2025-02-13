import { TestBed } from '@angular/core/testing';

import { GlobalErrorHandler } from './global-error-handler.service';
import { ErrorHandler } from '@angular/core';

describe('GlobalErrorHandlerService', () => {
  let service: ErrorHandler;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ErrorHandler,
          useClass: GlobalErrorHandler
        }
      ]
    });
    service = TestBed.inject(ErrorHandler);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
