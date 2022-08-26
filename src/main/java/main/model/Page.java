package main.model;

import com.github.cliftonlabs.json_simple.JsonObject;
import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;

/**
 * класс для работы с данными из одноименной таблицы в БД,
 * данные по вебстранице
 */
@Data
@Entity
@Table(name = "page")
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    // адрес страницы от корня сайта
    @Column(name="path", columnDefinition="TEXT")
    private String path;
    // код ответа, полученный при запросе страницы
    private int code;
    // контент страницы (HTML-код)
    @Column(name="content", columnDefinition="MEDIUMTEXT")
    @Basic(fetch = FetchType.LAZY)
    private String content;
    // ссылка на запись из таблицы site
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private Site site;
    /**
     * релевантность страницы
     * <p> вспомогательный параметр, используется при расчетах в процессе поиска текста,
     * <p> не сохраняется в таблице page
     */
    @Transient
    private float relevance;

    /**
     * @param value релевантность страницы
     */
    public void setRelevance(float value) { relevance = value; }

    /**
     * @return релевантность страницы
     */
    public float getRelevance() { return relevance; }

    @Transient
    public String toString() {
        return "id=" + getId() + ", " +
                "siteUrl=" + getSite().getUrl() + ", " +
                "path=" + getId() + ", " +
                "code=" + code + ", " +
                "content:\n" + content;
    }
}
