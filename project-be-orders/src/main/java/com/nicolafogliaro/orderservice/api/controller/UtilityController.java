package com.nicolafogliaro.orderservice.api.controller;

import com.nicolafogliaro.orderservice.api.service.CacheService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cache")
@Tag(name = "Cache API", description = "Utility to clean cache.")
public class UtilityController {


    @Autowired(required = false)
    private CacheService cacheService;

    @GetMapping("/clearCache")
    public void clearCache() {
        cacheService.clearCache();
    }

}
