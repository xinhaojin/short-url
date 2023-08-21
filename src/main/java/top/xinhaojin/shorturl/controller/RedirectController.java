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
