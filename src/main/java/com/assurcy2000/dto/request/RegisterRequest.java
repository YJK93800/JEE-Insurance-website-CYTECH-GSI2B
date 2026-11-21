package com.assurcy2000.dto.request;


public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String birthDate,
        String address,
        String password,
        String passwordConfirm) {
}
