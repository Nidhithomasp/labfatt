package main.java.com.passwordreset.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PasswordResetService {
    private static final long OTP_EXPIRY_SECONDS = 300; // 5 minutes
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();
    private final Clock clock;

    // Constructor with Clock for testability
    public PasswordResetService(Clock clock) {
        this.clock = clock;
    }

    public PasswordResetService() {
        this(Clock.systemUTC());
    }

    public String generateOTP(String email) {
        validateEmail(email);
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        otpStore.put(email, new OtpRecord(otp, clock.instant()));
        return otp;
    }

    public void resetPassword(String email, String otp, String newPassword) {
        validateInputs(email, otp, newPassword);
        OtpRecord record = otpStore.get(email);
        if (record == null) throw new SecurityException("No OTP generated for this email");
        if (clock.instant().isAfter(record.timestamp.plusSeconds(OTP_EXPIRY_SECONDS))) {
            otpStore.remove(email);
            throw new SecurityException("OTP expired");
        }
        if (!record.otp.equals(otp)) {
            throw new SecurityException("Invalid OTP");
        }
        // Simulate secure password update (hash & store in DB in production)
        otpStore.remove(email);
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
    }

    private void validateInputs(String email, String otp, String newPassword) {
        if (email == null || otp == null || newPassword == null) {
            throw new IllegalArgumentException("Missing required fields");
        }
        if (newPassword.length() < 8 || newPassword.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("Password must be >=8 chars with special characters");
        }
    }

    private record OtpRecord(String otp, Instant timestamp) {}
}