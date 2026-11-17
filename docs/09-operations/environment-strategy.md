# Environment Strategy

- Local: Docker Compose running PostgreSQL, MinIO, and Spring Boot backend with hot-reload Vite web frontend.
- Staging: Cloud VM / Container environment mirroring production with anonymized test datasets.
- Production: Hardened Linux VM / Managed Container cluster with automated PostgreSQL backups and Cloudflare CDN.
