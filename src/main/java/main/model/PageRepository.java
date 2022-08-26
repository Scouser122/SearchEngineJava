package main.model;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Set;

/**
 * Интерфейс для работы с таблицей page в БД
 */
public interface PageRepository extends CrudRepository<Page, Integer> {
    /**
     * Поиск по пути и идентификатору сайта
     * @param path путь к странице
     * @param siteId идентификатор сайта
     * @return обьект с данными записи по странице
     */
    Page findByPathAndSiteId(String path, int siteId);

    /**
     * Поиск списка страниц по идентификаторам
     * @param ids идентификаторы страниц
     * @return список страниц
     */
    Iterable<Page> findByIdIn(Set<Integer> ids);

    /**
     * @return общее количество страниц в базе
     */
    @Query(value = "SELECT COUNT(*) FROM page", nativeQuery = true)
    long getNumPages();

    /**
     * Поиск идентификаторов страниц для сайта
     * @param siteId идентификатор сайта
     * @return список идентификаторов страниц
     */
    @Query(value = "SELECT id FROM page WHERE site_id = :siteId", nativeQuery = true)
    Set<Integer> getPageIdsForSite(@Param("siteId") int siteId);

    /**
     * Удаление всех страниц для сайта
     * @param siteId идентификатор сайта
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM page WHERE site_id = :siteId", nativeQuery = true)
    void deleteBySiteId(@Param("siteId") int siteId);

    @Query(value = "select it.page_id, sum(it.rank) as sum_rank\n" +
            "from `index` as it\n" +
            "inner join page as pt\n" +
            "on it.page_id = pt.id\n" +
            "inner join site as st\n" +
            "on st.id = pt.site_id\n" +
            "where page_id in (:pageIds)\n" +
            "group by page_id\n" +
            "order by sum_rank desc\n" +
            "limit :offset, :rowLimit", nativeQuery = true)
    ArrayList<Tuple> searchPagesForIds(@Param("pageIds") Set<Integer> pageIds,
                                       @Param("offset") Integer offset,
                                       @Param("rowLimit") Integer limit);
}
