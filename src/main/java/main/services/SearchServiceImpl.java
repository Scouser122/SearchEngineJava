package main.services;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import main.model.Lemma;
import main.model.Page;
import main.model.Site;
import main.utils.TimeCounter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.Tuple;
import java.util.*;

/**
 * Сервис для поиска текста по страницам сохраненным в базе
 */
@Service
public class SearchServiceImpl implements SearchService {
    // ссылка на обьект для доступа к сервису лемм
    @Autowired
    private LemmaService lemmaService;
    // ссылка на обьект для доступа к сервису индексов
    @Autowired
    private IndexService indexService;
    // ссылка на обьект для доступа к сервису вебстраниц
    @Autowired
    private PageService pageService;
    // ссылка на обьект для доступа к сервису сайтов
    @Autowired
    private SiteService siteService;
    // количество слов, которое отображаем во фрагменте в результатах поиска перед найденными словами
    private static final int SNIPPET_WORDS_BEFORE = 3;
    // количество слов, которое отображаем во фрагменте в результатах поиска после найденных слов
    private static final int SNIPPET_WORDS_AFTER = 5;

    public JsonObject processSearch(String query, String siteUrl, int offset, int limit) {
        query = query.toLowerCase();

        JsonObject result = new JsonObject();
        result.put("result", true);
        JsonArray arrayResult = new JsonArray();

        Set<Integer> pageIds = getPageIdsForLemmasInQuery(query);
        if (pageIds.isEmpty()) {
            result.put("count", 0);
            result.put("data", arrayResult);
            return result;
        }

        pageIds = filterPagesBySite(siteUrl, pageIds);

        ArrayList<Tuple> sqlResults = pageService.searchPagesForIds(pageIds, offset, limit);
        result.put("count", pageIds.size());
        pageIds.clear();
        Double maxRank = sqlResults.isEmpty() ? 0.0 : sqlResults.get(0).get(1, Double.class);
        for (Tuple tuple : sqlResults) {
            pageIds.add(tuple.get(0, Integer.class));
        }
        HashMap<Integer, Page> pageHashMap = pageService.getPages(pageIds);
        for (Tuple tuple : sqlResults) {
            Page page = pageHashMap.get(tuple.get(0, Integer.class));
            if (page != null) {
                page.setRelevance((float)(tuple.get(1, Double.class) / maxRank));
                JsonObject item = getSearchItem(page, query);
                arrayResult.add(item);
            }
        }
        result.put("data", arrayResult);

        return result;
    }

    /**
     * Получить список идентификаторов страниц,
     * на которых находятся леммы, найденные в заданном запросе.
     * <p> Леммы сортируются по возрастанию frequency,
     * <p> для каждой следущей леммы в списке удаляем из списка идентификаторов страниц те,
     * которые не были найдены для предыдущей леммы
     * @param query текст запроса
     * @return список идентификаторов страниц
     */
    private Set<Integer> getPageIdsForLemmasInQuery(String query) {
        HashSet<String> lemmaStrings = lemmaService.findLemmaStringsInText(query);
        if (lemmaStrings.isEmpty()) {
            return new HashSet<>();
        }
        ArrayList<Tuple> sqlResults = indexService.getLemmaIdsAndPageIdsSortedByFrequency(lemmaStrings);
        Set<Integer> pageIds = new HashSet<>();
        Set<Integer> prevPageIds = null;
        Integer currentLemmaId = sqlResults.isEmpty() ? -1 : sqlResults.get(0).get(0, Integer.class);
        for (Tuple sqlResult : sqlResults) {
            Integer lemmaId = sqlResult.get(0, Integer.class);
            Integer pageId = sqlResult.get(1, Integer.class);
            if (lemmaId.equals(currentLemmaId)) {
                if (prevPageIds == null || prevPageIds.contains(pageId)) {
                    pageIds.add(pageId);
                }
            } else {
                prevPageIds = pageIds;
                pageIds = new HashSet<>();
                if (prevPageIds.contains(pageId)) {
                    pageIds.add(pageId);
                }
                currentLemmaId = lemmaId;
            }
        }
        return pageIds;
    }

    /**
     * Фильтрация списка страниц по идентификатору сайта
     * @param siteUrl url сайта
     * @param pageIds список страниц
     * @return отфильтрованных список страниц
     */
    private Set<Integer> filterPagesBySite(String siteUrl, Set<Integer> pageIds) {
        if (siteUrl == null || siteUrl.isEmpty()) {
            return pageIds;
        }
        Site site = siteService.getSite(siteUrl, false);
        if (site == null) {
            return pageIds;
        }
        Set<Integer> sitePageIds = pageService.getPageIdsForSite(site.getId());
        pageIds.removeIf(pageId -> !sitePageIds.contains(pageId));
        return pageIds;
    }

    /**
     * Получения элемента списка результатов поиска
     * @param page обьект страницы
     * @param query текст поискового запроса
     * @return данные для отображения элемента списка результатов поиска
     */
    private JsonObject getSearchItem(Page page, String query) {
        JsonObject result = new JsonObject();
        result.put("site", page.getSite().getUrl());
        result.put("siteName", page.getSite().getName());
        result.put("uri", page.getPath());
        Document document = Jsoup.parse(page.getContent());
        Elements titleElements = document.select("title");
        if (titleElements.size() > 0) {
            Element title = titleElements.first();
            if (title != null) {
                result.put("title", title.text());
            }
        }
        Elements bodyElements = document.select("body");
        if (bodyElements.size() > 0) {
            String body = Objects.requireNonNull(bodyElements.first()).text();
            String bodyLowerCase = body.toLowerCase();
            String snippet = searchForSnippetInBody(body, bodyLowerCase, query);
            if (!snippet.isEmpty()) {
                result.put("snippet", snippet);
            }
        }
        result.put("relevance", page.getRelevance());
        return result;
    }

    /**
     * Поиск текста в теле вебстраницы
     * @param body текст элемента body вебстраницы
     * @param query поисковый запрос
     * @return найденный фрагмент текста
     */
    private String searchForSnippetInBody(String body, String bodyLowerCase, String query) {
        String snippet = getSnippetFromBody(body, bodyLowerCase, query);
        if (snippet != null) {
            return snippet;
        }
        snippet = findSnippetInQueryPart(body, bodyLowerCase, query);
        if (snippet != null) {
            return snippet;
        }
        return findSnippetQueryLemmas(body, bodyLowerCase, query);
    }

    /**
     * Поиск фрагмента текста в теле вебстраницы по части поискового запроса
     * @param body текст элемента body вебстраницы
     * @param query поисковый запрос
     * @return найденный фрагмент текста
     */
    private String findSnippetInQueryPart(String body, String bodyLowerCase, String query) {
        String snippet;
        String [] words = query.split(" ");
        if (words.length <= 1) {
            return null;
        }
        String fromEndToStart = query;
        while (!fromEndToStart.isEmpty()) { // перебор частей поискового запроса от конца к началу
            int spacePos = fromEndToStart.lastIndexOf(" ");
            if (spacePos != -1) {
                fromEndToStart = fromEndToStart.substring(0, spacePos);
                snippet = getSnippetFromBody(body, bodyLowerCase, fromEndToStart);
                if (snippet != null) {
                    return snippet;
                }
            } else {
                fromEndToStart = "";
            }
        }
        String fromStartToEnd = query;
        while (!fromStartToEnd.isEmpty()) { // перебор частей поискового запроса от начала к концу
            int spacePos = fromStartToEnd.indexOf(" ");
            if (spacePos != -1) {
                fromStartToEnd = fromStartToEnd.substring(spacePos + 1);
                snippet = getSnippetFromBody(body, bodyLowerCase, fromStartToEnd);
                if (snippet != null) {
                    return snippet;
                }
            } else {
                fromStartToEnd = "";
            }
        }
        return null;
    }

    /**
     * Поиск фрагмента текста по словам и леммам
     * @param body текст элемента body вебстраницы
     * @param query поисковый запрос
     * @return найденный фрагмент текста
     */
    private String findSnippetQueryLemmas(String body, String bodyLowerCase, String query) {
        String snippet;
        String [] words = query.split(" ");
        for (String word : words) {
            String wordPart = word;
            while (wordPart.length() > 1) {
                snippet = getSnippetFromBody(body, bodyLowerCase, wordPart);
                if (snippet != null) {
                    return snippet;
                }
                wordPart = wordPart.substring(0, wordPart.length() - 1);
            }
        }
        HashSet<String> lemmaStrings = lemmaService.findLemmaStringsInText(query);
        for (String lemmaString : lemmaStrings) {
            String wordPart = lemmaString;
            while (wordPart.length() > 1) {
                snippet = getSnippetFromBody(body, bodyLowerCase, wordPart);
                if (snippet != null) {
                    return snippet;
                }
                wordPart = wordPart.substring(0, wordPart.length() - 1);
            }
        }
        return "";
    }

    /**
     * Поиск текста в теле вебстраницы по части поискового запроса
     * @param body текст элемента body вебстраницы
     * @param searchString искомый текст
     * @return фрагмент с найденным текстом, null - если фрагмент не найден
     */
    private String getSnippetFromBody(String body, String bodyLowerCase, String searchString) {
        if (searchString.isEmpty()) {
            return null;
        }
        int queryStart = bodyLowerCase.indexOf(searchString);
        if (queryStart == -1) {
            return null;
        }
        int queryEnd = queryStart + searchString.length();
        searchString = body.substring(queryStart, queryEnd);
        int snippetStart = queryStart;
        int wordsBefore = 0;
        while (wordsBefore < SNIPPET_WORDS_BEFORE && snippetStart != 0) {
            snippetStart--;
            while (snippetStart != 0 && body.charAt(snippetStart) != ' ') {
                snippetStart--;
            }
            wordsBefore++;
        }
        int wordsAfter = 0;
        int snippetEnd = queryEnd;
        while (wordsAfter < SNIPPET_WORDS_AFTER && snippetEnd != (body.length() - 1)) {
            snippetEnd++;
            while (snippetEnd != (body.length() - 1) && body.charAt(snippetEnd) != ' ') {
                snippetEnd++;
            }
            wordsAfter++;
        }
        String snippet = body.substring(snippetStart, snippetEnd);
        snippet = snippet.replaceAll(searchString, "<b>" + searchString + "</b>");
        return snippet;
    }
}
