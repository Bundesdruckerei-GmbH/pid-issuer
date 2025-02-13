import { booleanAttribute, ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Params, RouterLink } from '@angular/router';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'basic-button',
  standalone: true,
  imports: [
    RouterLink,
    IconComponent
  ],
  templateUrl: './basic-button.component.html',
  styleUrl: './basic-button.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BasicButtonComponent {
  readonly bordered = input(false, {transform: booleanAttribute});
  readonly small = input(false, { transform: booleanAttribute });
  readonly single = input(false, { transform: booleanAttribute });
  readonly buttonDeactivated = input(false, { transform: booleanAttribute });
  readonly iconUrl = input<string>();
  readonly buttonId = input<string>();
  readonly routerLinkValue = input<string>();
  readonly queryParams = input<Params>();
  readonly type = input<'submit' | 'reset' | 'button'>('button');
}
