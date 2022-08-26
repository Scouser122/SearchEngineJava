package main.utils.event;

/**
 * Интерфейс для слушателя события из обьекта типа CustomEvent
 */
public interface CustomEventListener {
    /**
     * метод вызывается когда обьект CustomEvent вызывает свой метод callWithBool
     * @param value значение параметра
     */
    void eventCalledWithBool(boolean value);
}
