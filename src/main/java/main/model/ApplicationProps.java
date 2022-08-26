package main.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Класс для создания обьекта с настройками,
 * заданными в конфигурационном файле application.yml,
 * для удобства получения доступа к настройкам
 */
@Data
@ConfigurationProperties
public class ApplicationProps {
    private List<Map<String, String>> sites;
    private List<Map<String, String>> fields;
    private String userAgent;
}
