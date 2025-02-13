import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [
    RouterLinkActive,
    RouterLink
  ],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss'
})
export class FooterComponent {
  protected readonly legalNoticeUrl = 'https://www.bundesdruckerei.de/en/legal-notice';
  protected readonly privacyTermsUrl = 'https://demo.pid-issuer.bundesdruckerei.de/privacy-terms';
}
