package main.utils;

/**
 * Класс для замеров времени работы кода и испольуземой памяти
 */
public class TimeCounter {
    // время начала работы кода
    private long startTime = 0;
    // занятая приложением память на начало работы кода
    private long startMemory = 0;
    // текстовое описание действия, выполняемого кодом
    private final String action;

    /**
     * @param actionName текстовое описание действия, выполняемого кодом
     */
    public TimeCounter(String actionName) {
        this.action = actionName;
        reset();
    }

    /**
     * Сброс таймера начала работы кода
     */
    public void reset() {
        startTime = System.currentTimeMillis();
        startMemory = getMemoryUsage();
    }

    /**
     * Вывод результатов работы таймера в лог
     */
    public void printStats() {
        printStats(-1);
    }

    /**
     * Вывод результатов работы таймера в лог
     * @param step шаг проверки результатов, если -1 - то не выводим это значение
     */
    public void printStats(int step) {
        StringBuilder builder = new StringBuilder();
        builder.append("Затраченное время на ").append(action).append(" ");
        if (step != -1) {
            builder.append("(шаг ").append(step).append(")");
        }
        builder.append(": ").append(System.currentTimeMillis() - startTime).append("ms");
        builder.append(", память: ").append(getSizeAsString(getMemoryUsage() - startMemory));
        builder.append(", общая память: ").append(getSizeAsString(getMemoryUsage()));
        System.out.println(builder);
    }

    private static String getSizeAsString(long size) {
        StringBuilder builder = new StringBuilder();
        String[] prefixes = { "", "K", "M", "G", "T" };
        int index = 0;
        long remain = 0;
        if (size < 0) {
            builder.append("-");
        }
        size = Math.abs(size);
        while (size > 1024) {
            remain = size % 1024;
            size /= 1024;
            if(index < prefixes.length - 1) {
                index++;
            } else {
                break;
            }
        }
        builder.append(size);
        if (remain > 0) {
            double dec = ((double) remain) / 1024.0;
            builder.append(".").append(((int) (dec * 100)));
        }
        builder.append(prefixes[index]).append("B");
        return builder.toString();
    }

    private static long getMemoryUsage() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }
}
