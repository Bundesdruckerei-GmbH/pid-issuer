import { Component } from '@angular/core';
import { NotificationsService } from '../../services/notifications.service';
import { AsyncPipe } from '@angular/common';
import { AlertComponent } from '../../../shared/components/alert/alert.component';

@Component({
  selector: 'notifications',
  standalone: true,
  imports: [
    AsyncPipe,
    AlertComponent
  ],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss'
})
export class NotificationsComponent {
  constructor(protected notifications: NotificationsService) {
  }
}
