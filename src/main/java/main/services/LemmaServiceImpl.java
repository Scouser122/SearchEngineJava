package main.services;

import main.model.Field;
import main.model.Lemma;
import main.model.LemmaRepository;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Сервис для работы с данными из таблицы lemma
 */
@Service
public class LemmaServiceImpl implements LemmaService {
    // обьект для работы с таблицей в БД
    @Autowired
    private LemmaRepository lemmaRepository;
    // ссылка на обьект для доступа к сервису полей
    @Autowired
    private FieldService fieldService;
    // ссылка на обьект для доступа к сервису индексов
    @Autowired
    private IndexService indexService;
    // обьект из библиотеки лемматизации для получения лемм (исходных форм слов) на русском языке
    private LuceneMorphology russianMorph;
    // обьект из библиотеки лемматизации для получения лемм (исходных форм слов) на английском языке
    private LuceneMorphology englishMorph;
    // регулярное выражение для разбивки текста на слова
    private static final String TEXT_SPLIT_REGEX = "( \\- )|[,.;:\"! ]";
    // регулярное выражения для игнорирования служебных частей речи при получении лемм для слов
    private static final String IGNORE_SERVICE_PARTS_REGEX = ".*(ПРЕДЛ|МЕЖД|СОЮЗ|МС|ЧАСТ|PREP|PART|CONJ|ARTICLE).*";

    public void processDocumentInPage(Document document, int pageId) {
        HashMap<String, HashMap<Field, Integer>> words = scanWords(document);
        updateLemmas(words, pageId);
    }

    public ArrayList<Lemma> findLemmasForText(String text) {
        ArrayList<Lemma> result = new ArrayList<>();
        HashSet<String> lemmaStrings = findLemmaStringsInText(text);
        Iterable<Lemma> lemmaIterable = lemmaRepository.findByLemmaInOrderByFrequencyAsc(lemmaStrings);
        for (Lemma lemma : lemmaIterable) {
            result.add(lemma);
        }
        return result;
    }

    public HashSet<String> findLemmaStringsInText(String text) {
        HashSet<String> lemmaStrings = new HashSet<>();
        String[] wordsArr = text.split(TEXT_SPLIT_REGEX);
        for(String word : wordsArr) {
            if (word.length() == 0) {
                continue;
            }
            List<String> wordBaseForms = getWordBaseForms(word);
            for (String baseForm : wordBaseForms) {
                if(baseForm.length() == 0) {
                    continue;
                }
                lemmaStrings.add(baseForm);
            }
        }
        return lemmaStrings;
    }

    public long getNumLemmas() {
        return lemmaRepository.getNumLemmas();
    }

    public void cleanUpLemmas(Set<Integer> lemmaIds) {
        synchronized (this) {
            Iterable<Lemma> lemmas = lemmaRepository.findByIdIn(lemmaIds);
            Set<Integer> lemmasToDelete = new HashSet<>();
            ArrayList<Lemma> lemmasToUpdate = new ArrayList<>();
            for (Lemma lemma : lemmas) {
                if (lemma.getFrequency() > 1) {
                    lemma.setFrequency(lemma.getFrequency() - 1);
                    lemmasToUpdate.add(lemma);
                } else {
                    lemmasToDelete.add(lemma.getId());
                }
            }
            if (!lemmasToUpdate.isEmpty()) {
                lemmaRepository.saveAll(lemmasToUpdate);
            }
            if (!lemmasToDelete.isEmpty()) {
                lemmaRepository.deleteLemmasByIds(lemmasToDelete);
            }
        }
    }

    /**
     * Поиск слов на вебстранице
     * @param document обьект с текстом вебстраницы
     * @return список слов с количеством их появления в полях html документа
     */
    private HashMap<String, HashMap<Field, Integer> > scanWords(Document document) {
        HashMap<String, HashMap<Field, Integer> > words = new HashMap<>();
        ArrayList<Field> fields = fieldService.getAllFields();
        for (Field field : fields) {
            Elements elements = document.select(field.getName());
            for(Element el : elements) {
                scanLemmas(el.text().toLowerCase(), words, field);
            }
        }
        return words;
    }

    /**
     * Поиск слов в элементах вебстраницы
     * @param text текст элемента вебстраницы
     * @param words список слов с количеством их появления в полях html документа
     * @param field ссылка на обьект поля
     */
    private void scanLemmas(String text, HashMap<String, HashMap<Field, Integer> > words, Field field) {
        String[] wordsArr = text.split(TEXT_SPLIT_REGEX);
        for(String word : wordsArr) {
            if(word.length() == 0) {
                continue;
            }
            List<String> wordBaseForms = getWordBaseForms(word);
            for (String baseForm : wordBaseForms) {
                if (words.containsKey(baseForm)) {
                    HashMap<Field, Integer> wordMap = words.get(baseForm);
                    int count = wordMap.getOrDefault(field, 0);
                    wordMap.put(field, count + 1);
                } else {
                    HashMap<Field, Integer> wordMap = new HashMap<>();
                    wordMap.put(field, 1);
                    words.put(baseForm, wordMap);
                }
            }
        }
    }

    /**
     * @return обьект из библиотеки лемматизации для получения лемм (исходных форм слов) на русском языке
     */
    private LuceneMorphology getRussianMorph() {
        if(russianMorph == null) {
            try {
                russianMorph = new RussianLuceneMorphology();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return russianMorph;
    }

    /**
     * @return обьект из библиотеки лемматизации для получения лемм (исходных форм слов) на английском языке
     */
    private LuceneMorphology getEnglishMorph() {
        if(englishMorph == null) {
            try {
                englishMorph = new EnglishLuceneMorphology();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return englishMorph;
    }

    /**
     * Получение информации по слову (для определения части речи)
     * @param word слово
     * @return информация по слову
     */
    private List<String> getMorphInfo(String word) {
        List<String> morphInfo = null;
        try {
            morphInfo = getRussianMorph().getMorphInfo(word);
        } catch (WrongCharaterException ex) {
            try {
                morphInfo = getEnglishMorph().getMorphInfo(word);
            } catch (WrongCharaterException ignored) {
            }
        }
        return morphInfo;
    }

    /**
     * @param word слово
     * @return список возможных исходных форм слова
     */
    private List<String> getWordBaseForms(String word) {
        ArrayList<String> result = new ArrayList<>();
        List<String> morphInfoList = getMorphInfo(word);
        if (morphInfoList == null) {
            return result;
        }
        for (String morphInfo : morphInfoList) {
            int delIndex = morphInfo.indexOf('|');
            if (delIndex == -1 || morphInfo.matches(IGNORE_SERVICE_PARTS_REGEX)) {
                continue;
            }
            String morphInfoWord = morphInfo.substring(0, delIndex);
            if (morphInfoWord.isEmpty()) {
                continue;
            }
            List<String> normalForms = null;
            try {
                normalForms = getRussianMorph().getNormalForms(morphInfoWord);
            } catch (WrongCharaterException ex) {
                try {
                    normalForms = getEnglishMorph().getNormalForms(morphInfoWord);
                } catch (WrongCharaterException ignored) {
                }
            } catch (Exception ex) {
                System.err.println("Ошибка поиска нормальных форм слова: " + ex + ", слово: " + morphInfoWord);
            }
            if (normalForms != null) {
                result.addAll(normalForms);
            }
        }
        return result;
    }

    /**
     * Обновление данных по леммам в таблице
     * @param words список слов с количеством их появления в полях html документа
     * @param pageId идентификатор страницы
     */
    private void updateLemmas(HashMap<String, HashMap<Field, Integer> > words, int pageId) {
        ArrayList<Lemma> lemmas = new ArrayList<>();
        ArrayList<Lemma> notFoundLemmas = new ArrayList<>();
        synchronized (this) {
            Iterable<Lemma> lemmaIterable = lemmaRepository.findByLemmaIn(words.keySet());
            for (Lemma lemma : lemmaIterable) {
                lemma.setFrequency(lemma.getFrequency() + 1);
                try {
                    lemma.countRank(words.get(lemma.getLemma()));
                } catch (NullPointerException ex) {
                    notFoundLemmas.add(lemma);
                }
                words.remove(lemma.getLemma());
                lemmas.add(lemma);
            }
            for (Map.Entry<String, HashMap<Field, Integer>> word : words.entrySet()) {
                Lemma lemma = new Lemma(word.getKey(), 1);
                lemma.countRank(word.getValue());
                lemmas.add(lemma);
            }
            lemmaRepository.saveAll(lemmas);
        }
        indexService.updateIndexes(lemmas, pageId);
        alignLemmas(notFoundLemmas, pageId);
    }

    /**
     * Установка одинаковых значений 'rank' для идентичных лемм,
     * которые записываются в разные строки таблицы lemma,
     * например слова 'темный' и 'тёмный' будут одинаковыми при запросах в mysql
     * @param lemmas список "одинаковых для БД" лемм
     * @param pageId идентификатор страницы
     */
    private void alignLemmas(ArrayList<Lemma> lemmas, int pageId) {
        for (Lemma lemma : lemmas) {
            HashSet<String> findSet = new HashSet<>();
            findSet.add(lemma.getLemma());
            HashSet<Integer> lemmaIds = new HashSet<>();
            Iterable<Lemma> lemmaIterable = lemmaRepository.findByLemmaIn(findSet);
            for (Lemma foundLemma : lemmaIterable) {
                lemmaIds.add(foundLemma.getId());
            }
            indexService.alignLemmasRank(lemmaIds, pageId);
        }
    }
}
