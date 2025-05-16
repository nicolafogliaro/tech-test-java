package com.nicolafogliaro.orderservice.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheService {

    @Autowired
    private CacheManager manager;

    public void clearCache() {
        log.info(">>> [clearCache]");
        manager.getCacheNames().forEach(name -> manager.getCache(name).clear());
        log.info("<<< [clearCache]");
    }

}
