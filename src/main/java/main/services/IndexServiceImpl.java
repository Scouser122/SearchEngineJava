package main.services;

import main.model.Index;
import main.model.IndexRepository;
import main.model.Lemma;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import javax.persistence.Tuple;
import java.util.*;

/**
 * Сервис для работы с данными из таблицы index
 */
@Service
public class IndexServiceImpl implements IndexService {
    // обьект для работа с таблицей в БД
    @Autowired
    private IndexRepository indexRepository;

    public void updateIndexes(ArrayList<Lemma> lemmas, int pageId) {
        HashMap<Integer, Lemma> lemmasMap = new HashMap<>();
        lemmas.forEach(l -> lemmasMap.put(l.getId(), l));
        Iterable<Index> indexes = indexRepository.findByPageIdAndLemmaIdIn(pageId, lemmasMap.keySet());
        ArrayList<Index> updatedIndexes = new ArrayList<>();
        for(Index index : indexes) {
            Lemma lemma = lemmasMap.get(index.getLemmaId());
            if (lemma != null) {
                index.setRank(lemma.getRank());
                lemmasMap.remove(index.getLemmaId());
                updatedIndexes.add(index);
            }
        }
        for(Map.Entry<Integer, Lemma> entry : lemmasMap.entrySet()) {
            Index index = new Index(pageId, entry.getKey(), entry.getValue().getRank());
            updatedIndexes.add(index);
        }
        synchronized (this) {
            indexRepository.saveAll(updatedIndexes);
        }
    }

    public void alignLemmasRank(HashSet<Integer> lemmaIds, int pageId) {
        Iterable<Index> indexes = indexRepository.findByPageIdAndLemmaIdIn(pageId, lemmaIds);
        float maxRank = 0.0f;
        for(Index index : indexes) {
            maxRank = Math.max(index.getRank(), maxRank);
        }
        for(Index index : indexes) {
            index.setRank(maxRank);
        }
        synchronized (this) {
            indexRepository.saveAll(indexes);
        }
    }

    public void deleteIndexesForPages(Set<Integer> pageIds) {
        if (!pageIds.isEmpty()) {
            synchronized (this) {
                indexRepository.deleteByPageIds(pageIds);
            }
        }
    }
    public ArrayList<Tuple> getLemmaIdsAndPageIdsSortedByFrequency(Set<String> lemmaNames) {
        if (lemmaNames.isEmpty()) {
            return new ArrayList<>();
        }
        return indexRepository.getLemmaIdsAndPageIdsSortedByFrequency(lemmaNames);
    }
}
