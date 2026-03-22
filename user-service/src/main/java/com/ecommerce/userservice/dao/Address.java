package com.ecommerce.userservice.dao;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String id;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private boolean defaultAddress;

    public enum Type {SHIPPING, BILLING}

    private Type type;
}
