# Performance tests with the actual hardware from TR

The directory src/test/k6 contains some k6 performance tests.
for additional information look at [performance-tests.md](performance-tests.md)

This document shows the results of the performance tests using Redis with the current hardware and software 
in order to later compare the performance when using a Postgres database.

## Test hardware / software
- LeanClient with Windows 11 and active Microsoft Defender
- Intel(R) Core(TM) i7-1185G7 @ 3.00GHz   3.00 GHz
- 512GB SSD 
- 32GB RAM
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

## Results statistics

### fetch-status-list.js
(This test need a URI to an existing list)

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


     data_received..................: 393 MB 13 MB/s
     data_sent......................: 15 MB  495 kB/s
     http_req_blocked...............: avg=141.7µs  min=0s med=0s      max=5.14s p(90)=0s      p(95)=0s
     http_req_connecting............: avg=54.2µs   min=0s med=0s      max=5.04s p(90)=0s      p(95)=0s
     http_req_duration..............: avg=22.12ms  min=0s med=12.02ms max=5.29s p(90)=42.07ms p(95)=55.78ms
       { expected_response:true }...: avg=22.12ms  min=0s med=12.02ms max=5.29s p(90)=42.07ms p(95)=55.78ms
     http_req_failed................: 0.00%  ✓ 0           ✗ 128142
     http_req_receiving.............: avg=2.71ms   min=0s med=0s      max=5.25s p(90)=526.9µs p(95)=1.01ms
     http_req_sending...............: avg=246.16µs min=0s med=0s      max=5.09s p(90)=0s      p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s med=0s      max=0s    p(90)=0s      p(95)=0s
     http_req_waiting...............: avg=19.15ms  min=0s med=11.61ms max=5.14s p(90)=40.01ms p(95)=49.96ms
     http_reqs......................: 128142 4268.529087/s
     iteration_duration.............: avg=23.31ms  min=0s med=12.44ms max=5.29s p(90)=43.27ms p(95)=58.39ms
     iterations.....................: 128142 4268.529087/s
     vus............................: 100    min=100       max=100
     vus_max........................: 100    min=100       max=100

                                      
running (0m30.0s), 000/100 VUs, 128142 complete and 0 interrupted iterations 
```

### update-status.js (append-fsync-always.delay)
(This test need a URI at the body to an existing list)

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


     data_received..................: 680 kB 23 kB/s
     data_sent......................: 3.6 MB 121 kB/s
     http_req_blocked...............: avg=21.21µs min=0s     med=0s      max=14.33ms  p(90)=0s       p(95)=0s
     http_req_connecting............: avg=5.84µs  min=0s     med=0s      max=2.16ms   p(90)=0s       p(95)=0s
     http_req_duration..............: avg=23.72ms min=9.12ms med=23.01ms max=120.93ms p(90)=30.44ms  p(95)=33.49ms
       { expected_response:true }...: avg=23.72ms min=9.12ms med=23.01ms max=120.93ms p(90)=30.44ms  p(95)=33.49ms
     http_req_failed................: 0.00%  ✓ 0          ✗ 12546
     http_req_receiving.............: avg=63.16µs min=0s     med=0s      max=5.52ms   p(90)=470.55µs p(95)=524µs
     http_req_sending...............: avg=26.78µs min=0s     med=0s      max=2.24ms   p(90)=0s       p(95)=129.02µs
     http_req_tls_handshaking.......: avg=0s      min=0s     med=0s      max=0s       p(90)=0s       p(95)=0s
     http_req_waiting...............: avg=23.63ms min=9.12ms med=22.9ms  max=120.93ms p(90)=30.34ms  p(95)=33.43ms
     http_reqs......................: 12546  417.950247/s
     iteration_duration.............: avg=23.89ms min=9.63ms med=23.17ms max=135.28ms p(90)=30.54ms  p(95)=33.67ms
     iterations.....................: 12546  417.950247/s
     vus............................: 10     min=10       max=10
     vus_max........................: 10     min=10       max=10


running (0m30.0s), 00/10 VUs, 12546 complete and 0 interrupted iterations
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


     data_received..................: 3.8 MB 125 kB/s
     data_sent......................: 20 MB  669 kB/s
     http_req_blocked...............: avg=26.41µs min=0s      med=0s      max=13.4ms   p(90)=0s      p(95)=0s
     http_req_connecting............: avg=11.49µs min=0s      med=0s      max=13.12ms  p(90)=0s      p(95)=0s
     http_req_duration..............: avg=43.08ms min=18.73ms med=41.14ms max=151.45ms p(90)=53.35ms p(95)=60.33ms
       { expected_response:true }...: avg=43.08ms min=18.73ms med=41.14ms max=151.45ms p(90)=53.35ms p(95)=60.33ms
     http_req_failed................: 0.00%  ✓ 0           ✗ 69261
     http_req_receiving.............: avg=85.5µs  min=0s      med=0s      max=17.12ms  p(90)=0s      p(95)=541.2µs
     http_req_sending...............: avg=43.4µs  min=0s      med=0s      max=16.8ms   p(90)=0s      p(95)=0s
     http_req_tls_handshaking.......: avg=0s      min=0s      med=0s      max=0s       p(90)=0s      p(95)=0s
     http_req_waiting...............: avg=42.95ms min=18.21ms med=41.03ms max=151.45ms p(90)=53.17ms p(95)=60.21ms
     http_reqs......................: 69261  2305.717693/s
     iteration_duration.............: avg=43.31ms min=19.18ms med=41.36ms max=153.89ms p(90)=53.58ms p(95)=60.59ms
     iterations.....................: 69261  2305.717693/s
     vus............................: 100    min=100       max=100
     vus_max........................: 100    min=100       max=100

                                      
running (0m30.0s), 000/100 VUs, 69261 complete and 0 interrupted iterations 
```

### fetch-reference-basic.js (append-fsync-always.delay)

There is a 10ms wait after each request, thus the actual duration is 15.97ms - 10ms.

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


     data_received..................: 4.7 MB 155 kB/s
     data_sent......................: 3.9 MB 129 kB/s
     http_req_blocked...............: avg=25.18µs min=0s      med=0s      max=14.75ms p(90)=0s      p(95)=0s
     http_req_connecting............: avg=8.41µs  min=0s      med=0s      max=3.51ms  p(90)=0s      p(95)=0s
     http_req_duration..............: avg=1.46ms  min=0s      med=1.39ms  max=42.16ms p(90)=2.06ms  p(95)=2.54ms
       { expected_response:true }...: avg=1.46ms  min=0s      med=1.39ms  max=42.16ms p(90)=2.06ms  p(95)=2.54ms
     http_req_failed................: 0.00%  ✓ 0          ✗ 22494
     http_req_receiving.............: avg=107.4µs min=0s      med=0s      max=26.63ms p(90)=506.2µs p(95)=843.77µs
     http_req_sending...............: avg=32.97µs min=0s      med=0s      max=19.22ms p(90)=0s      p(95)=0s
     http_req_tls_handshaking.......: avg=0s      min=0s      med=0s      max=0s      p(90)=0s      p(95)=0s
     http_req_waiting...............: avg=1.32ms  min=0s      med=1.24ms  max=42.16ms p(90)=1.98ms  p(95)=2.39ms
     http_reqs......................: 22494  749.416666/s
     iteration_duration.............: avg=15.97ms min=10.11ms med=15.51ms max=91.32ms p(90)=16.68ms p(95)=18.16ms
     iterations.....................: 22494  749.416666/s
     vus............................: 12     min=12       max=12
     vus_max........................: 12     min=12       max=12

                                      
running (0m30.0s), 00/12 VUs, 22494 complete and 0 interrupted iterations 
```

### fetch-reference-bulk.js (append-fsync-always.delay)

There is a 250ms wait after each request, thus the actual duration is 263.86ms - 250ms.

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


     data_received..................: 2.5 MB 83 kB/s
     data_sent......................: 201 kB 6.7 kB/s
     http_req_blocked...............: avg=110.57µs min=0s       med=0s       max=11.57ms  p(90)=0s       p(95)=0s
     http_req_connecting............: avg=9.53µs   min=0s       med=0s       max=1.15ms   p(90)=0s       p(95)=0s
     http_req_duration..............: avg=1.5ms    min=0s       med=1.47ms   max=9.26ms   p(90)=2.19ms   p(95)=2.74ms
       { expected_response:true }...: avg=1.5ms    min=0s       med=1.47ms   max=9.26ms   p(90)=2.19ms   p(95)=2.74ms
     http_req_failed................: 0.00%  ✓ 0         ✗ 1140
     http_req_receiving.............: avg=118.29µs min=0s       med=0s       max=1.3ms    p(90)=518.1µs  p(95)=998.2µs
     http_req_sending...............: avg=26.71µs  min=0s       med=0s       max=2.18ms   p(90)=0s       p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s       med=0s       max=0s       p(90)=0s       p(95)=0s
     http_req_waiting...............: avg=1.36ms   min=0s       med=1.33ms   max=8.53ms   p(90)=2.09ms   p(95)=2.63ms
     http_reqs......................: 1140   37.892564/s
     iteration_duration.............: avg=263.86ms min=251.53ms med=264.28ms max=272.58ms p(90)=266.39ms p(95)=266.85ms
     iterations.....................: 1140   37.892564/s
     vus............................: 10     min=10      max=10
     vus_max........................: 10     min=10      max=10

                                                 
running (0m30.1s), 00/10 VUs, 1140 complete and 0 interrupted iterations  
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


     data_received..................: 33 MB  1.1 MB/s
     data_sent......................: 27 MB  905 kB/s
     http_req_blocked...............: avg=20.43µs  min=0s med=0s     max=51.7ms   p(90)=0s      p(95)=0s
     http_req_connecting............: avg=11.15µs  min=0s med=0s     max=33.61ms  p(90)=0s      p(95)=0s
     http_req_duration..............: avg=1.64ms   min=0s med=1.01ms max=644.44ms p(90)=2.8ms   p(95)=3.95ms
       { expected_response:true }...: avg=1.64ms   min=0s med=1.01ms max=644.44ms p(90)=2.8ms   p(95)=3.95ms
     http_req_failed................: 0.00%  ✓ 0           ✗ 158281
     http_req_receiving.............: avg=257.78µs min=0s med=0s     max=643.6ms  p(90)=999.6µs p(95)=1.01ms
     http_req_sending...............: avg=36.04µs  min=0s med=0s     max=70.53ms  p(90)=0s      p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s med=0s     max=0s       p(90)=0s      p(95)=0s
     http_req_waiting...............: avg=1.35ms   min=0s med=1ms    max=310.67ms p(90)=2.22ms  p(95)=3.01ms
     http_reqs......................: 158281 5262.614141/s
     iteration_duration.............: avg=1.86ms   min=0s med=1.18ms max=652.9ms  p(90)=3.01ms  p(95)=4.35ms
     iterations.....................: 158281 5262.614141/s
     vus............................: 10     min=10        max=10
     vus_max........................: 10     min=10        max=10

                                                            
running (0m30.1s), 00/10 VUs, 158281 complete and 0 interrupted iterations
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


     data_received..................: 31 MB  1.0 MB/s
     data_sent......................: 26 MB  863 kB/s
     http_req_blocked...............: avg=204.46µs min=0s med=0s     max=322.54ms p(90)=0s      p(95)=0s
     http_req_connecting............: avg=54.1µs   min=0s med=0s     max=167.86ms p(90)=0s      p(95)=0s
     http_req_duration..............: avg=17.89ms  min=0s med=9.01ms max=615.38ms p(90)=36.82ms p(95)=61.29ms
       { expected_response:true }...: avg=17.89ms  min=0s med=9.01ms max=615.38ms p(90)=36.82ms p(95)=61.29ms
     http_req_failed................: 0.00%  ✓ 0           ✗ 150502
     http_req_receiving.............: avg=2.91ms   min=0s med=0s     max=587.8ms  p(90)=998.4µs p(95)=1.01ms
     http_req_sending...............: avg=415.41µs min=0s med=0s     max=330.54ms p(90)=0s      p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s med=0s     max=0s       p(90)=0s      p(95)=0s
     http_req_waiting...............: avg=14.56ms  min=0s med=8.99ms max=371.78ms p(90)=32.88ms p(95)=49.83ms
     http_reqs......................: 150502 5015.407377/s
     iteration_duration.............: avg=19.7ms   min=0s med=9.52ms max=615.38ms p(90)=42.54ms p(95)=71.64ms
     iterations.....................: 150502 5015.407377/s
     vus............................: 100    min=100       max=100
     vus_max........................: 100    min=100       max=100

                                       
running (0m30.0s), 000/100 VUs, 150502 complete and 0 interrupted iterations
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


     data_received..................: 31 MB  1.0 MB/s
     data_sent......................: 26 MB  862 kB/s
     http_req_blocked...............: avg=20.02µs  min=0s med=0s     max=70.23ms  p(90)=0s      p(95)=0s
     http_req_connecting............: avg=10.98µs  min=0s med=0s     max=70.23ms  p(90)=0s      p(95)=0s
     http_req_duration..............: avg=1.72ms   min=0s med=1.01ms max=348.95ms p(90)=2.99ms  p(95)=4.01ms
       { expected_response:true }...: avg=1.7ms    min=0s med=1.01ms max=159.97ms p(90)=2.99ms  p(95)=4.01ms
     http_req_failed................: 0.00%  ✓ 10          ✗ 150336
     http_req_receiving.............: avg=272.04µs min=0s med=0s     max=107.8ms  p(90)=999.5µs p(95)=1.01ms
     http_req_sending...............: avg=40.5µs   min=0s med=0s     max=35.76ms  p(90)=0s      p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s med=0s     max=0s       p(90)=0s      p(95)=0s
     http_req_waiting...............: avg=1.41ms   min=0s med=1ms    max=347.95ms p(90)=2.27ms  p(95)=3.17ms
     http_reqs......................: 150346 5011.517012/s
     iteration_duration.............: avg=1.95ms   min=0s med=1.16ms max=350.95ms p(90)=3.08ms  p(95)=4.74ms
     iterations.....................: 150346 5011.517012/s
     vus............................: 10     min=10        max=10
     vus_max........................: 10     min=10        max=10

                                                      
running (0m30.0s), 00/10 VUs, 150346 complete and 0 interrupted iterations
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


     data_received..................: 37 MB  1.2 MB/s
     data_sent......................: 31 MB  1.0 MB/s
     http_req_blocked...............: avg=174.49µs min=0s med=0s     max=420.75ms p(90)=0s      p(95)=0s
     http_req_connecting............: avg=57.29µs  min=0s med=0s     max=420.75ms p(90)=0s      p(95)=0s
     http_req_duration..............: avg=15.1ms   min=0s med=8.58ms max=527.83ms p(90)=29.3ms  p(95)=47ms
       { expected_response:true }...: avg=14.81ms  min=0s med=8.13ms max=527.83ms p(90)=29.34ms p(95)=47.5ms
     http_req_failed................: 9.34%  ✓ 16746      ✗ 162450
     http_req_receiving.............: avg=2.33ms   min=0s med=0s     max=366.99ms p(90)=546.4µs p(95)=1ms
     http_req_sending...............: avg=326.76µs min=0s med=0s     max=350.99ms p(90)=0s      p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s med=0s     max=0s       p(90)=0s      p(95)=0s
     http_req_waiting...............: avg=12.44ms  min=0s med=8.24ms max=236.15ms p(90)=26.87ms p(95)=39.81ms
     http_reqs......................: 179196 5970.90757/s
     iteration_duration.............: avg=16.57ms  min=0s med=9ms    max=527.83ms p(90)=32.93ms p(95)=53.63ms
     iterations.....................: 179196 5970.90757/s
     vus............................: 100    min=100      max=100
     vus_max........................: 100    min=100      max=100

                                                     
running (0m30.0s), 000/100 VUs, 179196 complete and 0 interrupted iterations   
```