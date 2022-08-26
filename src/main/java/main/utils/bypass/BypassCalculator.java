package main.utils.bypass;

import main.model.*;
import main.services.SiteService;
import main.utils.event.CustomEvent;
import main.utils.event.CustomEventListener;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;

/**
 * Утилита обхода страниц сайта,
 * использует механизм Fork-Join для создания потоков для параллельной индексации страниц сайта
 */
public class BypassCalculator extends RecursiveTask<Void> {
    // данные результатов сканирования страницы
    private final BypassData data;
    // обьект с данными сайта
    private final Site site;
    // ссылка на обьект для доступа к сервису сайтов
    private final SiteService siteService;

    // значение user agent, используемое для загрузки контента страницы
    private static String userAgent;
    // флаг остановки общей индексации страниц
    private static volatile boolean stopIndexing = false;

    // обьект для вызова события остановки индексации
    private static final CustomEvent stopIndexingChanged = new CustomEvent();
    // список найденных на странице ссылок на другие страницы
    HashSet<String> links = new HashSet<>();

    /**
     * Установка значения user agent, используемого для загрузки контента страницы
     * @param value значение user agent
     */
    public static void setUserAgent(String value) {
        userAgent = value;
    }

    /**
     * Установка флага остановки индексации
     * @param value флаг остановки индексации
     */
    public static void setStopIndexing(boolean value) {
        stopIndexing = value;
        stopIndexingChanged.callWithBool(stopIndexing);
    }

    /**
     * @return флаг остановки индексации
     */
    public static boolean getStopIndexing() {
        return stopIndexing;
    }

    /**
     * Подписка на событие остановки индексации
     * @param listener обьект, который подписывается на событие
     */
    public static void listenForStopIndexing(CustomEventListener listener) {
        stopIndexingChanged.addListener(listener);
    }

    /**
     * Индексация всего сайта
     * @param site обьект с данными сайта
     * @param siteService ссылка на обьект для доступа к сервису сайтов
     */
    public BypassCalculator(Site site, SiteService siteService) {
        this.site = site;
        this.siteService = siteService;
        this.data = new BypassData();
        data.setPath("/");
        this.site.addPage(this.data.getPath());
    }

    /**
     * Индексация страницы на сайте (из цикла потоков)
     * @param site обьект с данными сайта
     * @param siteService ссылка на обьект для доступа к сервису сайтов
     * @param data данные результатов сканирования страницы
     */
    public BypassCalculator(Site site, SiteService siteService, BypassData data) {
        this.site = site;
        this.siteService = siteService;
        this.data = data;
    }

    /**
     * Индексация отдельной страницы на сайте
     * @param site обьект с данными сайта
     * @param siteService ссылка на обьект для доступа к сервису сайтов
     * @param pageUrl url страницы
     */
    public BypassCalculator(Site site, SiteService siteService, String pageUrl) {
        this.site = site;
        this.siteService = siteService;
        this.data = new BypassData();
        data.setPath(pageUrl.substring(site.getUrl().length()));
        if (data.getPath().isEmpty())
            data.setPath("/");
        this.site.addPage(this.data.getPath());
    }

    /**
     * Запуск процесса индексации страницы,
     * последующий поиск "дочерних" страниц и запуск потоков для их индексации
     * @return метод должен иметь возвращаемое значение, используем Void т.к. ничего считать здесь не нужно
     */
    @Override
    protected Void compute() {
        if(stopIndexing) {
            return null;
        }
        findPageChildren();
        List<BypassCalculator> taskList = new ArrayList<>();
        for (BypassData child : data.getChildren()) {
            BypassCalculator task = new BypassCalculator(site, siteService, child);
            task.fork();
            taskList.add(task);
        }
        for(BypassCalculator task : taskList) {
            task.join();
        }
        return null;
    }

    /**
     * Загрузка контента страницы, запуск индексации, и поиск "дочерних" страниц
     */
    private void findPageChildren() {
        try {
            Thread.sleep(500 + new Random().nextInt(2000));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        loadAndProcessPage();
        for (String link : links) {
            String linkWithoutQuery = removeQueryFromLink(link);
            if (!linkWithoutQuery.equals(site.getUrl()) && !site.getAllPages().contains(linkWithoutQuery)) {
                site.addPage(linkWithoutQuery);
                BypassData newData = new BypassData();
                newData.setPath(linkWithoutQuery);
                data.addChild(newData);
            }
        }
    }

    /**
     * Загрузка контента страницы и запуск индексации
     */
    public void loadAndProcessPage() {
        if (stopIndexing) {
            return;
        }
        String address = site.getUrl() + data.getPath();

        System.out.println("Загружаем код страницы " + address);
        Connection connection = loadPage(address);
        Document document;
        try {
            document = connection.get();
            int statusCode = connection.response().statusCode();
            data.setStatusCode(statusCode);
            if (statusCode < 400) { // not error
                findLinks(document);
                data.setContent(document.toString()); // set content before saving page
                int pageId = siteService.savePage(site, data);
                data.setContent(null); // unset content to free memory
                if (!stopIndexing && pageId > 0) {
                    System.out.println("Обрабатываем леммы для страницы " + address);
                    siteService.scanLemmas(document, pageId);
                    System.out.println("Обработка лемм для страницы " + address + " завершена");
                }
            } else {
                siteService.savePage(site, data);
            }
        } catch (IOException e) {
            siteService.setSiteError(site, e.getLocalizedMessage());
        }
    }

    /**
     * Загрузка контента страницы
     * @param address адрес страницы
     * @return обьект, который используем чтобы получить статус код и доступ к контенту
     */
    private Connection loadPage(String address) {
        return Jsoup.connect(address)
                .userAgent(userAgent)
                .referrer("http://www.google.com");
    }

    /**
     * Поиск тегов со ссылками на "дочерние" страницы на этой странице
     * @param document обьект с текстом вебстраницы
     */
    private void findLinks(Document document) {
        String[] linkTags = { "a", "link" };
        for (String tag : linkTags) {
            links.addAll(findLinksInElements(document.select(tag)));
        }
    }

    /**
     * Поиск ссылок на "дочерние" страницы на этой странице
     * @param elements элементы вебстраницы
     * @return список ссылок на "дочерние" страницы
     */
    private HashSet<String> findLinksInElements(Elements elements) {
        HashSet<String> links = new HashSet<>();
        elements.forEach((el) -> {
            String href = el.attr("href");
            if (href.lastIndexOf('.') != -1 && href.lastIndexOf('.') > href.lastIndexOf('/')) {
                String ext = href.substring(href.lastIndexOf('.'));
                if (!ext.equals(".html")) {
                    return;
                }
            }
            if (href.indexOf(site.getUrl()) == 0) {
                href = href.substring(site.getUrl().length());
                links.add(href);
            } else if (href.matches("\\/.*")) {
                links.add(href);
            }
        });
        return links;
    }

    /**
     * Вспомогательный метод, удаление параметров запроса и ссылки внутри страницы из ссылки
     * @param link текст ссылки
     * @return текст ссылки без параметров запроса
     */
    private String removeQueryFromLink(String link) {
        if (link.contains("?")) {
            link = link.substring(0, link.indexOf("?"));
        }
        if (link.contains("#")) {
            link = link.substring(0, link.indexOf("#"));
        }
        return link;
    }
}
