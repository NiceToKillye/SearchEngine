package loader.scanner;

import org.hibernate.Session;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
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

    public void scan(String websiteLink){
        uniqueWebsiteLinks.add(websiteLink);
        List<String> websiteMap = new ForkJoinPool().invoke(new ScannerTask(websiteLink));

        List<Website> websiteList = getWebsitesList(websiteMap);
        fillDB(websiteList);
        uniqueWebsiteLinks.clear();
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
                Connection.Response response = Jsoup
                        .connect(website)
                        .userAgent(USER_AGENT)
                        .timeout(1000)
                        .execute();
                website1.setContent(document.html());
                website1.setCode(response.statusCode());
            } catch (Exception e) {
                System.out.println("LINK: " + website + " " + website1.getCode());
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
}
