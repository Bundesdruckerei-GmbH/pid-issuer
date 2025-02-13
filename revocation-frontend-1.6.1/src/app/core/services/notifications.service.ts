import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AlertType } from '../../shared/components/alert/alert.component';
import { Notification } from '../models/notification';

@Injectable({
  providedIn: 'root'
})
export class NotificationsService {
  private notifications: Notification[] = [];
  private readonly notificationsSubject = new BehaviorSubject(<Notification[]>[]);
  public notifications$ = this.notificationsSubject.asObservable();

  constructor() { }

  public addError(title: string, text?: string, closeable?: boolean): void {
    this.createNotification(AlertType.Error, title, text, closeable);
  }

  public addWarning(title: string, text?: string, closeable?: boolean, closeAfterSeconds?: number): void {
    this.createNotification(AlertType.Warning, title, text, closeable, closeAfterSeconds);
  }

  public addInfo(title: string, text?: string, closeable?: boolean, closeAfterSeconds?: number): void {
    this.createNotification(AlertType.Info, title, text, closeable, closeAfterSeconds);
  }

  public addSuccess(title: string, text?: string, closeable?: boolean, closeAfterSeconds?: number): void {
    this.createNotification(AlertType.Success, title, text, closeable, closeAfterSeconds);
  }

  public clearNotifications(): void {
    this.notifications = [];
    this.notificationsSubject.next(this.notifications);
  }

  public addNotification(notification: Notification): void {
    this.notifications.push(notification);
    this.notificationsSubject.next(this.notifications);
  }

  public removeNotification(notification: Notification): void {
    this.notifications = this.notifications.filter(item => item !== notification);
    this.notificationsSubject.next(this.notifications);
  }

  private createNotification(type: AlertType, title: string, text?: string, closeable: boolean = true, closeAfterSeconds: number = -1): void {
    const notification = new Notification(type, title, text, closeable);
    this.addNotification(notification);

    if (closeAfterSeconds > 0) {
      window.setTimeout(() => this.removeNotification(notification), closeAfterSeconds * 1000);
    }
  }
}
