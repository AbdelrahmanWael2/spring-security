package com.example.security.OTP;

import org.springframework.stereotype.Service;


public interface EmailService {
    void sendOtp(String to, String otp);
}

