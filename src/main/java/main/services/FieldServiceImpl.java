package main.services;

import main.model.ApplicationProps;
import main.model.Field;
import main.model.FieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Сервис для работы с данными из таблицы field
 */
@Service
@EnableConfigurationProperties(value = ApplicationProps.class)
public class FieldServiceImpl implements FieldService {
    // обьект для работа с таблицей в БД
    @Autowired
    private FieldRepository fieldRepository;
    // настройки приложения из application.yml
    @Autowired
    private ApplicationProps appProperties;
    // список полей
    private final ArrayList<Field> fields = new ArrayList<>();

    /**
     * метод инициализации сервиса, вызывается после коннекта к БД до начала работы контроллеров
     * заполняет или обновляет данные в таблице field,
     * данные получает из конфиг файла application.yml
     */
    @PostConstruct
    public void initRepository() {
        HashMap<String, Float> tags = getTagsForFields();
        HashSet<Field> fieldsToUpdate = new HashSet<>();
        Iterable<Field> fieldIterable = fieldRepository.findByNameIn(tags.keySet());
        for(Field field : fieldIterable) {
            float weight = tags.get(field.getName());
            if (field.getWeight() != weight) {
                field.setWeight(weight);
                fieldsToUpdate.add(field);
            }
            tags.remove(field.getName());
            fields.add(field);
        }
        tags.forEach((tag, weight) -> {
            Field field = new Field();
            field.setName(tag);
            field.setSelector(tag);
            field.setWeight(weight);
            fields.add(field);
            fieldsToUpdate.add(field);
        });
        if (!fieldsToUpdate.isEmpty()) {
            fieldRepository.saveAll(fieldsToUpdate);
        }
    }

    public ArrayList<Field> getAllFields() {
        return fields;
    }

    /**
     * @return список имен полей с весами из конфиг файла application.yml
     */
    private HashMap<String, Float> getTagsForFields() {
        List<Map<String, String>> propFields = appProperties.getFields();
        HashMap<String, Float> usedTags = new HashMap<>();
        for(Map<String, String> fieldsData : propFields) {
            usedTags.put(fieldsData.get("name"), Float.parseFloat(fieldsData.get("weight")));
        }
        return usedTags;
    }
}
