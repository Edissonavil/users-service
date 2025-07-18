package com.aec.aec;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.aec.aec.UsersSrv.UsersServiceApplication;

// Le decimos explícitamente a Spring qué clase arrancar
@SpringBootTest(classes = UsersServiceApplication.class)
class UsersServiceApplicationTests {

    @Test
    void contextLoads() {
        // Si la aplicación arranca sin errores, este test pasa.
    }
}

