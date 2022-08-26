package main.services;

import main.model.Field;
import java.util.ArrayList;

public interface FieldService {
    /**
     * @return список полей
     */
    ArrayList<Field> getAllFields();
}
