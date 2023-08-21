## 介绍
这是一个长链接转短链接的SpringBoot项目，利用Redis存储长短链接的键值关系
## 实现
### 创建项目
用Spring Initializr创建一个空项目，使用Java8、SpringBoot2版本，勾选spring web
### 前端页面
在resource/static下创建index.html
```html
<!DOCTYPE html>
<html>
<head>
    <title>短链接生成器</title>
    <meta charset="UTF-8">
    <link rel="icon" href="https://cdn.staticaly.com/gh/xinhaojin/imgs-host@master/20230821/tutu.7cfpopzisy40.webp" sizes="16*16">
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            margin-top: 10%;
            padding: 0;
            background-color: #f4f4f4;
        }
        .container {
            max-width: 800px;
            margin: auto;
            padding: 20px;
            text-align: center;
        }
        h1 {
            color: #333;
        }
        input[type="text"] {
            width: 80%;
            padding: 10px;
            margin-bottom: 10px;
            border: 1px solid #ccc;
            border-radius: 5px;
        }
        button {
            padding: 10px 20px;
            background-color: #007bff;
            color: #fff;
            border: none;
            border-radius: 5px;
            cursor: pointer;
        }
        button:hover {
            background-color: #0056b3;
        }
        #shortUrl {
            font-weight: bold;
            color: #007bff;
        }
        .copy-message {
            display: none;
            color: #28a745;
            font-weight: bold;
            opacity: 0;
            transition: opacity 0.5s ease-in-out;
        }
    </style>
</head>
<body>
<div class="container">
    <h1>短链接生成器</h1>
    <input type="text" id="longUrl" placeholder="请输入原始长链接">
    <button onclick="shortenURL()">生成短链接</button>
    <p>短链接: <span id="shortUrl"></span></p>
    <button onclick="copyToClipboard()">复制到剪贴板</button>
    <div class="copy-message" id="copyMessage">复制成功！</div>
</div>

<script>
    function shortenURL() {
        const longUrl = document.getElementById('longUrl').value;
        fetch(`/api/shorten?url=${encodeURIComponent(longUrl)}`)
            .then(response => response.text())
            .then(shortUrl => {
                const fullShortUrl = `https://s.xinhaojin.top/s/${shortUrl}`;
                const shortUrlElement = document.getElementById('shortUrl');
                shortUrlElement.innerHTML = `<a href="${fullShortUrl}" target="_blank">${fullShortUrl}</a>`;
            });
    }


    function copyToClipboard() {
        const shortUrl = document.getElementById('shortUrl').textContent;
        const tempInput = document.createElement('input');
        tempInput.value = shortUrl;
        document.body.appendChild(tempInput);
        tempInput.select();
        document.execCommand('copy');
        document.body.removeChild(tempInput);

        const copyMessage = document.getElementById('copyMessage');
        copyMessage.style.display = 'block';
        setTimeout(() => {
            copyMessage.style.opacity = '1';
        }, 10);

        setTimeout(() => {
            copyMessage.style.opacity = '0';
        }, 1500);
    }
</script>
</body>
</html>
```
### controller
MyController.java，给前端提供生成短链接和恢复长链接的接口
```java
package top.xinhaojin.shorturl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.xinhaojin.shorturl.service.MyService;

@RestController
public class MyController {

    private final MyService myService;

    // 通过构造函数注入 MyService
    @Autowired
    public MyController(MyService myService) {
        this.myService = myService;
    }

    // 处理生成短链接的请求
    @GetMapping("/api/shorten")
    public String shortenURL(@RequestParam String url) {
        // 调用 MyService 中的方法，生成短链接并返回
        return myService.shortenURL(url);
    }

    // 处理还原长链接的请求
    @GetMapping("/api/restore")
    public String restoreURL(@RequestParam String key) {
        // 调用 MyService 中的方法，还原短链接为长链接并返回
        return myService.restoreURL(key);
    }
}
```
RedirectController.java,处理用户访问短链接时的重定向
```java
package top.xinhaojin.shorturl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import top.xinhaojin.shorturl.service.MyService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/s")
public class RedirectController {

    private final MyService myService;

    @Autowired
    public RedirectController(MyService myService) {
        this.myService = myService;
    }

    @GetMapping("/{shortKey}")
    public void redirect(@PathVariable String shortKey, HttpServletResponse response) throws IOException {
        String originalUrl = myService.restoreURL(shortKey);
        if (originalUrl != null) {
            response.sendRedirect(originalUrl);
        } else {
            // Handle not found case
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
```
### service
MyService.java
```java
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
```
### application.properties
```
spring.redis.host=1.15.xx.xxx
spring.redis.port=6379

server.port=7777
```
## docker部署
### 在项目根目录终端下执行以下maven命令，会在target下生成jar文件
```bash
mvn clean package
```
### 新建一个名为Dockerfile的文件，内容如下
```dockerfile
# 使用一个基础的 Java 镜像
FROM openjdk:8-jdk-alpine

# 拷贝 JAR 文件到容器中
COPY your-application-name-version.jar /app.jar

# 暴露应用程序运行的端口
EXPOSE 7777

# 运行 Spring Boot 应用程序
CMD ["java", "-jar", "/app.jar"]
```
### 在有docker环境的地方如安装了docker的Ubuntu服务器上，创建docker镜像
```bash
# docker build -t your-image-name:your-tag .
docker build -t short-url:v1.0 .
```
### 运行docker镜像
```bash
# docker run -d -p 7777:7777 your-image-name:tag
docker run -d -p 7777:7777 short-url:v1.0
```
### 检查运行情况
```bash
docker ps
```
## 绑定域名,用nginx做好反向代理
解析一个二级域名到目标服务器，如s.xinhaojin.top
### 查看nginx版本，如果没有，就安装
```bash
nginx -v
sudo apt update
sudo apt install nginx
```
### 修改/etc/nginx/nginx.conf
在http标签里添加一个server
```nginx
server {
		listen 80;
		server_name s.xinhaojin.top;

		location / {
			proxy_pass http://1.15.xx.xxx:7777;
			proxy_set_header Host $host;
			proxy_set_header X-Real-IP $remote_addr;
			proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
			proxy_set_header X-Forwarded-Proto $scheme;
		}
	}
```
### 检查 Nginx 服务是否已经存在于 systemd 中：
```bash
sudo systemctl is-enabled nginx
```
如果返回 disabled，则表示 Nginx 目前不会开机自启。
如果 Nginx 尚未开机自启，可以使用以下命令启用：
```bash
sudo systemctl enable nginx
```
### 申请SSL证书，需要临时关闭nginx以释放80端口
```bash
sudo systemctl stop nginx
sudo certbot certonly --standalone -d s.xinhaojin.top
```
### 修改nginx配置/etc/nginx/nginx.conf
把上一个server删去，替换以下代码
```nginx
server {
    listen 80;
    server_name s.xinhaojin.top;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name s.xinhaojin.top;

    ssl_certificate /etc/letsencrypt/live/s.xinhaojin.top/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/s.xinhaojin.top/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers off;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;

    location / {
        proxy_pass http://127.0.0.1:7777;  # Change to your Spring Boot app's IP and port
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ~ /.well-known {
        allow all;
    }
}
```
### 重启nginx
```bash
sudo systemctl start nginx
sudo systemctl reload nginx
```