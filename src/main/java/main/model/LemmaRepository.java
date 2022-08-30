package main.model;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Интерфейс для работы с таблицей lemma в БД
 */
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {
    /**
     * Поиск списка лемм по имени
     * @param lemmas список имен лемм
     * @return список лемм
     */
    Iterable<Lemma> findByLemmaIn(Set<String> lemmas);

    /**
     * Поиск списка лемм по идентификатору
     * @param lemmaIds список идентификаторов лемм
     * @return список лемм
     */
    Iterable<Lemma> findByIdIn(Set<Integer> lemmaIds);

    /**
     * Поиск списка лемм по имени (отсортирован по возрастанию частоты леммы)
     * @param lemmas список имен лемм
     * @return список лемм
     */
    Iterable<Lemma> findByLemmaInOrderByFrequencyAsc(Set<String> lemmas);

    /**
     * @return общее количество лемм в базе
     */
    @Query(value = "SELECT COUNT(*) FROM lemma", nativeQuery = true)
    long getNumLemmas();

    /**
     * Удаление лемм по идентификатору
     * @param lemmaIds список идентификаторов лемм
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM lemma WHERE id in (:lemmaIds)", nativeQuery = true)
    void deleteLemmasByIds(@Param("lemmaIds") Set<Integer> lemmaIds);
}
