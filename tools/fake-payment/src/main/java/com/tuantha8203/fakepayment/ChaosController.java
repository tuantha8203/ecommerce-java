package com.tuantha8203.fakepayment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/chaos")
@RequiredArgsConstructor
class ChaosController {

    private final Chaos chaos;

    @GetMapping
    ChaosSettings get() {
        return chaos.settings();
    }

    @PutMapping
    ChaosSettings update(@RequestBody ChaosSettings settings) {
        chaos.update(settings);
        return settings;
    }
}