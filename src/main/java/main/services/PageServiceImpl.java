package main.services;


import main.model.Page;
import main.model.PageRepository;
import main.model.Site;
import main.utils.bypass.BypassData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Сервис для работы с данными из таблицы page
 */
@Service
public class PageServiceImpl implements PageService {
    // обьект для работы с таблицей в БД
    @Autowired
    private PageRepository pageRepository;

    public HashMap<Integer, Page> getPages(Set<Integer> pageIds) {
        Iterable<Page> iterable = pageRepository.findByIdIn(pageIds);
        HashMap<Integer, Page> result = new HashMap<>();
        for(Page page : iterable) {
            result.put(page.getId(), page);
        }
        return result;
    }

    public int savePage(Site site, BypassData data) {
        Page page;
        synchronized (this) {
            page = pageRepository.findByPathAndSiteId(data.getPath(), site.getId());
        }
        if (page == null) {
            page = new Page();
            page.setPath(data.getPath());
            page.setSite(site);
            System.out.println("Добавляем страницу " + site.getUrl() + page.getPath() + " в базу данных");
        } else {
            System.out.println("Обновляем страницу " + site.getUrl() + page.getPath() + " в базе данных");
        }
        page.setCode(data.getStatusCode());
        page.setContent(data.getContent());
        synchronized (this) {
            try {
                pageRepository.save(page);
            } catch (Exception ex) {
                System.err.println("Ошибка сохранения страницы:" + ex + ", pageInfo: " + page);
                return -1;
            }
        }
        return page.getId();
    }

    public long getNumPages() {
        return pageRepository.getNumPages();
    }

    public Set<Integer> getPageIdsForSite(int siteId) {
        return pageRepository.getPageIdsForSite(siteId);
    }

    public void deletePagesForSite(int siteId) {
        synchronized (this) {
            pageRepository.deleteBySiteId(siteId);
        }
    }

    public ArrayList<Tuple> searchPagesForIds(Set<Integer> pageIds, Integer offset, Integer limit) {
        if (pageIds.isEmpty()) {
            return new ArrayList<>();
        }
        return pageRepository.searchPagesForIds(pageIds, offset, limit);
    }
}
