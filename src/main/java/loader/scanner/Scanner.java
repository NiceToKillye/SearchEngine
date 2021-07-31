package loader.scanner;

import loader.entity.Field;
import loader.entity.Index;
import loader.entity.Lemma;
import loader.lemmatizer.Lemmatizer;
import org.hibernate.Session;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

public class Scanner {

    private final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36";
    private final String REFERER = "http://www.google.com";

    private Session session;
    static Set<String> uniqueWebsiteLinks = ConcurrentHashMap.newKeySet();

    public Scanner(Session session){
        this.session = session;
    }

    public Scanner(){

    }

    public void scan(String websiteLink) throws IOException {
        uniqueWebsiteLinks.add(websiteLink);
        List<String> websiteMap = new ForkJoinPool().invoke(new ScannerTask(websiteLink));

        List<Website> websiteList = getWebsitesList(websiteMap);
        fillDB(websiteList);
        uniqueWebsiteLinks.clear();
        indexWebsite(websiteList);
    }

    private List<Website> getWebsitesList(List<String> websiteMap){
        List<Website> websiteList = new ArrayList<>();
        websiteMap.forEach(website ->{
            Website website1 = new Website();
            website1.setPath(website);
            try {
                Document document = Jsoup
                        .connect(website)
                        .userAgent(USER_AGENT)
                        .referrer(REFERER)
                        .maxBodySize(0)
                        .get();
                website1.setContent(document.html());

                Connection.Response response = Jsoup
                        .connect(website)
                        .userAgent(USER_AGENT)
                        .timeout(1000)
                        .execute();
                website1.setCode(response.statusCode());
            } catch (Exception e) {
                System.out.println("LINK: " + website + " " + website1.getCode());
                website1.setCode(504);
                e.printStackTrace();
            }

            websiteList.add(website1);
        });

        return websiteList;
    }

    private void fillDB(List<Website> websiteList){
        try {
            session.beginTransaction();
            websiteList.forEach(website -> session.saveOrUpdate(website));
            session.getTransaction().commit();
        }
        catch (Exception exception){
            session.getTransaction().rollback();
            System.out.println("WARNING: The transaction was not completed!");
        }
    }

    private void indexWebsite(List<Website> websiteList) throws IOException {
        Lemmatizer lemmatizer = new Lemmatizer();

        List<Field> fields = session.createQuery("from Field", Field.class).getResultList();

        websiteList.forEach(website -> {
            if(website.getCode() == 200) {
                Document document = Jsoup.parse(website.getContent());
                HashMap<String, Float> lemmas = new HashMap<>();
                findLemmas(document, lemmatizer, fields, lemmas);
                HashMap<Lemma, Float> lemmasFromDB = getDBLemmas(lemmas);
                saveIndex(lemmasFromDB, website);
            }
        });
    }

    private void findLemmas(Document document,
                            Lemmatizer lemmatizer,
                            List<Field> fields,
                            HashMap<String, Float> lemmas){
        fields.forEach(field -> {
            StringBuilder stringBuilder = new StringBuilder();
            List<String> text = getTagText(document, field.getSelector());
            text.forEach(str -> stringBuilder.append(str).append(" "));

            HashMap<String, Integer> textLemmas = lemmatizer.analyzeText(stringBuilder.toString());
            countRank(textLemmas, lemmas, field);
        });
    }

    private List<String> getTagText(Document document, String tag){
        List<String> tagText = new ArrayList<>();
        Elements elements = document.select(tag);
        elements.forEach(element -> tagText.add(element.text()));
        return tagText;
    }

    private void countRank(HashMap<String, Integer> textLemmas,
                           HashMap<String, Float> lemmas,
                           Field field){
        textLemmas.keySet().forEach(lemma -> {
            float rank = lemmas.getOrDefault(lemma, 0f);

            if(rank == 0f){
                float newRank = textLemmas.get(lemma) * field.getWeight();
                lemmas.put(lemma, newRank);
            }
            else {
                float newRank = (textLemmas.get(lemma) * field.getWeight()) + lemmas.get(lemma);
                lemmas.replace(lemma, newRank);
            }
        });
    }

    private HashMap<Lemma, Float> getDBLemmas(HashMap<String, Float> lemmas){
        HashMap<Lemma, Float> lemmaList = new HashMap<>();

        lemmas.keySet().forEach(lemma -> {
            Lemma dbLemma;

            try {
                dbLemma = session.createQuery("from Lemma where lemma=:lemma", Lemma.class)
                        .setParameter("lemma", lemma).getSingleResult();
            }
            catch (Exception exception){
                dbLemma = new Lemma(lemma, 0);
            }

            dbLemma.incrementFrequency(1);

            lemmaList.put(dbLemma, lemmas.get(lemma));
        });

        saveLemmas(lemmaList);

        return lemmaList;
    }

    private void saveLemmas(HashMap<Lemma, Float> lemmaList){
        try {
            session.beginTransaction();
            lemmaList.keySet().forEach(lemma -> session.saveOrUpdate(lemma));
            session.getTransaction().commit();
        }
        catch (Exception exception){
            session.getTransaction().rollback();
            System.out.println("WARNING: The transaction was not completed!");
        }
    }

    private void saveIndex(HashMap<Lemma, Float> lemmaList, Website website){
        List<Index> indexList = new ArrayList<>();
        int pageId = website.getId();

        lemmaList.keySet().forEach(lemma -> {

            int lemmaId = lemma.getId();
            float lemmaRank = lemmaList.get(lemma);

            Index index = new Index(pageId, lemmaId, lemmaRank);
            indexList.add(index);
        });

        try {
            session.beginTransaction();
            indexList.forEach(index -> session.saveOrUpdate(index));
            session.getTransaction().commit();
        }
        catch (Exception exception){
            session.getTransaction().rollback();
            System.out.println("WARNING: The transaction was not completed!");
        }

    }
}
