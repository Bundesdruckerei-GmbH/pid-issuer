import { ErrorHandler, inject, Injectable } from '@angular/core';
import { NotificationsService } from './notifications.service';
import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ErrorResponse } from '../../api/models/errorResponse';


@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly notificationService = inject(NotificationsService);

  handleError(error: Error): void {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        this.notificationService.addError('Network error', 'Network connection could not be established.', false);
      } else if (error.status === HttpStatusCode.Unauthorized) {
        this.notificationService.addError('Authentication failed', 'Authentication is required, please login.', false);
      } else if (this.isErrorResponse(error.error)) {
        this.notificationService.addError('Unsuccessful request', error.error.message, false);
      } else {
        this.notificationService.addError('Unsuccessful request', undefined, false);
      }
    }
    console.error(error);
  }

  private isErrorResponse(error: any): error is ErrorResponse {
    return error && 'message' in error;
  }
}
