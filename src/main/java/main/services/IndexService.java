package main.services;

import main.model.Lemma;
import org.springframework.data.repository.query.Param;

import javax.persistence.Tuple;
import java.util.*;

public interface IndexService {
    /**
     * Обновляет индексы в таблице
     * @param lemmas список лемм с данными для обновления
     * @param pageId идентификатор вебстраницы
     */
    void updateIndexes(ArrayList<Lemma> lemmas, int pageId);
    /**
     * Установка одинаковых значений 'rank' для идентичных лемм,
     * которые записываются в разные строки таблицы index,
     * например слова 'темный' и 'тёмный' будут одинаковыми при запросах в mysql
     * @param lemmaIds идентификаторы лемм
     * @param pageId идентификатор страницы
     */
    void alignLemmasRank(HashSet<Integer> lemmaIds, int pageId);
    /**
     * Удаление из таблицы индексов для указанных страниц
     * @param pageIds список идентификаторов страниц
     */
    void deleteIndexesForPages(Set<Integer> pageIds);

    /**
     * Получение списка идентификаторов лемм и страниц,
     * отсортированных по параметру frequency леммы.
     * <p>Вспомогательный метод для обработки поисковых запросов
     * @param lemmaNames список имен лемм
     * @return список идентификаторов лемм и страниц
     */
    ArrayList<Tuple> getLemmaIdsAndPageIdsSortedByFrequency(Set<String> lemmaNames);
}
