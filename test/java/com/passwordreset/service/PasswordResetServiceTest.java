package test.java.com.passwordreset.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetServiceTest {
    private PasswordResetService service;

    @BeforeEach
    void setUp() { service = new PasswordResetService(); }

    @Test
    void testGenerateOTP_invalidEmail_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.generateOTP(""));
        assertThrows(IllegalArgumentException.class, () -> service.generateOTP(null));
        assertThrows(IllegalArgumentException.class, () -> service.generateOTP("noatsign"));
    }

    @Test
    void testResetPassword_noOTPGenerated_throwsException() {
        assertThrows(SecurityException.class, () ->
                service.resetPassword("user@test.com", "123456", "SecurePass1!"));
    }

    @Test
    void testResetPassword_wrongOTP_throwsException() {
        String email = "user@test.com";
        service.generateOTP(email);
        assertThrows(SecurityException.class, () ->
                service.resetPassword(email, "999999", "SecurePass1!"));
    }

    @Test
    void testResetPassword_expiredOTP_throwsException() {
        String email = "user@test.com";
        // Use a clock set 6 minutes in the future to simulate expiry
        Clock expiredClock = Clock.fixed(Instant.now().plusSeconds(360), ZoneId.systemDefault());
        PasswordResetService expiredService = new PasswordResetService(expiredClock);
        expiredService.generateOTP(email);

        assertThrows(SecurityException.class, () ->
                expiredService.resetPassword(email, expiredService.generateOTP(email).split(" ")[0], "SecurePass1!"));
    }

    @Test
    void testResetPassword_weakPassword_throwsException() {
        String email = "user@test.com";
        service.generateOTP(email);
        assertThrows(IllegalArgumentException.class, () ->
                service.resetPassword(email, service.generateOTP(email), "short"));
        assertThrows(IllegalArgumentException.class, () ->
                service.resetPassword(email, service.generateOTP(email), "noSpecial123"));
    }
}