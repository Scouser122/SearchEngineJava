package main.services;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import main.model.ApplicationProps;
import main.model.Site;
import main.model.SiteRepository;
import main.model.SiteStatus;
import main.utils.bypass.BypassCalculator;
import main.utils.bypass.BypassData;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.Tuple;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

/**
 * Сервис для работы с данными из таблицы site
 */
@Service
@EnableConfigurationProperties(value = ApplicationProps.class)
public class SiteServiceImpl implements SiteService {
    // обьект для работы с таблицей в БД
    @Autowired
    private SiteRepository siteRepository;
    // ссылка на обьект для доступа к сервису вебстраниц
    @Autowired
    private PageService pageService;
    // ссылка на обьект для доступа к сервису лемм
    @Autowired
    private LemmaService lemmaService;
    // ссылка на обьект для доступа к сервису индексов
    @Autowired
    private IndexService indexService;
    // настройки приложения из application.yml
    @Autowired
    private ApplicationProps appProperties;
    // статус индексации поискового движка
    private volatile boolean indexing = false;
    // потоки индексации сайтов
    private final ArrayList<Thread> indexSiteThreads = new ArrayList<>();

    /**
     * метод инициализации сервиса,
     * вызывается после коннекта к БД до начала работы контроллеров,
     * задает параметр userAgent для поиска и обработчик события остановки индексации
     */
    @PostConstruct
    public void initialize() {
        BypassCalculator.setUserAgent(appProperties.getUserAgent());
        // когда общая индексация останавливается - запускаем потоки индексации отдельных сайтов,
        // которые находся в режиме ожидания
        BypassCalculator.listenForStopIndexing(value -> {
            if (!value) {
                this.indexWaitingPages();
            }
        });
        cleanUpSites();
    }

    public Site getSite(String siteUrl, boolean createIfNotExist) {
        if (siteUrl.charAt(siteUrl.length() - 1) == '/') {
            siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
        }
        Site site;
        synchronized (this) {
            site = siteRepository.findByUrl(siteUrl);
        }
        if (site == null && createIfNotExist) {
            site = new Site();
            site.setUrl(siteUrl);
            synchronized (this) {
                site = siteRepository.save(site);
            }
        }
        return site;
    }

    public void startIndexing() {
        setIndexing(true);
        BypassCalculator.setStopIndexing(false);
        new Thread(() -> {
            List<Map<String, String>> sites = appProperties.getSites();
            ArrayList<Thread> threads = new ArrayList<>();
            for(Map<String, String> siteData : sites) {
                Site site = getSite(siteData.get("url"), true);
                site.setName(siteData.get("name"));
                threads.add(scanSite(site));
            }
            for(Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("Индексация сайтов завершена");
            BypassCalculator.setStopIndexing(false);
            setIndexing(false);
        }).start();
    }

    public boolean getIndexing() {
        return indexing;
    }

    public void stopIndexing() {
        BypassCalculator.setStopIndexing(true);
    }

    public boolean indexPage(String url) {
        List<Map<String, String>> sites = appProperties.getSites();
        Site site = null;
        for(Map<String, String> siteData : sites) {
            String siteUrl = siteData.get("url");
            if (url.contains(siteUrl)) {
                site = getSite(siteData.get("url"), true);
                site.setName(siteData.get("name"));
                break;
            }
        }
        if (site == null) {
            return false;
        }
        Site finalSite = site;
        Thread indexThread = new Thread(() -> {
            BypassCalculator calculator = new BypassCalculator(finalSite, this, url);
            calculator.loadAndProcessPage();
            if (finalSite.getStatus() == SiteStatus.INDEXING) {
                updateSiteStatus(finalSite, SiteStatus.INDEXED);
            }
            System.out.println("Индексация страницы " + url + " завершена");
        });
        if (BypassCalculator.getStopIndexing()) {
            indexSiteThreads.add(indexThread);
        } else {
            indexThread.start();
        }
        return true;
    }

    public JsonObject getSitesData() {
        JsonObject root = new JsonObject();
        JsonObject total = new JsonObject();
        total.put("sites", getNumSites());
        total.put("pages", pageService.getNumPages());
        total.put("lemmas", lemmaService.getNumLemmas());
        total.put("isIndexing", indexing);
        root.put("total", total);
        JsonArray detailed = new JsonArray();
        List<Map<String, String>> siteDatas = appProperties.getSites();
        HashSet<String> urls = new HashSet<>();
        for(Map<String, String> siteData : siteDatas) {
            urls.add(siteData.get("url"));
        }
        Iterable<Tuple> stats = siteRepository.getSitesStats();
        HashMap<Integer, Tuple> siteStats = new HashMap<>();
        for (Tuple stat : stats) {
            siteStats.put(stat.get(0, Integer.class), stat);
        }
        Iterable<Site> sites = siteRepository.findByUrlIn(urls);
        for (Site site : sites) {
            JsonObject siteInfo = new JsonObject();
            siteInfo.put("url", site.getUrl());
            siteInfo.put("name", site.getName());
            siteInfo.put("status", site.getStatus().toString());
            long statusTime = site.getStatusTime().toInstant(ZoneOffset.UTC).toEpochMilli();
            siteInfo.put("statusTime", statusTime);
            if (site.getLastError() != null) {
                siteInfo.put("error", site.getLastError());
            }
            Tuple siteStat = siteStats.get(site.getId());
            siteInfo.put("lemmas", siteStat != null ? siteStat.get(1, BigInteger.class).intValue() : 0);
            siteInfo.put("pages", siteStat != null ? siteStat.get(2, BigInteger.class).intValue() : 0);
            detailed.add(siteInfo);
        }
        root.put("detailed", detailed);
        return root;
    }

    public int savePage(Site site, BypassData data) {
        updateSiteStatus(site, SiteStatus.INDEXING);
        return pageService.savePage(site, data);
    }

    public void scanLemmas(Document document, int pageId) {
        lemmaService.processDocumentInPage(document, pageId);
    }

    public void setSiteError(Site site, String error) {
        site.setLastError(error);
        site.setStatus(SiteStatus.FAILED);
        synchronized (this) {
            siteRepository.save(site);
        }
    }

    public void deleteSite(int siteId) {
        synchronized (this) {
            siteRepository.deleteSite(siteId);
        }
    }

    /**
     * @param value статус индексации, true если индексация в данный момент в процессе
     */
    private void setIndexing(boolean value) {
        indexing = value;
    }

    /**
     * Очистка данных по вебсайту, запускается перед индексацией
     * @param site обьект сайта
     */
    private void clearSiteData(Site site) {
        Set<Integer> pageIds = pageService.getPageIdsForSite(site.getId());
        Set<Integer> lemmaIds = indexService.getLemmaIdsForPageIds(pageIds);
        lemmaService.cleanUpLemmas(lemmaIds);
        indexService.deleteIndexesForPages(pageIds);
        pageService.deletePagesForSite(site.getId());
    }

    /**
     * Создание потока индексации сайта
     * @param site обьект сайта
     * @return созданный поток
     */
    private Thread scanSite(Site site) {
        clearSiteData(site);
        updateSiteStatus(site, SiteStatus.INDEXING);
        Thread thread = new Thread(() -> {
            long start = System.currentTimeMillis();
            ForkJoinPool jp = new ForkJoinPool();
            jp.invoke(new BypassCalculator(site, this));
            System.out.println("Добавление страниц в базу для сайта " + site.getUrl() + " завершено." +
                    " Общее время: " + ((System.currentTimeMillis() - start) / 1000) + " sec.");
            if (site.getStatus() == SiteStatus.INDEXING) {
                updateSiteStatus(site, SiteStatus.INDEXED);
            }
        });
        thread.start();
        return thread;
    }

    /**
     * Обновление статуса индексации сайта в таблице
     * @param site обьект сайта
     * @param status статус индексации
     */
    private void updateSiteStatus(Site site, SiteStatus status) {
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());
        synchronized (this) {
            siteRepository.save(site);
        }
    }

    /**
     * @return количество проиндексированных сайтов
     */
    private long getNumSites() {
        long result;
        synchronized (this) {
            result = siteRepository.getNumSites();
        }
        return result;
    }

    /**
     * Запуск потоков, ожидающих в очереди на индексацию
     */
    private void indexWaitingPages() {
        if (indexSiteThreads.isEmpty()) {
            return;
        }
        for(Thread thread : indexSiteThreads) {
            thread.start();
        }
        indexSiteThreads.clear();
    }

    /**
     * Удаление из таблицы сайтов тех,
     * которые не были найдены в application.yml
     */
    private void cleanUpSites() {
        List<Map<String, String>> sitesInProps = appProperties.getSites();
        Iterable<Site> savedSites = siteRepository.findAll();
        for (Site site : savedSites) {
            boolean foundSite = false;
            for (Map<String, String> siteData : sitesInProps) {
                String url = siteData.get("url");
                if (url != null && url.equals(site.getUrl())) {
                    foundSite = true;
                }
            }
            if (!foundSite) {
                clearSiteData(site);
                deleteSite(site.getId());
            }
        }
    }
}
