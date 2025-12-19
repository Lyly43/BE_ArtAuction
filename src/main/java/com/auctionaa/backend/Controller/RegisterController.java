package com.auctionaa.backend.Controller;

import com.auctionaa.backend.DTO.Request.RegisterRequest;
import com.auctionaa.backend.DTO.Request.ResendOtpRequest;
import com.auctionaa.backend.DTO.Request.VerifyOtpRequest;
import com.auctionaa.backend.DTO.Response.AuthResponse;
import com.auctionaa.backend.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class RegisterController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(userService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponse> resendOtp(@RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(userService.resendOtp(request.getEmail()));
    }
}
