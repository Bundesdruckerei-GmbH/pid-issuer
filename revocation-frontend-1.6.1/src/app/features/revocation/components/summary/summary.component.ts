import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IssuanceCount } from '../../../../api/models/issuanceCount';

@Component({
  selector: 'revocation-summary',
  standalone: true,
  imports: [],
  templateUrl: './summary.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SummaryComponent {
  readonly issuanceCount = input<IssuanceCount>();
}
