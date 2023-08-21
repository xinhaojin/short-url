package top.xinhaojin.shorturl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

@Service
public class MyService {

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public MyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String shortenURL(String longURL) {
        try {
            // 自动添加 http:// 前缀
            if (!longURL.startsWith("http://") && !longURL.startsWith("https://")) {
                longURL = "http://" + longURL;
            }

            if (!isValidURL(longURL)) {
                return "invalid";
            }

            String shortKey = generateShortKey(longURL);

            // 检查是否已存在相同的短链接
            String existingLongURL = redisTemplate.opsForValue().get(shortKey);
            if (existingLongURL != null) {
                return shortKey;
            }

            // 将短链接与长链接映射关系存储到 Redis 中
            redisTemplate.opsForValue().set(shortKey, longURL);

            return shortKey;
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String restoreURL(String shortKey) {
        try {
            // 从 Redis 中获取短链接对应的长链接
            return redisTemplate.opsForValue().get(shortKey);
        } catch (DataAccessException e) {
            e.printStackTrace();
            return "error";
        }
    }

    private String generateShortKey(String longURL) {
        try {
            // 创建 MD5 哈希函数实例
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(longURL.getBytes());

            // 获取生成的哈希字节数组
            byte[] hashBytes = md.digest();

            // 转换为固定长度的十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b & 0xff));
            }

            // 截取前 8 个字符作为短链接
            return sb.substring(0, 8);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // 在实际应用中，可以返回默认的短链接或其他处理方式
            return "default123";
        }
    }

    private boolean isValidURL(String url) {
        // 使用简单的正则表达式验证是否为合法网址
        String regex = "^(http|https)://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}(/.*)?$";
        return Pattern.matches(regex, url);
    }
}
