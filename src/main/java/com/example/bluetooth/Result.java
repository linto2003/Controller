package com.example.bluetooth;

public interface Result {
    void onSuccess(String message);
    void onFailure(String message);
}
