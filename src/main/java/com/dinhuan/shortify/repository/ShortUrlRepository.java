package com.dinhuan.shortify.repository;

import com.dinhuan.shortify.domain.shorturl.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

}
