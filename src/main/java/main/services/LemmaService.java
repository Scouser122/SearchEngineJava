package main.services;

import main.model.Lemma;
import org.jsoup.nodes.Document;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public interface LemmaService {
    /**
     * Получение лемм из обьекта содержащего иходных код страницы
     * @param document обьект содержащий иходных код страницы
     * @param pageId идентификатор страницы
     */
    void processDocumentInPage(Document document, int pageId);

    /**
     * Поиск лемм из текста
     * @param text текст для поиска
     * @return список лемм в текстовом виде
     */
    HashSet<String> findLemmaStringsInText(String text);

    /**
     * @return количество лемм в таблице
     */
    long getNumLemmas();

    /**
     * Удаление или обновление лемм для удаляемых страниц
     * @param lemmaIds список идентификаторов страниц
     */
    void cleanUpLemmas(Set<Integer> lemmaIds);
}
