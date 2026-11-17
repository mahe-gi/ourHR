# API Standards Specification

## 1. REST Conventions
- Base Path: /api/v1
- Content-Type: application/json
- Character Set: UTF-8

## 2. Standard Response Envelope
### Success Response
```json
{
  "success": true,
  "data": {},
  "meta": {
    "timestamp": "2026-11-17T12:00:00Z",
    "pagination": null
  }
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "The 50% Basic wage rule constraint was violated.",
    "details": [
      {
        "field": "basicPay",
        "issue": "Basic pay must be at least 50% of gross remuneration."
      }
    ]
  }
}
```
