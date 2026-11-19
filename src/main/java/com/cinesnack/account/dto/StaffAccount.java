package com.cinesnack.account.dto;

public record StaffAccount(
    long id,
    String name,
    String email,
    String role,
    String status
) {}
