package main.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

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
}
