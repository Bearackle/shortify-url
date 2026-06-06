package com.dinhuan.shortify.service;

public interface ShortUrlService {
    String createShortUrl(String longUrl);
    String getLongUrl(String shortCode);
}
