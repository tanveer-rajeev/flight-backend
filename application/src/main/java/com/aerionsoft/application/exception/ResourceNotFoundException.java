package com.aerionsoft.application.exception;

import com.aerionsoft.application.enums.common.ErrorCode;
import lombok.Getter;

/**
 * Thrown when a domain entity referenced by id (or another natural key) cannot
 * be located. The {@link GlobalExceptionHandler}
 * translates this to a 404 with code {@link ErrorCode#RESOURCE_NOT_FOUND}.
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String resource;
    private final Object identifier;

    public ResourceNotFoundException(String resource, Object identifier) {
        super(buildMessage(resource, identifier));
        this.resource = resource;
        this.identifier = identifier;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resource = null;
        this.identifier = null;
    }

    private static String buildMessage(String resource, Object identifier) {
        if (resource == null || resource.isBlank()) {
            return "Resource not found";
        }
        if (identifier == null) {
            return resource + " not found";
        }
        return resource + " with id " + identifier + " not found";
    }
}
