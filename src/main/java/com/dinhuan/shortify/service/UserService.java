package com.dinhuan.shortify.service;

public interface UserService {
    boolean register(String email, String password);
    boolean login(String email, String password);
    boolean getProfile(String email);
}
