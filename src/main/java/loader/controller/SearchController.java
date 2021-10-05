package loader.controller;

import loader.entity.Website;
import loader.searcher.Searcher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class SearchController {

    private final Searcher searcher;

    public SearchController(Searcher searcher) {
        this.searcher = searcher;
    }

    @GetMapping
    public String index(Model model){
        return "searcher";
    }

    @PostMapping
    List<String> search(@RequestBody String searchString){
        HashMap<Website, Double> result = searcher.search(searchString);

        return result.keySet().stream().map(Website::getPath).collect(Collectors.toList());
    }
}
