package atf.reporting.ui.audit.category;

import java.util.List;

/**
 * Renderer-neutral contract for the Categories-to-Tests links.
 *
 * <p>The browser adapter is responsible for discovering and clicking DOM
 * nodes. This class owns only the zero/one/many cardinality and identifier
 * rules so the same contract is testable without Playwright.</p>
 */
public final class CategoryFilterContract {
    private CategoryFilterContract() {
    }

    public static LinkValidation validateLinkedScenarioIds(int declaredCount,
                                                           List<String> linkedIds) {
        if (declaredCount < 0 || linkedIds == null || linkedIds.size() != declaredCount) {
            return new LinkValidation(false, declaredCount,
                    linkedIds == null ? 0 : linkedIds.size(), List.of("count"));
        }
        List<String> invalidIds = linkedIds.stream()
                .filter(id -> id == null || id.isBlank() || !id.matches("\\d+"))
                .toList();
        return new LinkValidation(invalidIds.isEmpty(), declaredCount,
                linkedIds.size(), invalidIds);
    }

    public record LinkValidation(boolean valid, int declaredCount, int actualCount,
                                 List<String> invalidIds) {
        public LinkValidation {
            invalidIds = List.copyOf(invalidIds);
        }
    }
}
