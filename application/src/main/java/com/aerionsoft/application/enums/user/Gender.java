package com.aerionsoft.application.enums.user;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other");

    private final String display;

    Gender(String display) {
        this.display = display;
    }

}

