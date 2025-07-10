package com.loqal.inventoryservice.entity;

import lombok.Getter;

@Getter
enum MovementType{
    IN("in"),
    OUT("out"),
    ADJUSTMENT("adjustment");

    private final String type;

    MovementType(String type) {
        this.type = type;
    }

    public static MovementType fromString(String type) {
        for (MovementType movementType : MovementType.values()) {
            if (movementType.type.equalsIgnoreCase(type)) {
                return movementType;
            }
        }
        throw new IllegalArgumentException("Unknown movement type: " + type);
    }
}
