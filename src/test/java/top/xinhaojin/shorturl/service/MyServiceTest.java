package top.xinhaojin.shorturl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@SpringJUnitConfig
@ActiveProfiles("test")  // 如果需要使用不同的配置文件，可以在此指定
public class MyServiceTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MyService myService;

    @BeforeEach
    public void setUp() {
        // 在每个测试方法运行前，清空 Redis 中的数据
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    public void testShortenURL() {
        String longURL = "https://www.example.com";
        String shortKey = myService.shortenURL(longURL);
        assertNotNull(shortKey);
        String storedLongURL = redisTemplate.opsForValue().get(shortKey);
        System.out.println(longURL);
        System.out.println(storedLongURL);
        System.out.println(shortKey);
    }

    @Test
    public void testRestoreURL() {
        String longURL = "https://www.example.com";
        String shortKey = myService.shortenURL(longURL);
        String restoredURL = myService.restoreURL(shortKey);
        System.out.println(longURL);
        System.out.println(restoredURL);
        System.out.println(shortKey);
    }
}
