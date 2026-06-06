package com.dinhuan.shortify.controller;

import com.dinhuan.shortify.dto.shorturl.LongUrl;
import com.dinhuan.shortify.service.ShortUrlService;
import com.dinhuan.shortify.service.impl.ShortUrlServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class UrlController {
    private final ShortUrlService service;
    @PostMapping(path = "/url")
    @ResponseBody
    public Map<String,String> createShortUrl(@RequestBody LongUrl longUrl) {
        String shortCode = service.createShortUrl(longUrl.url());
        return Map.of("shortCode", shortCode);
    }
    @GetMapping(path = "/{shortCode}")
    public ResponseEntity<Void> redirect (@PathVariable String shortCode){
        String longUrl = service.getLongUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
