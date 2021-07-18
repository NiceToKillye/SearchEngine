package loader.lemmatizer;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Lemmatizer {

    private final LuceneMorphology luceneMorphRu;
    private final LuceneMorphology luceneMorphEn;

    private final List<String> servicePartsOfSpeech = new ArrayList<>();

    public Lemmatizer() throws IOException {
        luceneMorphRu = new RussianLuceneMorphology();
        luceneMorphEn = new EnglishLuceneMorphology();

        servicePartsOfSpeech.add("ПРЕДЛ");
        servicePartsOfSpeech.add("МЕЖД");
        servicePartsOfSpeech.add("СОЮЗ");
        servicePartsOfSpeech.add("ПРЕДК");
    }

    public HashMap<String, Integer> analyzeText(String text) {
        HashMap<String, Integer> lemmas = new HashMap<>();
        text = text.toLowerCase();

        String russianText = text.replaceAll("[^А-ЯЁа-яё\\s]", "")
                .replaceAll("\\s++", " ").trim();
        String englishText = text.replaceAll("[^A-Za-z\\s]", "")
                .replaceAll("\\s++", " ").trim();

        putLemmas(russianText, luceneMorphRu, lemmas);
        putLemmas(englishText, luceneMorphEn, lemmas);

        return lemmas;
    }

    private void putLemmas(String text, LuceneMorphology morphology, HashMap<String, Integer> lemmas){
        if(!text.isEmpty()){
            text = replaceAll(text, morphology);
            if(!text.isEmpty()){
                lemmas.putAll(findLem(text, morphology));
            }
        }
    }

    private String replaceAll(String text, LuceneMorphology morphology){
        List<String> result = new ArrayList<>();

        Arrays.asList(text.split(" ")).forEach(word -> {
            String info = morphology.getMorphInfo(word).get(0).split(" ")[1];

            if(!servicePartsOfSpeech.contains(info)){
                result.add(word);
            }
        });

        StringBuilder builder = new StringBuilder();
        result.forEach(word -> builder.append(word).append(" "));

        return builder.toString();
    }

    private HashMap<String, Integer> findLem(String text, LuceneMorphology morphology){
        HashMap<String, Integer> lemmas = new HashMap<>();

        Arrays.stream(text.split(" "))
                .forEach(word -> morphology.getNormalForms(word)
                        .forEach(firstForm -> {
                            int count = lemmas.getOrDefault(firstForm, 0);

                            if (count == 0) {
                                lemmas.put(firstForm, 1);
                            } else {
                                lemmas.replace(firstForm, count + 1);
                            }
                        })
                );

        return lemmas;
    }

}
