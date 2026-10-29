package com.tuantha8203.shop;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

    static final ApplicationModules modules = ApplicationModules.of(ShopApplication.class);

    @Test
    void kiemTraRanhGioiModule() {
        modules.forEach(System.out::println); // in ra các module Modulith phát hiện được
        modules.verify();
    }

    @Test
    void sinhTaiLieuModule() {
        new Documenter(modules).writeDocumentation();
    }
}
