package loader.scanner;

import loader.entity.*;
import loader.lemmatizer.Lemmatizer;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class Scanner {

    @Value("${user.agent}")
    private String USER_AGENT;
    @Value("${user.referer}")
    private String REFERER;
    @Value("${arrays.websitesToScan}")
    private String[] websitesToScan;

    private final IndexRepository indexRepository;
    private final WebsiteRepository websiteRepository;
    private final FieldRepository fieldRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    static Set<String> uniqueWebsiteLinks = ConcurrentHashMap.newKeySet();


    public Scanner(IndexRepository indexRepository,
                   WebsiteRepository websiteRepository,
                   FieldRepository fieldRepository,
                   LemmaRepository lemmaRepository,
                   SiteRepository siteRepository){
        this.indexRepository = indexRepository;
        this.websiteRepository = websiteRepository;
        this.fieldRepository = fieldRepository;
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
    }

    public void scan(String websiteLink) throws IOException {
        uniqueWebsiteLinks.add(websiteLink);
        List<String> websiteMap = new ForkJoinPool().invoke(new ScannerTask(websiteLink, USER_AGENT, REFERER));

        Site site = createSite(websiteLink);

        List<Website> websiteList = getWebsitesList(websiteMap, websiteLink, site.getId());
        fillDB(websiteList);
        uniqueWebsiteLinks.clear();
        indexWebsite(websiteList, site.getId());
    }

    @PostConstruct
    public void scan() throws InterruptedException, IOException {
        Field firstField = new Field("title", "title", 1.0f);
        Field secondField = new Field("body", "body", 0.8f);

        fieldRepository.save(firstField);
        fieldRepository.save(secondField);

        if(websitesToScan.length == 0){

            return;
        }
        else if(websitesToScan.length == 1){
            scan(websitesToScan[0]);
            return;
        }

        List<List<String>> linksList = new ArrayList<>();
        List<Site> siteList = new ArrayList<>();
        for(String websiteLink : websitesToScan){
            List<String> websiteMap = new ForkJoinPool().invoke(new ScannerTask(websiteLink, USER_AGENT, REFERER));
            linksList.add(websiteMap);
            siteList.add(createSite(websiteLink));
        }

        List<List<Website>> websitesList = new ArrayList<>();
        for(int i = 0; i < linksList.size(); i++){
            websitesList.add(
                    getWebsitesList(
                            linksList.get(i),
                            siteList.get(i).getUrl(),
                            siteList.get(i).getId()
                    )
            );
        }
        websitesList.forEach(this::fillDB);

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(availableProcessors);

        List<Callable<Boolean>> indexTasks = new ArrayList<>();
        for(int i = 0; i < websitesList.size(); i++){
            int finalI = i;
            indexTasks.add(
                    () -> {
                        indexWebsite(websitesList.get(finalI), siteList.get(finalI).getId());
                        return Boolean.TRUE;
                    }
            );
        }
        executorService.invokeAll(indexTasks);

        try {
            executorService.shutdown();
            executorService.awaitTermination(60, TimeUnit.SECONDS);
        }
        catch (Exception exception){
            System.err.println("Finishing all tasks");
        }
        finally {
            if(!executorService.isTerminated()){
                executorService.shutdownNow();
                System.out.println("Finished");
            }
            else {
                System.out.println("Already finished");
            }
        }
    }

    private Site createSite(String websiteLink){
        Pattern pattern = Pattern.compile("http[s]*://");
        Matcher matcher = pattern.matcher(websiteLink);
        String name = matcher.replaceAll("");
        Site site = new Site(Status.INDEXING, LocalDate.now(), null, websiteLink, name);
        siteRepository.save(site);
        return site;
    }

    private List<Website> getWebsitesList(List<String> websiteMap, String websiteLink, int siteId){
        List<Website> websiteList = new ArrayList<>();
        websiteMap.forEach(website ->{
            Website website1 = new Website();
            if(website.replace(websiteLink, "").length() == 0){
                website1.setPath(websiteLink);
            }
            else {
                website1.setPath(website.replace(websiteLink, ""));
            }
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
                        .referrer(REFERER)
                        .timeout(1000)
                        .execute();
                website1.setCode(response.statusCode());
            } catch (Exception e) {
                System.out.println("LINK: " + website + " " + website1.getCode());
                website1.setCode(504);
                e.printStackTrace();
            }
            website1.setSiteId(siteId);

            websiteList.add(website1);
        });

        return websiteList;
    }

    private void fillDB(List<Website> websiteList){
        try {
            websiteRepository.saveAll(websiteList);
        }
        catch (Exception exception){
            System.out.println("Transaction was not compilied");
        }
    }

    private void indexWebsite(List<Website> websiteList, int siteId) throws IOException {
        Lemmatizer lemmatizer = new Lemmatizer();

        List<Field> fields = fieldRepository.findAll();

        websiteList.forEach(website -> {
            if(website.getCode() == 200) {
                Document document = Jsoup.parse(website.getContent());
                HashMap<String, Float> lemmas = new HashMap<>();
                findLemmas(document, lemmatizer, fields, lemmas);
                HashMap<Lemma, Float> lemmasFromDB = getDBLemmas(lemmas, siteId);
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

    //sdfsdfsdf
    private HashMap<Lemma, Float> getDBLemmas(HashMap<String, Float> lemmas, int siteId){
        HashMap<Lemma, Float> lemmaList = new HashMap<>();

        lemmas.keySet().forEach(lemma -> {
            Lemma dbLemma;

            try {
                dbLemma = lemmaRepository.findLemmaBySiteIdAndLemma(siteId, lemma);
            }
            catch (Exception exception){
                List<Lemma> dbLemmas = lemmaRepository.findAllBySiteIdAndLemma(siteId, lemma)
                        .stream().filter(lemma1 -> lemma1.getLemma().equals(lemma))
                        .collect(Collectors.toList());
                dbLemma = dbLemmas.get(0);
            }

            if(dbLemma == null){
                dbLemma = new Lemma(siteId, lemma, 0);
            }

            dbLemma.incrementFrequency(1);

            lemmaList.put(dbLemma, lemmas.get(lemma));
        });

        saveLemmas(lemmaList);

        return lemmaList;
    }

    private synchronized void saveLemmas(HashMap<Lemma, Float> lemmaList){
        lemmaRepository.saveAll(lemmaList.keySet());
    }

    private synchronized void saveIndex(HashMap<Lemma, Float> lemmaList, Website website){
        List<Index> indexList = new ArrayList<>();
        int pageId = website.getId();

        lemmaList.keySet().forEach(lemma -> {

            int lemmaId = lemma.getId();
            float lemmaRank = lemmaList.get(lemma);

            Index index = new Index(pageId, lemmaId, lemmaRank);
            indexList.add(index);
        });

        indexRepository.saveAll(indexList);
    }
}
