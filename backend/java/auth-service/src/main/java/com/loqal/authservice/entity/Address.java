package com.loqal.authservice.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    public static Address defaultAddress() {
        return Address.builder()
                .street("N/A")
                .city("N/A")
                .state("N/A")
                .postalCode("000000")
                .country("N/A")
                .build();
    }
}
