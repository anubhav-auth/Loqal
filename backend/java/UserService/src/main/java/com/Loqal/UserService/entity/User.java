package com.Loqal.UserService.entity;

import lombok.Data;
import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
@Document("users")
@Data
public class User {
    @Id
    private String id;
    private String username;
    private String email;
    private String password;
    private List<String> roles; // CUSTOMER, DELIVERY_AGENT, DISPATCHER, MERCHANT, ADMIN
    private List<RabbitConnectionDetails.Address> savedAddresses;
}
