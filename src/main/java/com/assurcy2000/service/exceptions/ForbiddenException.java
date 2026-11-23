package com.assurcy2000.service.exceptions;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException() { super("Accès refusé."); }
    public ForbiddenException(String message) { super(message); }
}
