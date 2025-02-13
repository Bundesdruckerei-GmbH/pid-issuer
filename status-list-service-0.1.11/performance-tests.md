# Performance tests

The directory src/test/k6 contains some k6 performance tests.

Details on k6 installation und usage can be found at:

- https://k6.io/docs/get-started/installation/
- https://k6.io/docs/get-started/running-k6/

## Test hardware

- Intel(R) Core(TM) i7-10610U CPU @ 1.80GHz
- 512GB SSD Samsung MZVLB512HAJQ-00007
- 8GBG DDR4 2667 MHz Fujitsu Onboard RAM
- 32GB DDR4-3200 (M471A4G43AB1-CWE) RAM
- Redis is running on the same machine as the service

## Configurations

Different configurations were used for the test scenarios.

### append-fsync-always.delay

```yaml
app:
  ...
  redis:
    persistence-strategy:
      append-fsync-always: true
  status-list-pools:
    tests:
      api-key: af05bedc-ec26-472a-af56-f3b862e8e00d
      issuer: http://localhost:8080
      size: 50000
      bits: 1
      precreation:
        lists: 1
        check-delay: 40s
      prefetch:
        threshold: 1000
        capacity: 10000
        on-underflow: delay
...
```

### append-fsync-always.fail

```yaml
app:
  ...
  redis:
    persistence-strategy:
      append-fsync-always: true
  status-list-pools:
    tests:
      api-key: af05bedc-ec26-472a-af56-f3b862e8e00d
      issuer: http://localhost:8080
      size: 50000
      bits: 1
      precreation:
        lists: 1
        check-delay: 40s
      prefetch:
        threshold: 1000
        capacity: 10000
        on-underflow: fail
...
```

## Result summary and scalability considerations

For testing status lists with a size of 50000 were used.

Fetching of status lists through the service allows up to 11271 status lists served per second. Due to the possibility
to serve the statically generated lists using a webserver this can be scaled with ease.

Updates were possible at a rate of 425 per second and took 23.49ms on average with 10 VUs* and at 2595 per second with
an average runtime of 37.56ms with 100 VUs. Redis is the bottleneck here and thus this value will probably not scale
when using multiple instances. Nevertheless, in the current implementation 5 redis commands are issued per updated
status which could be reduced to 2 when caching of the status list configuration is used. Removing the version feature (
causing status list JWT updates even without change) will lead to a single command. This could increase the throughput
by up to 300%, so 1200 - 7500 updates per second seem possible, but this would have to be checked, because the
individual runtimes of the redis commands involved are unknown. For bulk changes, for example the full revocation of a
status list, a more efficient operation could be implemented if needed.

Fetching new references is amazingly fast. With append-fsync-always storage strategy and the delay strategy for buffer
underflows, leading to zero failed requests, a rate of 8435 references per second could be generated with 10 VUs and
10163 per seconds with 100VUs. Due to prefetching, the load on redis is negligible and thus this will scale almost
linear with the number of instances of the service that run.

The prefetch buffer capacity was set to 10000, prefetch threshold was set to 1000 for append-fsync-always strategy.
Tuning this values can lead to less request errors for the fail strategy for buffer underflows and faster response times
for the delay strategy. A list size increase will also influence the same factors.

`*` A VUs in k6 is a virtual user, an entity performing requests in parallel to other entities. This roughly corresponds
to a thread in case of the status update and reference creation cases and to the number of wallets / verifiers
performing requests in the case of delivering that status list.

## Result details

When nothing else is mentioned requests were performed with 10 VUs (parallel users). The list contains the test file
name followed by the used configuration.

- `fetch-status-list.js` (append-fsync-always.delay, but should be irrelevant) - Tests fetching of status lists as fast
  as possible

  With 100 parallel users a single list could be retrieved on average in 8.84ms and around 11271 lists could be provided
  per second.
- `update-status.js` (append-fsync-always.delay) - Tests updates of status lists as fast as possible

  10 VUs: Updates took 23.49ms on average and around 425 updates could be performed per second.

  100 VUs: Updates took 37.56ms on average and around 2595 updates could be performed per second.
- `fetch-reference-<variant>.js` - Fetches status references, variants tested:
    - `basic` (append-fsync-always.delay)- Fetched less references as available per second in the prefetch buffer

      Around 992 references per second were requested. Fetching took 2.06ms. The prefetch buffer did not underflow.
    - `bulk` (append-fsync-always.delay) - Fetched less references as available per second in the prefetch buffer,
      fetching 25 status at a time

      Around 991 references per second were requested. Fetching took 2.23ms. The prefetch buffer did not underflow.
    - `unlimited` (append-fsync-always.delay) - Fetched as many status as possible

      10 VUs: Around 8435 references were fetched per second. Fetching took 1.17ms on average. No prefetch buffer
      underflow occurred.

      100 VUs: Around 10163 references were fetched per second. Fetching took 9.8ms on average with a maximum of 125ms.
      There were some prefetch buffer underflows.
    - `unlimited` (append-fsync-always.fail) - Fetched as many status as possible

      10 VUs: Around 8471 references were fetched per second. Fetching took 1.02ms on average. No prefetch buffer
      underflow occurred.

      100 VUs: Around 10956 references were fetched per second. Fetching took 9.08ms on average. Around 0.03% of the
      requests failed due to a prefetch buffer underflow.

## Results statistics

### fetch-status-list.js

```
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

     execution: local
        script: fetch-status-list.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 100 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 930 MB 31 MB/s
     data_sent......................: 39 MB  1.3 MB/s
     http_req_blocked...............: avg=15.81µs  min=856ns    med=1.93µs  max=69.43ms  p(90)=3.07µs  p(95)=4.14µs  
     http_req_connecting............: avg=5.37µs   min=0s       med=0s      max=35.04ms  p(90)=0s      p(95)=0s      
     http_req_duration..............: avg=8.72ms   min=265.36µs med=6.82ms  max=130.71ms p(90)=15.84ms p(95)=21.29ms 
       { expected_response:true }...: avg=8.72ms   min=265.36µs med=6.82ms  max=130.71ms p(90)=15.84ms p(95)=21.29ms 
     http_req_failed................: 0.00%  ✓ 0            ✗ 338222
     http_req_receiving.............: avg=140.78µs min=10.66µs  med=24.53µs max=57.38ms  p(90)=48.01µs p(95)=106.46µs
     http_req_sending...............: avg=39.63µs  min=4.23µs   med=8.98µs  max=52.46ms  p(90)=15.65µs p(95)=25.21µs 
     http_req_tls_handshaking.......: avg=0s       min=0s       med=0s      max=0s       p(90)=0s      p(95)=0s      
     http_req_waiting...............: avg=8.54ms   min=232.84µs med=6.74ms  max=130.44ms p(90)=15.5ms  p(95)=20.67ms 
     http_reqs......................: 338222 11271.882954/s
     iteration_duration.............: avg=8.84ms   min=299.9µs  med=6.91ms  max=130.77ms p(90)=16.07ms p(95)=21.66ms 
     iterations.....................: 338222 11271.882954/s
     vus............................: 91     min=91         max=100 
     vus_max........................: 100    min=100        max=100 


running (0m30.0s), 000/100 VUs, 338222 complete and 0 interrupted iterations
```

### update-status.js (append-fsync-always.delay)

#### 10 VUs

```
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

     execution: local
        script: update-status.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 10 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 692 kB 23 kB/s
     data_sent......................: 3.7 MB 123 kB/s
     http_req_blocked...............: avg=5.04µs  min=1.48µs med=2.79µs  max=546.46µs p(90)=4.21µs  p(95)=5.83µs 
     http_req_connecting............: avg=1.18µs  min=0s     med=0s      max=468.45µs p(90)=0s      p(95)=0s     
     http_req_duration..............: avg=23.37ms min=9.89ms med=23.13ms max=54.28ms  p(90)=27.45ms p(95)=29.65ms
       { expected_response:true }...: avg=23.37ms min=9.89ms med=23.13ms max=54.28ms  p(90)=27.45ms p(95)=29.65ms
     http_req_failed................: 0.00%  ✓ 0          ✗ 12765
     http_req_receiving.............: avg=33.08µs min=8.58µs med=27.93µs max=221.43µs p(90)=53.15µs p(95)=64.19µs
     http_req_sending...............: avg=21.13µs min=8.53µs med=18.69µs max=390.22µs p(90)=30.42µs p(95)=36.89µs
     http_req_tls_handshaking.......: avg=0s      min=0s     med=0s      max=0s       p(90)=0s      p(95)=0s     
     http_req_waiting...............: avg=23.32ms min=9.86ms med=23.08ms max=54.22ms  p(90)=27.39ms p(95)=29.6ms 
     http_reqs......................: 12765  425.148508/s
     iteration_duration.............: avg=23.49ms min=9.97ms med=23.25ms max=54.41ms  p(90)=27.58ms p(95)=29.76ms
     iterations.....................: 12765  425.148508/s
     vus............................: 10     min=10       max=10 
     vus_max........................: 10     min=10       max=10 


running (0m30.0s), 00/10 VUs, 12765 complete and 0 interrupted iterations
```

#### 100 VUs

```
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

     execution: local
        script: update-status.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 100 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 4.3 MB 144 kB/s
     data_sent......................: 23 MB  771 kB/s
     http_req_blocked...............: avg=6.29µs  min=909ns   med=2.05µs  max=6.31ms  p(90)=4.09µs  p(95)=6.94µs 
     http_req_connecting............: avg=1.82µs  min=0s      med=0s      max=3.96ms  p(90)=0s      p(95)=0s     
     http_req_duration..............: avg=37.42ms min=19.27ms med=36.52ms max=83.71ms p(90)=43.45ms p(95)=49.03ms
       { expected_response:true }...: avg=37.42ms min=19.27ms med=36.52ms max=83.71ms p(90)=43.45ms p(95)=49.03ms
     http_req_failed................: 0.00%  ✓ 0           ✗ 79864
     http_req_receiving.............: avg=40.29µs min=6.31µs  med=15.41µs max=14.44ms p(90)=51.38µs p(95)=95.32µs
     http_req_sending...............: avg=22.25µs min=5.47µs  med=12.12µs max=6.25ms  p(90)=27.3µs  p(95)=44.2µs 
     http_req_tls_handshaking.......: avg=0s      min=0s      med=0s      max=0s      p(90)=0s      p(95)=0s     
     http_req_waiting...............: avg=37.36ms min=19.17ms med=36.47ms max=75.47ms p(90)=43.37ms p(95)=48.95ms
     http_reqs......................: 79864  2659.596795/s
     iteration_duration.............: avg=37.56ms min=20.1ms  med=36.65ms max=83.94ms p(90)=43.62ms p(95)=49.19ms
     iterations.....................: 79864  2659.596795/s
     vus............................: 100    min=100       max=100
     vus_max........................: 100    min=100       max=100


running (0m30.0s), 000/100 VUs, 79864 complete and 0 interrupted iterations
```

### fetch-reference-basic.js (append-fsync-always.delay)

There is a 10ms wait after each request, thus the actual duration is 12.06ms - 10ms.

```
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

     execution: local
        script: fetch-reference-basic.js
        output: -

     scenarios: (100.00%) 1 scenario, 12 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 12 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 6.2 MB 206 kB/s
     data_sent......................: 5.1 MB 171 kB/s
     http_req_blocked...............: avg=7.64µs  min=1.19µs   med=3.41µs  max=4.65ms   p(90)=7.06µs   p(95)=10.13µs 
     http_req_connecting............: avg=2.42µs  min=0s       med=0s      max=4.58ms   p(90)=0s       p(95)=0s      
     http_req_duration..............: avg=1.41ms  min=346.9µs  med=1.18ms  max=152.12ms p(90)=2.03ms   p(95)=2.52ms  
       { expected_response:true }...: avg=1.41ms  min=346.9µs  med=1.18ms  max=152.12ms p(90)=2.03ms   p(95)=2.52ms  
     http_req_failed................: 0.00%  ✓ 0          ✗ 29788
     http_req_receiving.............: avg=79.68µs min=13.17µs  med=48.71µs max=5.8ms    p(90)=151.13µs p(95)=231.88µs
     http_req_sending...............: avg=20.55µs min=6.48µs   med=14.34µs max=9.07ms   p(90)=30.49µs  p(95)=40.46µs 
     http_req_tls_handshaking.......: avg=0s      min=0s       med=0s      max=0s       p(90)=0s       p(95)=0s      
     http_req_waiting...............: avg=1.31ms  min=286.13µs med=1.09ms  max=149.95ms p(90)=1.9ms    p(95)=2.37ms  
     http_reqs......................: 29788  992.739356/s
     iteration_duration.............: avg=12.06ms min=10.46ms  med=11.89ms max=163.04ms p(90)=12.74ms  p(95)=13.29ms 
     iterations.....................: 29788  992.739356/s
     vus............................: 12     min=12       max=12 
     vus_max........................: 12     min=12       max=12 


running (0m30.0s), 00/12 VUs, 29788 complete and 0 interrupted iterations
```

### fetch-reference-bulk.js (append-fsync-always.delay)

There is a 250ms wait after each request, thus the actual duration is 252.23ms - 250ms.

```
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

     execution: local
        script: fetch-reference-bulk.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 10 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 2.6 MB 87 kB/s
     data_sent......................: 209 kB 7.0 kB/s
     http_req_blocked...............: avg=8.69µs   min=1.4µs    med=3.63µs   max=385.85µs p(90)=6.77µs   p(95)=8.96µs  
     http_req_connecting............: avg=2.79µs   min=0s       med=0s       max=304.45µs p(90)=0s       p(95)=0s      
     http_req_duration..............: avg=1.51ms   min=481.94µs med=1.36ms   max=5.68ms   p(90)=2.1ms    p(95)=2.65ms  
       { expected_response:true }...: avg=1.51ms   min=481.94µs med=1.36ms   max=5.68ms   p(90)=2.1ms    p(95)=2.65ms  
     http_req_failed................: 0.00%  ✓ 0         ✗ 1190
     http_req_receiving.............: avg=53.65µs  min=12.47µs  med=42.5µs   max=756.65µs p(90)=90.53µs  p(95)=123.77µs
     http_req_sending...............: avg=19.55µs  min=7.17µs   med=14.95µs  max=935.17µs p(90)=30.96µs  p(95)=38.97µs 
     http_req_tls_handshaking.......: avg=0s       min=0s       med=0s       max=0s       p(90)=0s       p(95)=0s      
     http_req_waiting...............: avg=1.43ms   min=407.67µs med=1.29ms   max=5.16ms   p(90)=2.04ms   p(95)=2.55ms  
     http_reqs......................: 1190   39.642252/s
     iteration_duration.............: avg=252.23ms min=250.71ms med=252.18ms max=256.51ms p(90)=252.92ms p(95)=253.44ms
     iterations.....................: 1190   39.642252/s
     vus............................: 10     min=10      max=10
     vus_max........................: 10     min=10      max=10


running (0m30.0s), 00/10 VUs, 1190 complete and 0 interrupted iterations
```

### fetch-reference-unlimited.js (append-fsync-always.delay)

#### 10 VUs

```
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

     execution: local
        script: fetch-reference-unlimited.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 10 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 52 MB  1.7 MB/s
     data_sent......................: 44 MB  1.5 MB/s
     http_req_blocked...............: avg=4.79µs  min=827ns    med=2.25µs   max=6.97ms   p(90)=2.94µs  p(95)=3.7µs   
     http_req_connecting............: avg=1.62µs  min=0s       med=0s       max=4.85ms   p(90)=0s      p(95)=0s      
     http_req_duration..............: avg=1.1ms   min=331µs    med=950.49µs max=168.78ms p(90)=1.6ms   p(95)=1.99ms  
       { expected_response:true }...: avg=1.1ms   min=331µs    med=950.49µs max=168.78ms p(90)=1.6ms   p(95)=1.99ms  
     http_req_failed................: 0.00%  ✓ 0           ✗ 253073
     http_req_receiving.............: avg=64.94µs min=10.51µs  med=46.14µs  max=9.99ms   p(90)=98.87µs p(95)=152.75µs
     http_req_sending...............: avg=12.06µs min=4.73µs   med=10.64µs  max=9.69ms   p(90)=13.89µs p(95)=16.58µs 
     http_req_tls_handshaking.......: avg=0s      min=0s       med=0s       max=0s       p(90)=0s      p(95)=0s      
     http_req_waiting...............: avg=1.02ms  min=279.88µs med=879.33µs max=166.68ms p(90)=1.51ms  p(95)=1.88ms  
     http_reqs......................: 253073 8435.488178/s
     iteration_duration.............: avg=1.17ms  min=377.54µs med=1.01ms   max=169.28ms p(90)=1.67ms  p(95)=2.07ms  
     iterations.....................: 253073 8435.488178/s
     vus............................: 10     min=10        max=10  
     vus_max........................: 10     min=10        max=10  


running (0m30.0s), 00/10 VUs, 253073 complete and 0 interrupted iterations
```

#### 100 VUs

```
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

     execution: local
        script: fetch-reference-unlimited.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 100 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 63 MB  2.1 MB/s
     data_sent......................: 53 MB  1.7 MB/s
     http_req_blocked...............: avg=17.29µs  min=877ns    med=2.05µs  max=48.8ms   p(90)=3.17µs  p(95)=4.24µs  
     http_req_connecting............: avg=6.31µs   min=0s       med=0s      max=43.05ms  p(90)=0s      p(95)=0s      
     http_req_duration..............: avg=9.62ms   min=336.81µs med=8.29ms  max=125.11ms p(90)=16.06ms p(95)=20.21ms 
       { expected_response:true }...: avg=9.62ms   min=336.81µs med=8.29ms  max=125.11ms p(90)=16.06ms p(95)=20.21ms 
     http_req_failed................: 0.00%  ✓ 0            ✗ 305137
     http_req_receiving.............: avg=197.95µs min=11.09µs  med=27µs    max=81.37ms  p(90)=94.69µs p(95)=242.08µs
     http_req_sending...............: avg=50.3µs   min=4.45µs   med=10.19µs max=80.33ms  p(90)=17.55µs p(95)=27.75µs 
     http_req_tls_handshaking.......: avg=0s       min=0s       med=0s      max=0s       p(90)=0s      p(95)=0s      
     http_req_waiting...............: avg=9.37ms   min=288.87µs med=8.16ms  max=111.04ms p(90)=15.71ms p(95)=19.66ms 
     http_reqs......................: 305137 10163.439795/s
     iteration_duration.............: avg=9.8ms    min=408.13µs med=8.41ms  max=125.17ms p(90)=16.36ms p(95)=20.72ms 
     iterations.....................: 305137 10163.439795/s
     vus............................: 98     min=98         max=100 
     vus_max........................: 100    min=100        max=100 


running (0m30.0s), 000/100 VUs, 305137 complete and 0 interrupted iterations
```

### fetch-reference-unlimited.js (append-fsync-always.fail)

#### 10 VUs

```
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

     execution: local
        script: fetch-reference-unlimited.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 10 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 53 MB  1.8 MB/s
     data_sent......................: 44 MB  1.5 MB/s
     http_req_blocked...............: avg=4.9µs   min=909ns    med=2.2µs    max=9.14ms   p(90)=2.83µs   p(95)=3.58µs  
     http_req_connecting............: avg=1.81µs  min=0s       med=0s       max=9.07ms   p(90)=0s       p(95)=0s      
     http_req_duration..............: avg=1.09ms  min=327.01µs med=943.19µs max=152.35ms p(90)=1.6ms    p(95)=2ms     
       { expected_response:true }...: avg=1.09ms  min=327.01µs med=943.19µs max=152.35ms p(90)=1.6ms    p(95)=2ms     
     http_req_failed................: 0.00%  ✓ 0           ✗ 254146
     http_req_receiving.............: avg=65.51µs min=10.12µs  med=46.52µs  max=10.36ms  p(90)=100.54µs p(95)=154.98µs
     http_req_sending...............: avg=11.94µs min=4.74µs   med=10.52µs  max=12.76ms  p(90)=13.68µs  p(95)=16.47µs 
     http_req_tls_handshaking.......: avg=0s      min=0s       med=0s       max=0s       p(90)=0s       p(95)=0s      
     http_req_waiting...............: avg=1.02ms  min=286.77µs med=872.05µs max=149.45ms p(90)=1.51ms   p(95)=1.9ms   
     http_reqs......................: 254146 8471.172374/s
     iteration_duration.............: avg=1.16ms  min=380.83µs med=1ms      max=152.68ms p(90)=1.67ms   p(95)=2.09ms  
     iterations.....................: 254146 8471.172374/s
     vus............................: 10     min=10        max=10  
     vus_max........................: 10     min=10        max=10  


running (0m30.0s), 00/10 VUs, 254146 complete and 0 interrupted iterations
```

#### 100 VUs

```
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

     execution: local
        script: fetch-reference-unlimited.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 100 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 68 MB  2.3 MB/s
     data_sent......................: 57 MB  1.9 MB/s
     http_req_blocked...............: avg=16.73µs  min=909ns    med=2.04µs  max=66.69ms  p(90)=2.95µs  p(95)=3.81µs  
     http_req_connecting............: avg=5.91µs   min=0s       med=0s      max=22.41ms  p(90)=0s      p(95)=0s      
     http_req_duration..............: avg=8.91ms   min=364.97µs med=7.87ms  max=304.83ms p(90)=14.3ms  p(95)=17.29ms 
       { expected_response:true }...: avg=8.9ms    min=364.97µs med=7.87ms  max=304.83ms p(90)=14.29ms p(95)=17.25ms 
     http_req_failed................: 0.09%  ✓ 297          ✗ 328764
     http_req_receiving.............: avg=167.89µs min=11.19µs  med=26.59µs max=70.92ms  p(90)=95.25µs p(95)=231.27µs
     http_req_sending...............: avg=43.62µs  min=4.56µs   med=10.09µs max=66.84ms  p(90)=15.98µs p(95)=24.44µs 
     http_req_tls_handshaking.......: avg=0s       min=0s       med=0s      max=0s       p(90)=0s      p(95)=0s      
     http_req_waiting...............: avg=8.7ms    min=293.22µs med=7.75ms  max=266.07ms p(90)=14.04ms p(95)=16.85ms 
     http_reqs......................: 329061 10966.537499/s
     iteration_duration.............: avg=9.08ms   min=439.6µs  med=7.98ms  max=306.12ms p(90)=14.57ms p(95)=17.77ms 
     iterations.....................: 329061 10966.537499/s
     vus............................: 100    min=100        max=100 
     vus_max........................: 100    min=100        max=100 


running (0m30.0s), 000/100 VUs, 329061 complete and 0 interrupted iterations
```