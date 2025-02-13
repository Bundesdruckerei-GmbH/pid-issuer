import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TitleHeaderComponent } from './core/components/title-header/title-header.component';
import { FooterComponent } from './core/components/footer/footer.component';
import { NotificationsComponent } from './core/components/notifications/notifications.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, TitleHeaderComponent, FooterComponent, NotificationsComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {

}
