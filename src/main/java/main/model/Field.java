package main.model;

import lombok.Data;

import javax.persistence.*;

/**
 * класс для работы с данными из одноименной таблицы в БД,
 * поля на страницах сайтов
 */
@Data
@Entity
@Table(name = "field")
public class Field {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    // имя поля
    private String name;
    // CSS-выражение, позволяющее получить содержимое конкретного поля
    private String selector;
    // релевантность (вес) поля от 0 до 1
    private float weight;
}
