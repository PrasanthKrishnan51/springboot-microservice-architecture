package com.ecommerce.userservice.dto;

import com.ecommerce.userservice.dao.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private User.Role role;
    private boolean enabled;
    private List<Address> addresses;
    private LocalDateTime createdAt;
}
