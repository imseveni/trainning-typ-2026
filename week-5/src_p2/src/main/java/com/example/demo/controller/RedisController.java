package com.example.demo.controller;

import com.example.demo.service.RedisService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/redis")
public class RedisController {

    private final RedisService redisService;

    public RedisController(RedisService redisService) {
        this.redisService = redisService;
    }

    // POST /redis?key={key}&value={value}
    @PostMapping
    public ResponseEntity<String> set(
            @RequestParam String key,
            @RequestParam String value) {

        redisService.set(key, value, 60);

        return ResponseEntity.ok("Saved to Redis");
    }

    // GET /redis/{key}
    @GetMapping("/{key}")
    public ResponseEntity<?> get(@PathVariable String key) {

        Object value = redisService.get(key);

        if (value == null) {
            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(value);
    }

    // GET /redis/{key}/ttl
    @GetMapping("/{key}/ttl")
    public ResponseEntity<Long> getTtl(
            @PathVariable String key) {

        return ResponseEntity.ok(
                redisService.getTtl(key));
    }

    // DELETE /redis/{key}
    @DeleteMapping("/{key}")
    public ResponseEntity<String> delete(
            @PathVariable String key) {

        redisService.delete(key);

        return ResponseEntity.ok("Deleted");
    }
}