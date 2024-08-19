package com.hmdp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class SpringDataRedisDemoAranApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void test() {
        stringRedisTemplate.opsForValue().set("name", "虎哥224562");
        Object name = stringRedisTemplate.opsForValue().get("name");
        System.out.println("name = " + name);
    }

}
