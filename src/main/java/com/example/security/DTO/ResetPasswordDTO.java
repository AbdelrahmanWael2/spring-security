package com.example.security.DTO;

import lombok.Getter;

@Getter
public class ResetPasswordDTO {
    private String username;
    private String verCode;
    private String newPassword;
}
