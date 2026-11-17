# Integration Architecture

## 1. S3-Compatible Storage Integration
- Adapter Interface: FileStorageService with methods uploadFile(key, byteStream, contentType) and generatePresignedUrl(key).
- Implementations: AWS S3, Cloudflare R2, MinIO (local dev container).

## 2. Bank Bulk Payout Integration
- Export standard comma-separated and fixed-width file formats:
  - HDFC Bank Bulk Payout CSV
  - ICICI Bank Corporate Payout Format
  - SBI CMP File Format
  - Axis Bank Bulk Upload Format

## 3. EPFO ECR Text File Integration
- Formatted in official 25-column hash-delimited (#~#) text structure required by the Unified EPFO Shram Suvidha portal.

## 4. Biometric Device Webhook / Ingestion API
- REST endpoint accepting authenticated biometric hardware payloads containing employee_code, punch_timestamp, and in_out_direction.
