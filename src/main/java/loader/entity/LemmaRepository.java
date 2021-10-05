package loader.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Lemma findLemmaBySiteIdAndLemma(int siteId, String lemma);
    List<Lemma> findAllBySiteIdAndLemma(int siteId, String lemma);
    Lemma findLemmaByLemma(String lemma);
}
