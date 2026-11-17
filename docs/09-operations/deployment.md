# Deployment Strategy

## Dual Deployment Topology
1. Decoupled Cloud Deployment (Default):
   - Frontend static assets hosted on Cloudflare Pages / S3 / CDN.
   - Spring Boot API running in a standalone Docker container.
2. All-in-One Portable Deployment (Traditional MSMEs / Local):
   - React web app compiled into Spring Boot src/main/resources/static/.
   - Packaged into a single self-executable JAR running on standard port 8080.
