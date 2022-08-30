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
 * Интерфейс для работы с таблицей indexes в БД
 */
public interface IndexRepository extends CrudRepository<Index, Integer> {
    /**
     * Поиск списка индексов
     * @param pageId идентификатор страницы
     * @param lemmaId список идентификаторов лемм
     * @return список индексов
     */
    Iterable<Index> findByPageIdAndLemmaIdIn(int pageId, Set<Integer> lemmaId);

    /**
     * Поиск списка индексов
     * @param lemmaId идентификатор леммы
     * @return список индексов
     */
    Iterable<Index> findByLemmaId(int lemmaId);

    /**
     * Поиск списка индексов
     * @param pageIds список идентификаторов страниц
     * @return список индексов
     */
    Iterable<Index> findByPageIdIn(Set<Integer> pageIds);

    /**
     * Поиск уникальных идентификаторов страниц, которые соответствуют лемме
     * @param lemmaId идентификатор леммы
     * @return список идентификаторов страниц
     */
    @Query(value = "select distinct page_id \n" +
            "from `index`\n" +
            "where lemma_id = :lemmaId", nativeQuery = true)
    Set<Integer> findPageIdsForLemma(@Param("lemmaId") Integer lemmaId);

    /**
     * Поиск уникальных идентификаторов страниц, которые соответствуют лемме и страницам
     * @param lemmaId идентификатор леммы
     * @param pageIds идентификаторы страниц
     * @return список идентификаторов страниц
     */
    @Query(value = "select distinct page_id \n" +
            "from `index`\n" +
            "where lemma_id = :lemmaId\n" +
            "and page_id in (:pageIds)", nativeQuery = true)
    Set<Integer> findPageIdsForLemmaAndPages(@Param("lemmaId") Integer lemmaId, @Param("pageIds") Set<Integer> pageIds);

    /**
     * Удаление всех индексов для страниц
     * @param pageIds список идентификаторов страниц
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM `index` WHERE page_id in (:pageIds)", nativeQuery = true)
    void deleteByPageIds(@Param("pageIds") Set<Integer> pageIds);

    /**
     * Удаление всех индексов
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM `index`", nativeQuery = true)
    void deleteAllIndexes();

    /**
     * Получение списка идентификаторов лемм и страниц,
     * отсортированных по параметру frequency леммы.
     * <p>Вспомогательный метод для обработки поисковых запросов
     * @param lemmaNames список имен лемм
     * @return список идентификаторов лемм и страниц
     */
    @Query(value = "select it.lemma_id, it.page_id\n" +
            "from `index` as it\n" +
            "inner join lemma as lt\n" +
            "on it.lemma_id = lt.id\n" +
            "where lt.lemma in (:lemmaNames)\n" +
            "order by lt.frequency asc", nativeQuery = true)
    ArrayList<Tuple> getLemmaIdsAndPageIdsSortedByFrequency(@Param("lemmaNames") Set<String> lemmaNames);

    /**
     * Поиск уникальных идентификаторов лемм, которые были найдены на страницах
     * @param pageIds идентификаторы страниц
     * @return список идентификаторов лемм
     */
    @Query(value = "select distinct lemma_id \n" +
            "from `index`\n" +
            "where page_id in (:pageIds)", nativeQuery = true)
    Set<Integer> getLemmaIdsForPageIds(@Param("pageIds") Set<Integer> pageIds);
}
