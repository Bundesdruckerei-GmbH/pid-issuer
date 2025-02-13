import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DIALOG_DATA, DialogConfig, DialogRef } from '@angular/cdk/dialog';
import { BasicButtonComponent } from '../basic-button/basic-button.component';
import { IconComponent } from '../icon/icon.component';

export interface ConfirmDialogContent {
  title: string;
  content: string;
  okButtonLabel?: string;
  cancelButtonLabel?: string;
}

export enum ConfirmDialogResult {
  ok = 'ok', cancel = 'cancel'
}

export function dialogConfirmed(result?: ConfirmDialogResult): boolean {
  return result == ConfirmDialogResult.ok;
}

@Component({
  selector: 'confirm-dialog',
  standalone: true,
  imports: [
    BasicButtonComponent,
    IconComponent
  ],
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfirmDialogComponent {
  protected readonly dialogContent = inject<ConfirmDialogContent>(DIALOG_DATA);
  private readonly dialogRef = inject(DialogRef);

  static getDefaultConfig(): DialogConfig<ConfirmDialogContent, DialogRef<ConfirmDialogResult, ConfirmDialogComponent>> {
    const dialogConfig = new DialogConfig<ConfirmDialogContent, DialogRef<ConfirmDialogResult, ConfirmDialogComponent>>();
    dialogConfig.width = '32rem';
    dialogConfig.disableClose = true;
    dialogConfig.restoreFocus = true;
    dialogConfig.ariaModal = true;
    dialogConfig.ariaLabelledBy = 'confirm-dialog-title';
    return dialogConfig;
  }

  closeOk(): void {
    this.dialogRef.close(ConfirmDialogResult.ok);
  }

  closeCancel(): void {
    this.dialogRef.close(ConfirmDialogResult.cancel);
  }
}
