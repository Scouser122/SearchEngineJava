package main.services;

import com.github.cliftonlabs.json_simple.JsonObject;

public interface SearchService {
    /**
     * Обработка поискового запроса и выдача результата
     * @param query текст запроса
     * @param siteUrl url вебсайта
     * @param offset сдвиг от начала списка результатов
     * @param limit количество результатов, которое необходимо вывести
     * @return список найденных страниц и фрагментов текста с найденными словами
     */
    JsonObject processSearch(String query, String siteUrl, int offset, int limit);
}
