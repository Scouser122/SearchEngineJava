package main.utils.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для оповещения о событиях
 */
public class CustomEvent {
    // список "слушателей" подписанных на события
    private final List<CustomEventListener> listeners = new ArrayList<>();

    /**
     * Добавление подписчика
     * @param listener обьект, который подписывается на "прослушивание" события
     */
    public void addListener(CustomEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Вызов события с параметром типа boolean
     * @param value значение параметра
     */
    public void callWithBool(boolean value) {
        for(CustomEventListener listener : listeners) {
            listener.eventCalledWithBool(value);
        }
    }
}
