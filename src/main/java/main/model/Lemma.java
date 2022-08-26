package main.model;

import lombok.Data;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * класс для работы с данными из одноименной таблицы в БД,
 * леммы, встречающиеся в текстах
 */
@Data
@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    // нормальная форма слова
    private String lemma;
    // количество страниц, на которых слово встречается хотя бы один раз
    private int frequency;

    /**
     * вспомогательный параметр для расчета значения rank для индекса
     * не является полем таблицы lemma
     */
    @Transient
    private float rank;

    public Lemma() {

    }

    /**
     * @param name нормальная форма слова
     * @param frequency количество страниц, на которых слово встречается хотя бы один раз
     */
    public Lemma(String name, int frequency) {
        lemma = name;
        this.frequency = frequency;
    }

    /**
     * Вспомогательный метод для расчета значения rank для индекса
     * @param fieldsMap список полей и весов для них на конкретной странице
     */
    @Transient
    public void countRank(HashMap<Field, Integer> fieldsMap) {
        rank = 0.0f;
        for(Map.Entry<Field, Integer> entry : fieldsMap.entrySet()) {
            rank += entry.getKey().getWeight() + entry.getValue();
        }
    }
}
