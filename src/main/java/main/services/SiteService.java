package main.services;

import com.github.cliftonlabs.json_simple.JsonObject;
import main.model.*;
import main.utils.bypass.BypassData;
import org.jsoup.nodes.Document;
import org.springframework.data.repository.query.Param;

import javax.persistence.Tuple;
import java.util.Set;

public interface SiteService {
    /**
     * Получение сайта из таблицы
     * @param siteUrl url сайта
     * @param createIfNotExist создать новый обьект если не был создан ранее
     * @return обьект с данными сайта
     */
    Site getSite(String siteUrl, boolean createIfNotExist);
    /**
     * Запуск процесса индексации
     */
    void startIndexing();
    /**
     * @return статус процесса индексации, true если индексация в данный момент в процессе
     */
    boolean getIndexing();
    /**
     * Остановка индексации
     */
    void stopIndexing();
    /**
     * Индексация отдельной страницы
     * @param url url вебстраницы
     * @return false - если этой страницы нет на сайте из списка в application.yml, иначе - true
     */
    boolean indexPage(String url);
    /**
     * Получение общей статистики по индексации сайтов
     * @return JSON обьект с данными статистики
     */
    JsonObject getSitesData();
    /**
     * Сахранение данных о странице в базе,
     * вызывается из утилиты обхода страниц сайта
     * @param site обьект сайта
     * @param data данные результатов сканирования страницы
     * @return идентификатор сохраненной страницы, -1 если страницу не удалось сохранить
     */
    int savePage(Site site, BypassData data);
    /**
     * Получение лемм из обьекта содержащего иходных код страницы,
     * вызывается из утилиты обхода страниц сайта
     * @param document обьект содержащий иходных код страницы
     * @param pageId идентификатор страницы
     */
    void scanLemmas(Document document, int pageId);
    /**
     * Установка ошибки индексации сайта,
     * вызывается из утилиты обхода страниц сайта
     * @param site обьект сайта
     * @param error текст ошибки
     */
    void setSiteError(Site site, String error);

    /**
     * Удаление одного сайта
     * @param siteId идентификатор сайта
     */
    void deleteSite(int siteId);
}
