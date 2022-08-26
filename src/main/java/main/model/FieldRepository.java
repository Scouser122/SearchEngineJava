package main.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * Интерфейс для работы с таблицей field в БД
 */
@Repository
public interface FieldRepository extends CrudRepository<Field, Integer> {
    /**
     * Получение списка полей по имени
     * @param names список имён полей
     * @return список полей
     */
    Iterable<Field> findByNameIn(Set<String> names);
}
