package loader.searcher;


import loader.entity.Index;
import loader.entity.Lemma;
import loader.lemmatizer.Lemmatizer;
import loader.scanner.Website;
import org.hibernate.Session;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Searcher {

    private final Session session;
    private final Lemmatizer lemmatizer;
    private final int FREQUENCY_AMOUNT = 100;

    public Searcher(Session session) throws IOException {
        this.session = session;
        lemmatizer = new Lemmatizer();
    }

    public HashMap<Website, Double> search(String searchString){
        List<Lemma> lemmas = findLemmas(searchString);
        if(lemmas.isEmpty()){
            return new HashMap<>();
        }
        List<Integer> websiteIds = session.createQuery("from Index where lemmaId=:lemmaId", Index.class)
                .setParameter("lemmaId", lemmas.get(0).getId()).getResultList()
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

            try {
                dbLemma = session.createQuery("from Lemma where lemma=:lemma", Lemma.class)
                        .setParameter("lemma", lemma).getSingleResult();
            }
            catch (Exception exception){
                dbLemma = null;
            }

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
            index = session.createQuery("from Index where lemmaId=:lemmaId and pageId=:pageId", Index.class)
                    .setParameter("lemmaId", lemmaId)
                    .setParameter("pageId", pageId)
                    .getSingleResult();
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
                website = session.createQuery("from Website where id=:id", Website.class)
                        .setParameter("id", websiteId).getSingleResult();
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

            List<Index> indexList = session.createQuery("from Index where lemmaId in :lemmaIds and pageId=:pageId", Index.class)
                    .setParameter("lemmaIds", lemmas.stream().map(Lemma::getId).collect(Collectors.toList()))
                    .setParameter("pageId", website.getId())
                    .getResultList();

            double relevancy = indexList.stream().mapToDouble(Index::getRank).sum();

            websiteRelevancy.put(website, relevancy);
        });

        double max = websiteRelevancy.values().stream().max(Double::compareTo).get();

        websiteRelevancy.keySet().forEach(website ->
                websiteRelevancy.replace(website, websiteRelevancy.get(website)/max));


        return websiteRelevancy;
    }
}
