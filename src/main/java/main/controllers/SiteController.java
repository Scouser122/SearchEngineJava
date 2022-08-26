package main.controllers;

import com.github.cliftonlabs.json_simple.JsonObject;
import main.services.SiteService;
import main.utils.TimeCounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// контроллер для обработки запросов статистики и индексации
@Controller
public class SiteController {
    // сервис будет выполнять работу с данными и возвращать результат
    @Autowired
    private SiteService siteService;

    /**
     * Метод запуска процесса индексации
     * @return обьект ответа с результатом попытки запуска индексации
     */
    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        TimeCounter timeCounter = new TimeCounter("запрос /startIndexing");
        final JsonObject object = new JsonObject();
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if(siteService.getIndexing()) {
            object.put("result", false);
            object.put("error", "Индексация уже запущена");
            timeCounter.printStats();
            return new ResponseEntity<>(object.toJson(), httpHeaders, HttpStatus.OK);
        }
        siteService.startIndexing();
        object.put("result", true);
        timeCounter.printStats();
        return new ResponseEntity<>(object.toJson(), httpHeaders, HttpStatus.OK);
    }

    /**
     * Метод остановки процесса индексации
     * @return обьект ответа с результатом попытки остановки индексации
     */
    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        TimeCounter timeCounter = new TimeCounter("запрос /stopIndexing");
        final JsonObject object = new JsonObject();
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (!siteService.getIndexing()) {
            object.put("result", false);
            object.put("error", "Индексация не запущена");
            timeCounter.printStats();
            return new ResponseEntity<>(object.toJson(), httpHeaders, HttpStatus.OK);
        }
        siteService.stopIndexing();
        object.put("result", true);
        timeCounter.printStats();
        return new ResponseEntity<>(object.toJson(), httpHeaders, HttpStatus.OK);
    }

    /**
     * Индексация отдельной страницы
     * @param url - путь к странице
     * @return обьект ответа с результатом попытки запуска индексации
     */
    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam(name = "url") String url) {
        TimeCounter timeCounter = new TimeCounter("запрос /indexPage");
        final JsonObject object = new JsonObject();
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (!siteService.indexPage(url)) {
            object.put("result", false);
            object.put("error", "Данная страница находится за пределами сайтов," +
                    " указанных в конфигурационном файле");
            timeCounter.printStats();
            return new ResponseEntity<>(object.toJson(), httpHeaders, HttpStatus.OK);
        }
        object.put("result", true);
        timeCounter.printStats();
        return new ResponseEntity<>(object.toJson(), httpHeaders, HttpStatus.OK);
    }

    /**
     * Получение статистики по индексированным сайтам и страницам
     * @return обьект ответа с данными статистики
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> statistics() {
        TimeCounter timeCounter = new TimeCounter("запрос /statistics");
        final JsonObject object = new JsonObject();
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        object.put("result", true);
        object.put("statistics", siteService.getSitesData());
        timeCounter.printStats();
        return new ResponseEntity<>(object.toJson(), httpHeaders, HttpStatus.OK);
    }
}
