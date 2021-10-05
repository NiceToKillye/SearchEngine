package loader.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IndexRepository extends JpaRepository<Index, Integer> {
    Index findByLemmaIdAndPageId(int lemmaId, int pageId);
    List<Index> findAllByLemmaId(int lemmaId);
    List<Index> findAllByLemmaIdInAndPageId(List<Integer> lemmaId, int pageId);
}
