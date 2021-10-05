package loader.searcher;


import loader.entity.*;
import loader.lemmatizer.Lemmatizer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class Searcher {

    private final Lemmatizer lemmatizer;
    private final int FREQUENCY_AMOUNT = 100;

    private final IndexRepository indexRepository;
    private final WebsiteRepository websiteRepository;
    private final LemmaRepository lemmaRepository;

    public Searcher(IndexRepository indexRepository,
                    WebsiteRepository websiteRepository,
                    LemmaRepository lemmaRepository) throws IOException {
        this.indexRepository = indexRepository;
        this.websiteRepository = websiteRepository;
        this.lemmaRepository = lemmaRepository;
        lemmatizer = new Lemmatizer();
    }

    public HashMap<Website, Double> search(String searchString){
        List<Lemma> lemmas = findLemmas(searchString);
        if(lemmas.isEmpty()){
            return new HashMap<>();
        }
        List<Integer> websiteIds = indexRepository.findAllByLemmaId(lemmas.get(0).getId())
                .stream().map(Index::getPageId).collect(Collectors.toList());

        HashMap<Integer, Boolean> suitabilityWebsites = new HashMap<>();
        websiteIds.forEach(website -> suitabilityWebsites.put(website, true));
        websiteIds.clear();

        List<Website> websites = selectWebsites(lemmas, suitabilityWebsites);
        if(websites.isEmpty()){
            return new HashMap<>();
        }
        HashMap<Website, Double> websiteRelevancy = calculateRelevancy(websites, lemmas);

        return websiteRelevancy;
    }

    private List<Lemma> findLemmas(String searchString){
        List<Lemma> lemmaList = new ArrayList<>();
        HashMap<String, Integer> lemmas = lemmatizer.analyzeText(searchString);

        lemmas.keySet().forEach(lemma -> {
            Lemma dbLemma;

            dbLemma = lemmaRepository.findLemmaByLemma(lemma);

            if(dbLemma != null){
                lemmaList.add(dbLemma);
            }

        });

        return lemmaList.stream()
                .filter(lemma -> lemma.getFrequency() < FREQUENCY_AMOUNT)
                .sorted(Comparator.comparing(Lemma::getFrequency).reversed())
                .collect(Collectors.toList());
    }

    private List<Website> selectWebsites(List <Lemma> lemmas,
                                         HashMap<Integer, Boolean> suitabilityWebsites){

        lemmas.forEach(lemma -> {
            suitabilityWebsites.keySet().forEach(websiteId ->
                    findSuitability(suitabilityWebsites, lemma.getId(), websiteId)
            );
            suitabilityWebsites.values().removeIf(Predicate.isEqual(false));
        });

        return getWebsites(suitabilityWebsites);
    }

    void findSuitability(HashMap<Integer, Boolean> suitabilityWebsites, int lemmaId, int pageId){
        Index index;

        try {
            index = indexRepository.findByLemmaIdAndPageId(lemmaId, pageId);
        }
        catch (Exception exception){
            index = null;
        }

        if(index == null){
            suitabilityWebsites.replace(pageId, false);
        }
    }

    List<Website> getWebsites(HashMap<Integer, Boolean> suitabilityWebsites){
        List<Website> websites = new ArrayList<>();

        suitabilityWebsites.keySet().forEach(websiteId -> {
            Website website;

            try {
                website = websiteRepository.findById(websiteId).get();
            }
            catch (Exception exception){
                website = null;
            }

            if(website != null){
                websites.add(website);
            }
        });

        return websites;
    }

    HashMap<Website, Double> calculateRelevancy(List<Website> websites, List<Lemma> lemmas){
        HashMap<Website, Double> websiteRelevancy = new HashMap<>();

        websites.forEach(website -> {

            List<Index> indexList = indexRepository.findAllByLemmaIdInAndPageId(
                    lemmas.stream().map(Lemma::getId).collect(Collectors.toList()), website.getId()
            );

            double relevancy = indexList.stream().mapToDouble(Index::getRank).sum();

            websiteRelevancy.put(website, relevancy);
        });

        double max = websiteRelevancy.values().stream().max(Double::compareTo).get();

        websiteRelevancy.keySet().forEach(website ->
                websiteRelevancy.replace(website, websiteRelevancy.get(website)/max));


        return websiteRelevancy;
    }
}
