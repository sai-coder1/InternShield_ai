package com.internshield;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AppTest {

    @Test
    void contextLoads() {
        assertNotNull(new App());
    }
}
