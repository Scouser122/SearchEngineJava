package main.services;

import main.model.Page;
import main.model.Site;
import main.utils.bypass.BypassData;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public interface PageService {
    /**
     * Получение списка страниц из таблицы по идентификаторам
     * @param pageIds список идентификаторов страниц
     * @return список страниц
     */
    HashMap<Integer, Page> getPages(Set<Integer> pageIds);

    /**
     * Сохранение/обновление данных по странице в базе
     * @param site ссылка на обьект вебсайта
     * @param data данные результатов сканирования страницы
     * @return идентификатор сохраненной страницы, -1 если страницу не удалось сохранить
     */
    int savePage(Site site, BypassData data);

    /**
     * @return общее количество проиндексированных вебстраниц
     */
    long getNumPages();

    /**
     * Получения списка идентификаторов страниц для сайта
     * @param siteId идентификатор сайта
     * @return список идентификаторов вебстраниц
     */
    Set<Integer> getPageIdsForSite(int siteId);

    /**
     * Удаление всех проиндексированных страниц для сайта
     * @param siteId идентификатор сайта
     */
    void deletePagesForSite(int siteId);

    /**
     * Получение списка идентификаторов страниц,
     * отсортированного по убыванию рассчитанного rank для каждой страницы.
     * <p> Вспомогательный метод для обработки поисковых запросов
     * @param pageIds список идентификаторов страниц, по которому будет проводиться поиск
     * @param offset сдвиг от начала списка результатов
     * @param limit количество результатов, которое необходимо вывести
     * @return список идентификаторов страниц и рассчитанного rank для каждой страницы
     */
    ArrayList<Tuple> searchPagesForIds(Set<Integer> pageIds, Integer offset, Integer limit);
}
