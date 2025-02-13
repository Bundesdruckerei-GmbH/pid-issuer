import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  vus: 100,
  duration: '30s',
};

export default function() {
  http.get('http://localhost:8090/5d6940eb-23a7-4e53-b220-2b591799eea8');
}
