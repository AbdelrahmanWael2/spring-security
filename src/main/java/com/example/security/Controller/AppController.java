package com.example.security.Controller;

import com.example.security.DTO.*;
import com.example.security.OTP.EmailService;
import com.example.security.OTP.Util;
import com.example.security.entity.Role;
import com.example.security.entity.UserEntity;
import com.example.security.repository.RoleRepository;
import com.example.security.repository.UserEntityRepository;
import com.example.security.security.JWTGenerator;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
public class AppController {

    @Autowired
    public AppController(AuthenticationManager authenticationManager, UserEntityRepository userEntityRepository,
                         RoleRepository roleRepository, PasswordEncoder passwordEncoder, JWTGenerator jwtGenerator, EmailService emailService){
        this.userEntityRepository = userEntityRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtGenerator = jwtGenerator;
        this.emailService = emailService;
    }

    private final UserEntityRepository userEntityRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JWTGenerator jwtGenerator;

    private final EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterDTO registerDTO) {
        if (userEntityRepository.existsByUsername(registerDTO.getUsername())) {
            return new ResponseEntity<>("Username is already taken!", HttpStatus.BAD_REQUEST);
        }

        UserEntity user = new UserEntity();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setOtp(Util.generateOtp());
        user.setVerified(false);

        Role role = roleRepository.findByName("ROLE_USER").orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);

        userEntityRepository.save(user);

        emailService.sendOtp(user.getEmail(), user.getOtp());

        return new ResponseEntity<>("User registration initiated. Please check your email for OTP.", HttpStatus.OK);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody otpDTO otpDTO) {
        UserEntity user = userEntityRepository.findByUsername(otpDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOtp().equals(otpDTO.getOtp())) {
            user.setVerified(true);
            user.setOtp(null);
            userEntityRepository.save(user);
            return new ResponseEntity<>("Email verified successfully", HttpStatus.OK);
        }

        return new ResponseEntity<>("Invalid OTP", HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> loginUser(@RequestBody LoginDTO loginDTO){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.getUsername(),
                        loginDTO.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtGenerator.generateToken(authentication);
        return new ResponseEntity<>(new AuthResponseDTO(token), HttpStatus.OK);
    }

    @PostMapping("/forget-password")
    public ResponseEntity<String> forgetPassword(@RequestBody ForgetPasswordDTO forgetPasswordDTO){
        UserEntity user = userEntityRepository.findByUsername(forgetPasswordDTO.getUsername()).orElseThrow();
        String verCode = Util.generateOtp();
        user.setVerCode(verCode);
        userEntityRepository.save(user);
        emailService.sendOtp(user.getEmail(), verCode);

        return new ResponseEntity<>("Verification code sent to your email", HttpStatus.OK);

    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO){
        UserEntity user = userEntityRepository.findByUsername(resetPasswordDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println(user.getVerCode());
        System.out.println(resetPasswordDTO.getVerCode());
        if(resetPasswordDTO.getVerCode().equals(user.getVerCode())){
            user.setPassword(resetPasswordDTO.getNewPassword());
            return new ResponseEntity<>("New password set", HttpStatus.OK);
        }else return new ResponseEntity<>("Incorrect otp", HttpStatus.BAD_REQUEST);
    }


}
