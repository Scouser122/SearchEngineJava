package main.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
// контроллер для показа вебстраницы по пути /admin заданном в конфигурационном файле
public class AdminController {
    // переменная для передачи данных из файла конфигурации в model для подстановки в html коде страницы
    @Value("${backend-api-url}")
    private String backendApiUrl;

    /**
     * Показ страницы управления сайтами по запросу domain/admin
     * @param model обьект для передачи данных для подстановки в html коде страницы
     * @return имя html файла в ресурсах, который нужно использовать для показа страницы
     */
    @RequestMapping("${web-interface-path}")
    public String admin(Model model) {
        model.addAttribute("backendUrl", backendApiUrl);
        return "index";
    }
    /**
     * Показ страницы управления сайтами по запросу domain/ или domain/index
     * @param model обьект для передачи данных для подстановки в html коде страницы
     * @return имя html файла в ресурсах, который нужно использовать для показа страницы
     */
    @RequestMapping("/")
    public String index(Model model) {
        model.addAttribute("backendUrl", backendApiUrl);
        return "index";
    }
}
