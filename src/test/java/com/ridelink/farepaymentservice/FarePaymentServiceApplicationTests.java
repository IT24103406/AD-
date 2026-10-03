package com.ridelink.farepaymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "jwt.secret=VGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LXRvLXVzZQ=="
})
class FarePaymentServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
