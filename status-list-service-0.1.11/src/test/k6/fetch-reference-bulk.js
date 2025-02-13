import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
};

export default function() {
  var params = {
	headers: {
                "x-api-key": "af05bedc-ec26-472a-af56-f3b862e8e00d"
	}
  };
  http.post('http://localhost:8090/api/tests/new-references/25', null, params);
  sleep(0.25)
}
