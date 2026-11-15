package com.assurcy2000.service.exceptions;

import java.util.Map;

public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super("Données invalides : " + errors);
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() { return errors; }
}
