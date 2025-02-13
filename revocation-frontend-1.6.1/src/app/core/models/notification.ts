import { AlertType } from '../../shared/components/alert/alert.component';

export class Notification {
  type: AlertType;
  title: string;
  text?: string;
  closeable?: boolean;

  constructor(type: AlertType, title: string, text?: string, closeable?: boolean) {
    this.type = type;
    this.title = title;
    this.text = text;
    this.closeable = closeable
  }
}
