# Input Validation Standards

- Backend: Jakarta Bean Validation (Hibernate Validator) annotations on all DTOs (@NotNull, @Size, @Pattern, @DecimalMin).
- Frontend: Zod schemas mirroring backend validation constraints for instantaneous client feedback.
- Format Checks: PAN format regex, IFSC regex, Aadhaar 12-digit Luhn algorithm verification, valid Indian mobile number format.
