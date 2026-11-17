# Backup & Disaster Recovery

- Database: Automated daily pg_dump backups with 30-day retention and point-in-time recovery (PITR).
- S3 Bucket: Multi-region object versioning to protect against accidental document deletion.
- Recovery Time Objective (RTO): < 2 hours; Recovery Point Objective (RPO): < 24 hours.
