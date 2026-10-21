# Rough Sizing

Order-of-magnitude only. Show each calculation in one line. Tag results `[derived]`.
Round generously; the goal is to know whether the load is "small", "moderate", or
"large", not an exact figure.

## Request rate

```
avg requests/sec   = (active users per day x requests per user per day) / 86,400
peak requests/sec  = avg requests/sec x peak factor
```

- Peak factor: use the user's number; otherwise `3` steady, `10` daily peaks,
  `30+` spiky events `[assumed]`.
- Split peak into reads and writes using the read:write ratio.

## Data volume

```
new data per day  = writes per day x average record size
data after N yrs  = new data per day x 365 x N x retention factor
```

- Include a growth multiplier if the user expects scale-up in 12-24 months.
- Large binary content (images, video, documents) is sized separately from structured records.

## Bandwidth (only if content is large)

```
peak bandwidth = peak requests/sec x average response size
```

## Classify the load (drives how many components are justified)

| Peak requests/sec | Data after 2 yrs | Label |
|---|---|---|
| < 100 | < 100 GB | Small: favor the simplest structure |
| 100 - 5,000 | 100 GB - 10 TB | Moderate: caching, async work, replicas may be justified |
| > 5,000 | > 10 TB | Large: partitioning and independent scaling likely justified |

These thresholds are heuristics. State the label and the reasoning; do not treat them as rules.

## Always state

- Which inputs each figure came from.
- The single number the design is most sensitive to (usually peak rate or write volume).
