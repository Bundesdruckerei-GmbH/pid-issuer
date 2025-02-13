import { SessionData } from './session-data';

describe('SessionData', () => {
  it('should create an instance', () => {
    expect(new SessionData('foo_123', 600)).toBeTruthy();
  });
});
