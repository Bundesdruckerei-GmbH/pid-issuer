import { Component } from '@angular/core';
import { BasicButtonComponent } from '../../components/basic-button/basic-button.component';
import { NavigationContainerComponent } from '../navigation-container/navigation-container.component';

@Component({
  selector: 'content-container',
  standalone: true,
  imports: [
    BasicButtonComponent,
    NavigationContainerComponent
  ],
  templateUrl: './content-container.component.html',
  styleUrl: './content-container.component.scss'
})
export class ContentContainerComponent {

}
