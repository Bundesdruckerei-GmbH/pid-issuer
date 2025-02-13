import { ApplicationConfig, ErrorHandler, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { sessionIdInterceptor } from './core/interceptors/session-id.interceptor';
import { GlobalErrorHandler } from './core/services/global-error-handler.service';
import { environment } from '../environments/environment';
import { BASE_PATH } from './api/variables';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([sessionIdInterceptor])),
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    { provide: BASE_PATH, useValue: environment.baseUrl }
  ]
};
