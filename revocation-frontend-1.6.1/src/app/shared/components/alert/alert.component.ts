import { booleanAttribute, Component, EventEmitter, Input, Output } from '@angular/core';
import { IconComponent } from '../icon/icon.component';

export enum AlertType {
  Error, Warning, Success, Info
}

@Component({
  selector: 'alert',
  standalone: true,
  imports: [
    IconComponent
  ],
  templateUrl: './alert.component.html',
  styleUrl: './alert.component.scss'
})
export class AlertComponent {
  @Input() type: AlertType = AlertType.Error;
  @Input({ required: true }) titleText!: string;
  @Input() id?: string;
  @Input({ transform: booleanAttribute }) borderLeft: boolean = false;
  @Input({ transform: booleanAttribute }) closeable: boolean = false;

  @Output() alertClose: EventEmitter<string | void> = new EventEmitter<string | void>();

  protected readonly AlertType = AlertType;
}
