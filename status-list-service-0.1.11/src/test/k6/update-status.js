import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  vus: 100,
  duration: '30s',
};

export default function() {
  var params = {
	headers: {
                "x-api-key": "af05bedc-ec26-472a-af56-f3b862e8e00d",
		"content-type": "application/json"
	}
  };
  var body = JSON.stringify({
	uri: "http://localhost:8090/a78feb9c-6503-4aab-a3c8-2417fbc89f6b",
	index: 2206,
	value: 1
  });
  http.post('http://localhost:8090/api/update-status', body, params);
}
