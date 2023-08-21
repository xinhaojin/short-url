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
