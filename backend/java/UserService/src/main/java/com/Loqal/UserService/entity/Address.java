package com.Loqal.UserService.entity;

import lombok.Data;

@Data
public class Address {
    private String label; // e.g., Home, Office
    private String street;
    private String city;
    private String state;
    private String postalCode;
}
