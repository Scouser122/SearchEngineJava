package main.controllers;

import com.github.cliftonlabs.json_simple.JsonObject;
import main.services.SearchService;
import main.utils.TimeCounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

// контроллер для обработки поисковых запросов
@Controller
public class SearchController {
    // сервис будет выполнять поиск по базе и возвращать результат
    @Autowired
    private SearchService searchService;
    private static final int DEFAULT_OFFSET = 0; // сдвиг от начала списка результатов по умолчанию
    private static final int DEFAULT_LIMIT = 20; // кол-во результатов по умолчанию

    /**
     * Осуществляет поиск страниц по переданному поисковому запросу (параметр query)
     * @param qParams список параметров запроса, может содержать параметры:
     *                <p> query - поисковый запрос (обязательный),
     *                <p> site - по какому вебсайту искать,
     *                <p> offset - сдвиг от начала списка результатов,
     *                <p> limit - количество результатов, которое необходимо вывести.
     * @return обьект ответа с результатми поиска в формате JSON
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(required = false) Map<String, String> qParams) {
        TimeCounter timeCounter = new TimeCounter("запрос /search");
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        JsonObject response = new JsonObject();
        String query = qParams.get("query");
        if (query == null || query.isEmpty()) {
            response.put("result", false);
            response.put("error", "Задан пустой поисковый запрос");
            timeCounter.printStats();
            return new ResponseEntity<>(response.toJson(), httpHeaders, HttpStatus.OK);
        }
        String siteUrl = qParams.get("site");
        String offsetString = qParams.get("offset");
        String limitString = qParams.get("limit");
        int offset = offsetString == null ? DEFAULT_OFFSET : Integer.parseInt(offsetString);
        int limit = limitString == null ? DEFAULT_LIMIT : Integer.parseInt(limitString);
        response = searchService.processSearch(query, siteUrl, offset, limit);
        timeCounter.printStats();
        return new ResponseEntity<>(response.toJson(), httpHeaders, HttpStatus.OK);
    }
}
