package main.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
// контроллер для показа вебстраницы по пути /admin заданном в конфигурационном файле
public class AdminController {
    @RequestMapping("${web-interface-path}")
    public String index() {
        return "index";
    }
}
