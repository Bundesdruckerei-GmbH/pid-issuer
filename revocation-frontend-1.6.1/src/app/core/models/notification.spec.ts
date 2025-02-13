import { Notification } from './notification';
import { AlertType } from '../../shared/components/alert/alert.component';

describe('Notification', () => {
  it('should create an instance', () => {
    expect(new Notification(AlertType.Info, 'Test Title', 'Test Text')).toBeTruthy();
  });
});
