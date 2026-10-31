import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 500,          // 500 clients at the same time
  duration: '30s',
};

export default function () {
  const key = `load-${__VU}-${__ITER}`;
  const res = http.post(
    'http://localhost:9090/charges',
    JSON.stringify({ reference: key, amount: 1000, currency: 'VND', cardToken: 'tok_visa' }),
    { headers: { 'Content-Type': 'application/json', 'Idempotency-Key': key } },
  );
  check(res, { 'charged': (r) => r.status === 201 });
}