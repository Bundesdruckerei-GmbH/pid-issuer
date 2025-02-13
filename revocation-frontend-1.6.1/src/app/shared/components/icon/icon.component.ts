import { Component, Input } from '@angular/core';
import { NgStyle } from '@angular/common';

@Component({
  selector: 'icon',
  standalone: true,
  imports: [
    NgStyle
  ],
  templateUrl: './icon.component.html',
  styleUrl: './icon.component.scss'
})
export class IconComponent {
  @Input({ required: true }) url!: string;
  @Input() titleText?: string;
  @Input() rotation?: number;

  protected isInteger = Number.isInteger;
}
