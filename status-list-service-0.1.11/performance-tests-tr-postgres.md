# Performance tests with the actual hardware from TR

The directory src/test/k6 contains some k6 performance tests.
for additional information look at [performance-tests.md](performance-tests.md)

This document shows the results of the performance tests using Posgres DB with the default settings at the current hardware and software.

## Result summary
This performance test is only an example at a specific hardware/software.
The results can be quite different on the bop. 

For all read requests there is no recognizable difference between the use of Redis and Postgres DB. 
A performance difference is clearly recognizable when updating lists.
With Redis, the maximum number of executed requests is around 10 times higher than when using Postgres DB.
The Postgres DB has its maximum of 8,000-9,000 requests in 30 seconds with four parallel VUs and 
Redis has its maximum of 80,000-90,000 requests in 30 seconds with circa 200 parallel VUs.

The results are as expected and show that the Postgres DB is slower than Redis, but still performs the updates fast enough.

## Test hardware / software
- LeanClient with Windows 11 and active Microsoft Defender
- Intel(R) Core(TM) i7-1185G7 @ 3.00GHz   3.00 GHz
- 512GB SSD 
- 32GB RAM
- Postgres DB is running on the same machine as the service

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


     data_received..................: 397 MB 13 MB/s
     data_sent......................: 15 MB  500 kB/s
     http_req_blocked...............: avg=93.45µs  min=0s med=0s      max=312.07ms p(90)=0s      p(95)=0s
     http_req_connecting............: avg=19.64µs  min=0s med=0s      max=235.51ms p(90)=0s      p(95)=0s
     http_req_duration..............: avg=22.37ms  min=0s med=13.7ms  max=417ms    p(90)=51.45ms p(95)=68.48ms
       { expected_response:true }...: avg=22.37ms  min=0s med=13.7ms  max=417ms    p(90)=51.45ms p(95)=68.48ms
     http_req_failed................: 0.00%  ✓ 0           ✗ 129386
     http_req_receiving.............: avg=1.58ms   min=0s med=0s      max=326.41ms p(90)=532.9µs p(95)=1.01ms
     http_req_sending...............: avg=181.36µs min=0s med=0s      max=375.69ms p(90)=0s      p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s med=0s      max=0s       p(90)=0s      p(95)=0s
     http_req_waiting...............: avg=20.61ms  min=0s med=13.28ms max=220.59ms p(90)=49.01ms p(95)=63.66ms
     http_reqs......................: 129386 4309.483507/s
     iteration_duration.............: avg=23.1ms   min=0s med=14.14ms max=444.18ms p(90)=52.57ms p(95)=70.58ms
     iterations.....................: 129386 4309.483507/s
     vus............................: 100    min=100       max=100
     vus_max........................: 100    min=100       max=100

                               
running (0m30.0s), 000/100 VUs, 129386 complete and 0 interrupted iterations 
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
        script: .\update-status.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 10 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 523 kB 17 kB/s
     data_sent......................: 2.8 MB 93 kB/s
     http_req_blocked...............: avg=28.37µs min=0s     med=0s      max=18.8ms   p(90)=0s       p(95)=0s
     http_req_connecting............: avg=5.21µs  min=0s     med=0s      max=2.41ms   p(90)=0s       p(95)=0s
     http_req_duration..............: avg=30.87ms min=3.15ms med=16.87ms max=419.3ms  p(90)=73.55ms  p(95)=101.67ms
       { expected_response:true }...: avg=30.87ms min=3.15ms med=16.87ms max=419.3ms  p(90)=73.55ms  p(95)=101.67ms
     http_req_failed................: 0.00%  ✓ 0          ✗ 9647
     http_req_receiving.............: avg=90.18µs min=0s     med=0s      max=5.62ms   p(90)=509.29µs p(95)=537.5µs
     http_req_sending...............: avg=34.36µs min=0s     med=0s      max=3.07ms   p(90)=0s       p(95)=507.4µs
     http_req_tls_handshaking.......: avg=0s      min=0s     med=0s      max=0s       p(90)=0s       p(95)=0s
     http_req_waiting...............: avg=30.74ms min=3.15ms med=16.74ms max=419.3ms  p(90)=73.36ms  p(95)=101.42ms
     http_reqs......................: 9647   321.197539/s
     iteration_duration.............: avg=31.08ms min=3.18ms med=17.06ms max=420.39ms p(90)=73.76ms  p(95)=101.88ms
     iterations.....................: 9647   321.197539/s
     vus............................: 10     min=10       max=10
     vus_max........................: 10     min=10       max=10

                                                                                                    
running (0m30.0s), 00/10 VUs, 9647 complete and 0 interrupted iterations
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


     data_received..................: 456 kB 15 kB/s
     data_sent......................: 2.4 MB 81 kB/s
     http_req_blocked...............: avg=140.1µs  min=0s      med=0s       max=18.02ms  p(90)=0s       p(95)=0s
     http_req_connecting............: avg=16.77µs  min=0s      med=0s       max=8ms      p(90)=0s       p(95)=0s
     http_req_duration..............: avg=357.28ms min=10.48ms med=349.92ms max=808.11ms p(90)=411.27ms p(95)=444.89ms
       { expected_response:true }...: avg=357.28ms min=10.48ms med=349.92ms max=808.11ms p(90)=411.27ms p(95)=444.89ms
     http_req_failed................: 0.00%  ✓ 0          ✗ 8437
     http_req_receiving.............: avg=56.31µs  min=0s      med=0s       max=1.77ms   p(90)=239.12µs p(95)=525.69µs
     http_req_sending...............: avg=37.83µs  min=0s      med=0s       max=7.99ms   p(90)=0s       p(95)=505.6µs
     http_req_tls_handshaking.......: avg=0s       min=0s      med=0s       max=0s       p(90)=0s       p(95)=0s
     http_req_waiting...............: avg=357.19ms min=10.48ms med=349.87ms max=808.11ms p(90)=411.06ms p(95)=444.6ms
     http_reqs......................: 8437   278.047461/s
     iteration_duration.............: avg=357.55ms min=10.48ms med=350.06ms max=808.11ms p(90)=411.31ms p(95)=444.96ms
     iterations.....................: 8437   278.047461/s
     vus............................: 100    min=100      max=100
     vus_max........................: 100    min=100      max=100

                                
running (0m30.3s), 000/100 VUs, 8437 complete and 0 interrupted iterations 
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
        script: .\fetch-reference-basic.js
        output: -

     scenarios: (100.00%) 1 scenario, 12 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 12 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 4.7 MB 158 kB/s
     data_sent......................: 3.9 MB 131 kB/s
     http_req_blocked...............: avg=19.19µs  min=0s      med=0s     max=12.36ms p(90)=0s       p(95)=0s
     http_req_connecting............: avg=6.89µs   min=0s      med=0s     max=5.74ms  p(90)=0s       p(95)=0s
     http_req_duration..............: avg=1.38ms   min=0s      med=1.37ms max=18.1ms  p(90)=2.09ms   p(95)=2.6ms
       { expected_response:true }...: avg=1.38ms   min=0s      med=1.37ms max=18.1ms  p(90)=2.09ms   p(95)=2.6ms
     http_req_failed................: 0.00%  ✓ 0        ✗ 22889
     http_req_receiving.............: avg=111.48µs min=0s      med=0s     max=16.59ms p(90)=506.52µs p(95)=947.5µs
     http_req_sending...............: avg=20.58µs  min=0s      med=0s     max=5.02ms  p(90)=0s       p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s      med=0s     max=0s      p(90)=0s       p(95)=0s
     http_req_waiting...............: avg=1.25ms   min=0s      med=1.23ms max=14.09ms p(90)=1.96ms   p(95)=2.48ms
     http_reqs......................: 22889  762.4742/s
     iteration_duration.............: avg=15.7ms   min=10.14ms med=15.5ms max=45.74ms p(90)=16.12ms  p(95)=16.53ms
     iterations.....................: 22889  762.4742/s
     vus............................: 12     min=12     max=12
     vus_max........................: 12     min=12     max=12

                        
running (0m30.0s), 00/12 VUs, 22889 complete and 0 interrupted iterations
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
        script: .\fetch-reference-bulk.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 10 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 2.5 MB 83 kB/s
     data_sent......................: 201 kB 6.7 kB/s
     http_req_blocked...............: avg=123.51µs min=0s       med=0s       max=12.31ms  p(90)=0s       p(95)=0s
     http_req_connecting............: avg=14.6µs   min=0s       med=0s       max=4.19ms   p(90)=0s       p(95)=0s
     http_req_duration..............: avg=1.73ms   min=0s       med=1.53ms   max=9.38ms   p(90)=2.53ms   p(95)=2.94ms
       { expected_response:true }...: avg=1.73ms   min=0s       med=1.53ms   max=9.38ms   p(90)=2.53ms   p(95)=2.94ms
     http_req_failed................: 0.00%  ✓ 0         ✗ 1140
     http_req_receiving.............: avg=113.75µs min=0s       med=0s       max=3ms      p(90)=507.9µs  p(95)=963.61µs
     http_req_sending...............: avg=17.81µs  min=0s       med=0s       max=3.53ms   p(90)=0s       p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s       med=0s       max=0s       p(90)=0s       p(95)=0s
     http_req_waiting...............: avg=1.6ms    min=0s       med=1.51ms   max=7.86ms   p(90)=2.34ms   p(95)=2.81ms
     http_reqs......................: 1140   37.828217/s
     iteration_duration.............: avg=264.33ms min=251.07ms med=264.66ms max=279.91ms p(90)=266.62ms p(95)=267.41ms
     iterations.....................: 1140   37.828217/s
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
        script: .\fetch-reference-unlimited.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 10 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 25 MB  842 kB/s
     data_sent......................: 21 MB  700 kB/s
     http_req_blocked...............: avg=17.85µs  min=0s med=0s     max=22.37ms p(90)=0s      p(95)=0s
     http_req_connecting............: avg=8.61µs   min=0s med=0s     max=19.91ms p(90)=0s      p(95)=0s
     http_req_duration..............: avg=1.89ms   min=0s med=1.03ms max=4.31s   p(90)=3.01ms  p(95)=4.82ms
       { expected_response:true }...: avg=1.89ms   min=0s med=1.03ms max=4.31s   p(90)=3.01ms  p(95)=4.82ms
     http_req_failed................: 0.00%  ✓ 0           ✗ 122117
     http_req_receiving.............: avg=322.92µs min=0s med=0s     max=98.83ms p(90)=999.8µs p(95)=1.01ms
     http_req_sending...............: avg=35.98µs  min=0s med=0s     max=63.32ms p(90)=0s      p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s med=0s     max=0s      p(90)=0s      p(95)=0s
     http_req_waiting...............: avg=1.54ms   min=0s med=1ms    max=4.31s   p(90)=2.52ms  p(95)=3.67ms
     http_reqs......................: 122117 4070.081052/s
     iteration_duration.............: avg=2.38ms   min=0s med=1.22ms max=4.33s   p(90)=3.5ms   p(95)=5.51ms
     iterations.....................: 122117 4070.081052/s
     vus............................: 10     min=10        max=10
     vus_max........................: 10     min=10        max=10

                           
running (0m30.0s), 00/10 VUs, 122117 complete and 0 interrupted iterations
```

#### 100 VUs

```
          /\      |‾‾| /‾‾/   /‾‾/
     /\  /  \     |  |/  /   /  /
    /  \/    \    |     (   /   ‾‾\
   /          \   |  |\  \ |  (‾)  |
  / __________ \  |__| \__\ \_____/ .io

     execution: local
        script: .\fetch-reference-unlimited.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 100 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 31 MB  1.0 MB/s
     data_sent......................: 26 MB  848 kB/s
     http_req_blocked...............: avg=203.41µs min=0s med=0s     max=462.59ms p(90)=0s      p(95)=0s
     http_req_connecting............: avg=52.34µs  min=0s med=0s     max=252.67ms p(90)=0s      p(95)=0s
     http_req_duration..............: avg=18.2ms   min=0s med=9.06ms max=559.28ms p(90)=39.84ms p(95)=62.6ms
       { expected_response:true }...: avg=18.2ms   min=0s med=9.06ms max=559.28ms p(90)=39.84ms p(95)=62.6ms
     http_req_failed................: 0.00%  ✓ 0           ✗ 148004
     http_req_receiving.............: avg=3.08ms   min=0s med=0s     max=473.37ms p(90)=999.5µs p(95)=1.31ms
     http_req_sending...............: avg=406.36µs min=0s med=0s     max=408.65ms p(90)=0s      p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s med=0s     max=0s       p(90)=0s      p(95)=0s
     http_req_waiting...............: avg=14.7ms   min=0s med=8.99ms max=333.13ms p(90)=35.58ms p(95)=52.79ms
     http_reqs......................: 148004 4929.618639/s
     iteration_duration.............: avg=20.02ms  min=0s med=9.53ms max=559.28ms p(90)=45.27ms p(95)=72.5ms
     iterations.....................: 148004 4929.618639/s
     vus............................: 100    min=100       max=100
     vus_max........................: 100    min=100       max=100

                                                        
running (0m30.0s), 000/100 VUs, 148004 complete and 0 interrupted iterations
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
        script: .\fetch-reference-unlimited.js
        output: -

     scenarios: (100.00%) 1 scenario, 10 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 10 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 31 MB  1.0 MB/s
     data_sent......................: 26 MB  868 kB/s
     http_req_blocked...............: avg=21.23µs  min=0s med=0s     max=36.78ms  p(90)=0s     p(95)=0s
     http_req_connecting............: avg=12.02µs  min=0s med=0s     max=36.62ms  p(90)=0s     p(95)=0s
     http_req_duration..............: avg=1.73ms   min=0s med=1.1ms  max=168.77ms p(90)=2.99ms p(95)=4.04ms
       { expected_response:true }...: avg=1.72ms   min=0s med=1.1ms  max=168.77ms p(90)=2.99ms p(95)=4.03ms
     http_req_failed................: 0.84%  ✓ 1279        ✗ 150124
     http_req_receiving.............: avg=303.08µs min=0s med=0s     max=155.84ms p(90)=1ms    p(95)=1.1ms
     http_req_sending...............: avg=38.09µs  min=0s med=0s     max=62.01ms  p(90)=0s     p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s med=0s     max=0s       p(90)=0s     p(95)=0s
     http_req_waiting...............: avg=1.39ms   min=0s med=1ms    max=163.79ms p(90)=2.31ms p(95)=3.14ms
     http_reqs......................: 151403 5046.301179/s
     iteration_duration.............: avg=1.94ms   min=0s med=1.36ms max=184.79ms p(90)=3.2ms  p(95)=4.7ms
     iterations.....................: 151403 5046.301179/s
     vus............................: 10     min=10        max=10
     vus_max........................: 10     min=10        max=10

                       
running (0m30.0s), 00/10 VUs, 151403 complete and 0 interrupted iterations
```

#### 100 VUs

```
          /\      |‾‾| /‾‾/   /‾‾/
     /\  /  \     |  |/  /   /  /
    /  \/    \    |     (   /   ‾‾\
   /          \   |  |\  \ |  (‾)  |
  / __________ \  |__| \__\ \_____/ .io

     execution: local
        script: .\fetch-reference-unlimited.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 100 looping VUs for 30s (gracefulStop: 30s)


     data_received..................: 29 MB  958 kB/s
     data_sent......................: 24 MB  796 kB/s
     http_req_blocked...............: avg=179.17µs min=0s med=0s      max=326.55ms p(90)=0s       p(95)=0s
     http_req_connecting............: avg=86.94µs  min=0s med=0s      max=208.46ms p(90)=0s       p(95)=0s
     http_req_duration..............: avg=19.58ms  min=0s med=12.08ms max=472.82ms p(90)=36.74ms  p(95)=58.87ms
       { expected_response:true }...: avg=19.58ms  min=0s med=12.08ms max=472.82ms p(90)=36.74ms  p(95)=58.87ms
     http_req_failed................: 0.00%  ✓ 0           ✗ 139018
     http_req_receiving.............: avg=2.73ms   min=0s med=0s      max=443.4ms  p(90)=539.23µs p(95)=1.01ms
     http_req_sending...............: avg=341.48µs min=0s med=0s      max=329.79ms p(90)=0s       p(95)=0s
     http_req_tls_handshaking.......: avg=0s       min=0s med=0s      max=0s       p(90)=0s       p(95)=0s
     http_req_waiting...............: avg=16.5ms   min=0s med=11.94ms max=289.48ms p(90)=33.94ms  p(95)=49.68ms
     http_reqs......................: 139018 4629.331593/s
     iteration_duration.............: avg=21.34ms  min=0s med=12.59ms max=522.42ms p(90)=40.97ms  p(95)=68.36ms
     iterations.....................: 139018 4629.331593/s
     vus............................: 100    min=100       max=100
     vus_max........................: 100    min=100       max=100

                                                                                                    
running (0m30.0s), 000/100 VUs, 139018 complete and 0 interrupted  
```