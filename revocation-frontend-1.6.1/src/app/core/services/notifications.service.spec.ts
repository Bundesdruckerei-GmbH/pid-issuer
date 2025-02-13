import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { NotificationsService } from './notifications.service';
import { Notification } from '../models/notification';
import { AlertType } from '../../shared/components/alert/alert.component';

describe('NotificationsService', () => {
  let service: NotificationsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should initially have no notifications', () => {
    service.notifications$.subscribe({
      next: notifications => {
        expect(notifications.length).toBe(0);
      },
      error: () => fail('should not fail')
    });
  });

  it('should add error notification', () => {
    service.addError('Error', 'Something went wrong');

    service.notifications$.subscribe({
      next: notifications => {
        expect(notifications.length).toBe(1);
        expect(notifications[0].type).toEqual(AlertType.Error);
        expect(notifications[0].title).toEqual('Error');
        expect(notifications[0].text).toEqual('Something went wrong');
      },
      error: () => fail('should not fail')
    });
  });

  it('should add warning notification', () => {
    service.addWarning('Warning', 'There could be a problem');

    service.notifications$.subscribe({
      next: notifications => {
        expect(notifications.length).toBe(1);
        expect(notifications[0].type).toEqual(AlertType.Warning);
        expect(notifications[0].title).toEqual('Warning');
        expect(notifications[0].text).toEqual('There could be a problem');
      },
      error: () => fail('should not fail')
    });
  });

  it('should add info notification', () => {
    service.addInfo('Info', 'And now for something completely different');

    service.notifications$.subscribe({
      next: notifications => {
        expect(notifications.length).toBe(1);
        expect(notifications[0].type).toEqual(AlertType.Info);
        expect(notifications[0].title).toEqual('Info');
        expect(notifications[0].text).toEqual('And now for something completely different');
      },
      error: () => fail('should not fail')
    });
  });

  it('should add success notification', () => {
    service.addSuccess('Success', 'Everything went perfectly fine');

    service.notifications$.subscribe({
      next: notifications => {
        expect(notifications.length).toBe(1);
        expect(notifications[0].type).toEqual(AlertType.Success);
        expect(notifications[0].title).toEqual('Success');
        expect(notifications[0].text).toEqual('Everything went perfectly fine');
      },
      error: () => fail('should not fail')
    });
  });

  it('should add notifications and remove them after a given time', fakeAsync(() => {
    let itemCount= 3;

    service.addSuccess('Success', 'Close after 3 seconds', true, 3);
    service.addSuccess('Success', 'Close after 2 seconds', true, 2);
    service.addSuccess('Success', 'Close after 1 second',  true, 1);

    service.notifications$.subscribe({
      next: notifications => expect(notifications.length).toBe(itemCount--),
      error: () => fail('should not fail')
    });

    tick(1000);
    tick(1000);
    tick(1000);
  }));

  it('should clear notifications', () => {
    (service as any).notifications = [
      new Notification(AlertType.Success, 'foo'),
      new Notification(AlertType.Warning, 'bar'),
      new Notification(AlertType.Error, 'baz'),
      new Notification(AlertType.Info, 'qux')
    ];

    service.clearNotifications();

    service.notifications$.subscribe({
      next: notifications => {
        expect(notifications.length).toBe(0);
      },
      error: () => fail('should not fail')
    });
  });

  it('should add a notification', () => {
    const n = new Notification(AlertType.Success, 'foo');

    service.addNotification(n);

    service.notifications$.subscribe({
      next: notifications => {
        expect(notifications.length).toBe(1);
        expect(notifications[0].type).toEqual(AlertType.Success);
        expect(notifications[0].title).toEqual('foo');
      },
      error: () => fail('should not fail')
    });
  });

  it('should remove a notification', () => {
    const n0 = new Notification(AlertType.Success, 'foo');
    const n1 = new Notification(AlertType.Warning, 'bar');
    const n2 = new Notification(AlertType.Error, 'baz');
    const n3 = new Notification(AlertType.Info, 'qux');

    (service as any).notifications = [n0, n1, n2, n3];

    service.removeNotification(n2);

    service.notifications$.subscribe({
      next: notifications => {
        expect(notifications.length).toBe(3);
        expect(notifications[0].title).toEqual('foo');
        expect(notifications[1].title).toEqual('bar');
        expect(notifications[2].title).toEqual('qux');
      },
      error: () => fail('should not fail')
    });
  });
});
