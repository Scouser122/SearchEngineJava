package main.model;

import lombok.Data;

import javax.persistence.*;

/**
 * класс для работы с данными из одноименной таблицы в БД,
 * поисковый индекс
 */
@Data
@Entity
@Table(name = "`index`")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    // идентификатор страницы
    @Column(name = "page_id")
    private int pageId;
    // идентификатор леммы
    @Column(name = "lemma_id")
    private int lemmaId;
    @Column(name = "`rank`")
    // ранг леммы на этой странице
    private float rank;

    public Index() {

    }

    /**
     * @param pageId идентификатор страницы
     * @param lemmaId идентификатор леммы
     * @param rank ранг леммы на этой странице
     */
    public Index(int pageId, int lemmaId, float rank) {
        this.pageId = pageId;
        this.lemmaId = lemmaId;
        this.rank = rank;
    }
}
