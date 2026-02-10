package com.ahmedbahaj.parking.exception;

public class ParkingNotAvailableException extends RuntimeException {
    public ParkingNotAvailableException(String message) {
        super(message);
    }
}
