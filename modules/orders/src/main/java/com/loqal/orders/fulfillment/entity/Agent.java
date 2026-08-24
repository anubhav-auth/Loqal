package com.loqal.orders.fulfillment.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("delivery_agents")
public class Agent implements Persistable<UUID> {

    public static final String OFF_DUTY = "OFF_DUTY";
    public static final String AVAILABLE = "AVAILABLE";
    public static final String ON_DELIVERY = "ON_DELIVERY";

    @Id
    private UUID id;
    private UUID tenantId;
    private UUID userId;
    private String name;
    private String phone;
    private String vehicleType;
    private String status;
    private Double currentLat;
    private Double currentLng;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Transient
    private boolean newRecord = false;

    public void markNew() {
        this.newRecord = true;
    }

    @Override
    public boolean isNew() {
        return newRecord;
    }

    /** Haversine distance in km; null coordinates sort last. */
    public Double distanceKmTo(double lat, double lng) {
        if (currentLat == null || currentLng == null) {
            return Double.MAX_VALUE;
        }
        double dLat = Math.toRadians(lat - currentLat);
        double dLng = Math.toRadians(lng - currentLng);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(currentLat)) * Math.cos(Math.toRadians(lat))
                * Math.pow(Math.sin(dLng / 2), 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
