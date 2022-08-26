package main.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * класс для работы с данными из одноименной таблицы в БД,
 * данные по сайту
 */
@Data
@Entity
@Table(name = "site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * текущий статус полной индексации сайта,
     * отражающий готовность поискового движка осуществлять поиск по сайту
     */
    @Enumerated(EnumType.STRING)
    private SiteStatus status = SiteStatus.INDEXING;
    // дата и время статуса
    @Column(name = "status_time")
    private LocalDateTime statusTime;
    // текст ошибки индексации
    @Column(name = "last_error", columnDefinition="TEXT")
    private String lastError;
    // адрес главной страницы сайта
    @Column(unique = true)
    private String url;
    // имя сайта
    @Column
    private String name;

    public Site() {
        statusTime = LocalDateTime.now();
    }

    /**
     * вспомогательный параметр для индексации,
     * <p> хранит список адресов страниц сайта
     * <p> не сохраняется в БД
     */
    @Transient
    private HashSet<String> allPages = new HashSet<>();

    /**
     * @return список адресов страниц сайта
     */
    @Transient
    public HashSet<String> getAllPages() {
        return allPages;
    }

    /**
     * Добавление адреса страницы в список адресов страниц сайта
     * @param pagePath адрес страницы
     */
    @Transient
    public void addPage(String pagePath) {
        allPages.add(pagePath);
    }
}
