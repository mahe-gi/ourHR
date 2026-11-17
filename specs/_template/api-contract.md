# API Contract: [ENDPOINT_NAME]

Endpoint: [METHOD] /api/v1/[PATH]
Module: [MODULE]
Roles Allowed: [ROLES]

## Request
### Headers
- Authorization: Bearer <jwt>
- Content-Type: application/json

### Body Schema
```json
{
  "field": "value"
}
```

## Response
### 200 OK
```json
{
  "success": true,
  "data": {},
  "meta": null
}
```

### 400 Bad Request
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Error details",
    "details": []
  }
}
```
