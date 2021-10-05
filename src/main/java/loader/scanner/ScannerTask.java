package loader.scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

@Async
public class ScannerTask extends RecursiveTask<List<String>> {

    private final String websiteStartLink;
    private String userAgent;
    private String referer;

    public ScannerTask(String websiteLink,
                       String userAgent,
                       String referer){
        this.websiteStartLink = websiteLink;
        this.userAgent = userAgent;
        this.referer = referer;
    }

    @Override
    protected List<String> compute() {
        ArrayList<String> websiteList = new ArrayList<>();
        websiteList.add(websiteStartLink);

        if(websiteStartLink.contains(".pdf")){
            return websiteList;
        }

        try{
            List<ScannerTask> subTasksList = new LinkedList<>();

            Document document = Jsoup
                    .connect(websiteStartLink)
                    .userAgent(userAgent)
                    .referrer(referer)
                    .maxBodySize(0)
                    .get();

            //Jsoup.connect(websiteStartLink).maxBodySize(0).get();
            Thread.sleep(1000);

            Elements urls = document.body().getElementsByTag("a");
            urls.forEach(url -> {
                String link = url.absUrl("href");

                if (link.contains("#")) {
                    link = link.split("#")[0];
                }
                if (link.contains("?")) {
                    link = link.split("\\?")[0];
                }
                if (link.startsWith(websiteStartLink) && !Scanner.uniqueWebsiteLinks.contains(link)) {
                    Scanner.uniqueWebsiteLinks.add(link);
                    ScannerTask task = new ScannerTask(link, userAgent, referer);
                    task.fork();
                    subTasksList.add(task);
                }
            });

            subTasksList.forEach(scannerTask ->
                    websiteList.addAll(scannerTask.join())
            );
        }
        catch (Exception exception){
            exception.printStackTrace();
        }

        return websiteList;
    }
}
