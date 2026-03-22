package com.ecommerce.userservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}
