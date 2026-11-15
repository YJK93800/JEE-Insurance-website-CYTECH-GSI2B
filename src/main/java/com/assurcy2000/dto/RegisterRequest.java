package com.assurcy2000.dto;


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
