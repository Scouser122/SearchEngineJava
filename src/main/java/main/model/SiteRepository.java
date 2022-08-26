package main.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.persistence.Tuple;
import java.util.Set;

/**
 * Интерфейс для работы с таблицей site в БД
 */
public interface SiteRepository extends CrudRepository<Site, Integer> {
    /**
     * Поиск сайта по адресу
     * @param url адрес сайта
     * @return обьект с данными записи по сайту
     */
    Site findByUrl(String url);

    /**
     * Поиск сайтов по адресам
     * @param urls список адресов сайтов
     * @return список сайтов
     */
    Iterable<Site> findByUrlIn(Set<String> urls);

    /**
     * @return общее количество сайтов в таблице
     */
    @Query(value = "SELECT COUNT(*) FROM site", nativeQuery = true)
    int getNumSites();

    /**
     * Получение статистики по всем сайтам
     * @return список кортежей с 3 параметрами:
     * <p> site_id - идентификатор сайта
     * <p> lemmas - количество найденных лемм на страницах сайта
     * <p> pages - количество проиндексированных страниц сайта
     */
    @Query(value = "select t2.site_id, count(DISTINCT t1.lemma_id) as lemmas, count(DISTINCT t1.page_id) as pages\n" +
            "from `index` t1\n" +
            "inner join page t2\n" +
            "on t1.page_id = t2.id\n" +
            "group by t2.site_id", nativeQuery = true)
    Iterable<Tuple> getSitesStats();
}
