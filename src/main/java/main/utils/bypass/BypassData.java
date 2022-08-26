package main.utils.bypass;

import lombok.Data;

import java.util.ArrayList;

/**
 * Данные результатов сканирования страницы
 */
@Data
public class BypassData {
    // путь к странице на сайте
    private String path;
    // HTTP status code полученный при попытке загрузки страницы
    private int statusCode;
    // контент страницы
    private String content;
    // список "дочерних" страниц, url которых найдены на этой странице
    private ArrayList<BypassData> children = new ArrayList<>();

    /**
     * Добавление "дочерней" страницы
     * @param data данные результатов сканирования страницы
     */
    public void addChild(BypassData data) {
        children.add(data);
    }
}
