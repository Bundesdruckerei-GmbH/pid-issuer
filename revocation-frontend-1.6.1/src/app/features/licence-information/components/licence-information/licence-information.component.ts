import { Component, inject } from '@angular/core';
import { Location } from '@angular/common';
import { BasicButtonComponent } from '../../../../shared/components/basic-button/basic-button.component';
import { ContentContainerComponent } from '../../../../shared/layout/content-container/content-container.component';
import { NavigationContainerComponent } from '../../../../shared/layout/navigation-container/navigation-container.component';
import { AngularMitLicenseComponent } from './angular-mit-license/angular-mit-license.component';
import { AngularCdkMitLicenseComponent } from './angular-cdk-mit-license/angular-cdk-mit-license.component';
import { EntitiesBsd2ClauseComponent } from './entities-bsd-2-clause/entities-bsd-2-clause.component';
import {
  NgxDeviceDetectorMitLicenseComponent
} from './ngx-device-detector-mit-license/ngx-device-detector-mit-license.component';
import { Parse5MitLicenseComponent } from './parse5-mit-license/parse5-mit-license.component';
import { RxjsApache2LicenseComponent } from './rxjs-apache-2-license/rxjs-apache-2-license.component';
import { TslibBsdLicenseComponent } from './tslib-bsd-license/tslib-bsd-license.component';
import { ZoneJsMitLicenseComponent } from './zone-js-mit-license/zone-js-mit-license.component';

@Component({
  selector: 'licence-information',
  standalone: true,
  imports: [
    BasicButtonComponent,
    ContentContainerComponent,
    NavigationContainerComponent,
    AngularMitLicenseComponent,
    AngularCdkMitLicenseComponent,
    EntitiesBsd2ClauseComponent,
    NgxDeviceDetectorMitLicenseComponent,
    Parse5MitLicenseComponent,
    RxjsApache2LicenseComponent,
    TslibBsdLicenseComponent,
    ZoneJsMitLicenseComponent
  ],
  templateUrl: './licence-information.component.html',
  styleUrl: './licence-information.component.scss'
})
export class LicenceInformationComponent {
  private readonly location = inject(Location);

  navigateBack(): void {
    this.location.back();
  }
}
