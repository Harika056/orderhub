#!/usr/bin/env python3
"""Fire order requests at the gateway and report latency percentiles.

Usage:
    python3 scripts/load_test.py --requests 2000 --concurrency 50
    python3 scripts/load_test.py --url http://localhost:8080/orders --requests 500 --concurrency 20

Only standard-library modules are used, so there is nothing to install.
The latency measured is the time to get the 202 Accepted response, i.e. what a customer waits
for before inventory and payment finish in the background.
"""
import argparse
import json
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor


def place_order(url, i):
    body = json.dumps({"sku": f"SKU-{i % 5 + 1}", "quantity": 1, "amount": 25.0}).encode()
    request = urllib.request.Request(
        url, data=body, method="POST", headers={"Content-Type": "application/json"})
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            ok = response.status in (200, 202)
    except (urllib.error.URLError, TimeoutError, OSError):
        ok = False
    return (time.perf_counter() - started) * 1000, ok


def percentile(sorted_values, p):
    index = min(len(sorted_values) - 1, int(round(p / 100 * (len(sorted_values) - 1))))
    return sorted_values[index]


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--url", default="http://localhost:8080/orders")
    parser.add_argument("--requests", type=int, default=1000)
    parser.add_argument("--concurrency", type=int, default=20)
    args = parser.parse_args()

    # Warm-up so JIT compilation and connection setup do not skew the numbers.
    for i in range(20):
        place_order(args.url, i)

    started = time.perf_counter()
    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        results = list(pool.map(lambda i: place_order(args.url, i), range(args.requests)))
    elapsed = time.perf_counter() - started

    latencies = sorted(ms for ms, ok in results if ok)
    errors = sum(1 for _, ok in results if not ok)
    if not latencies:
        print("Every request failed. Is the stack running? Try scripts/smoke_test.sh first.")
        return

    print(f"requests:    {args.requests} ({args.concurrency} concurrent)")
    print(f"errors:      {errors}")
    print(f"throughput:  {args.requests / elapsed:.0f} requests/sec")
    print(f"avg:         {sum(latencies) / len(latencies):.1f} ms")
    print(f"p50:         {percentile(latencies, 50):.1f} ms")
    print(f"p95:         {percentile(latencies, 95):.1f} ms")
    print(f"p99:         {percentile(latencies, 99):.1f} ms")


if __name__ == "__main__":
    main()
