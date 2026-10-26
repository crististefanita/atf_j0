package atf.shop.flow;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.model.ReportExecutionEvidence;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.support.ShopPurchaseTraceStep;
import atf.shop.support.validation.ShopPageValidationResult;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Builds Shop checkpoint evidence and publishes it before enforcing page gates. */
final class ShopPurchaseCheckpointPublisher {
    private final Consumer<ShopPurchaseCheckpoint> sink;

    ShopPurchaseCheckpointPublisher(Consumer<ShopPurchaseCheckpoint> sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    ShopPurchaseCheckpoint checkpoint(AtfGherkinKeyword keyword, String title,
                                      ShopPageValidationResult validation,
                                      List<AtfAssertion> additional,
                                      List<ReportExecutionEvidence> api) {
        return new ShopPurchaseCheckpoint(keyword, title, validation.assertions())
                .withAdditionalAssertions(additional)
                .withApiEvidence(api);
    }

    ShopPurchaseCheckpoint checkpoint(ShopPurchaseTraceStep step,
                                      ShopPageValidationResult validation,
                                      List<AtfAssertion> additional,
                                      List<ReportExecutionEvidence> api) {
        return checkpoint(step.keyword(), step.title(), validation, additional, api);
    }

    /** Catalog stages use this gate; checkout delegates the same order to ShopCheckoutFlow. */
    void publish(ShopPurchaseCheckpoint checkpoint) {
        report(checkpoint);
        if (!checkpoint.passed()) {
            throw new IllegalStateException("Page validation failed: " + checkpoint.title());
        }
    }

    /** The checkout coordinator publishes first and evaluates the same gate itself. */
    void report(ShopPurchaseCheckpoint checkpoint) {
        sink.accept(Objects.requireNonNull(checkpoint, "checkpoint"));
    }
}
