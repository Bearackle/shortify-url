package com.dinhuan.shortify.service.impl;

import com.baidu.fsg.uid.impl.DefaultUidGenerator;
import com.dinhuan.shortify.configuration.exception.ShortUrlNotFoundException;
import com.dinhuan.shortify.configuration.uid.DefaultUid;
import com.dinhuan.shortify.configuration.url.ShortenerProperties;
import com.dinhuan.shortify.configuration.url.SqidsConfig;
import com.dinhuan.shortify.domain.shorturl.ShortUrl;
import com.dinhuan.shortify.repository.ShortUrlRepository;
import com.dinhuan.shortify.service.ShortUrlService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.sqids.Sqids;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShortUrlServiceImpl implements ShortUrlService {
    private final DefaultUidGenerator uid;
    private final ShortUrlRepository urlRepository;
    private final Sqids sqids;
    @Override
    public String createShortUrl(String longUrl) {
        var id = uid.getUID();
        ShortUrl shortUrl = ShortUrl.builder()
                .id(id)
                .shortCode(sqids.encode(List.of(id)))
                .originalUrl(longUrl)
                .build();
        urlRepository.save(shortUrl);
        return shortUrl.getShortCode();
    }
    @Override
    @Cacheable(value = "urls", key = "#shortCode")
    public String getLongUrl(String shortCode) {
        Long id = sqids.decode(shortCode).getFirst();
        return urlRepository.findById(id).orElseThrow(
                () -> new ShortUrlNotFoundException(shortCode)).getOriginalUrl();
    }
}
