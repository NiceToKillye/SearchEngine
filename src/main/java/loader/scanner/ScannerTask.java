package loader.scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class ScannerTask extends RecursiveTask<List<String>> {

    private final String websiteStartLink;

    public ScannerTask(String websiteLink){
        this.websiteStartLink = websiteLink;
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

            Document document = Jsoup.connect(websiteStartLink).maxBodySize(0).get();
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
                    ScannerTask task = new ScannerTask(link);
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
