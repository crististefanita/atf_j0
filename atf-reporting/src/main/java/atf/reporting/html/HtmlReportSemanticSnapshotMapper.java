package atf.reporting.html;

import atf.reporting.model.ReportExecutionModel;

import java.util.Collection;

/** Converts the mutable lifecycle document into the immutable renderer model. */
final class HtmlReportSemanticSnapshotMapper {
    private HtmlReportSemanticSnapshotMapper() {
    }

    static ReportExecutionModel map(Collection<HtmlReportSemanticDocument.ScenarioModel> scenarios) {
        return new ReportExecutionModel(scenarios.stream().map(scenario ->
                new ReportExecutionModel.Scenario(scenario.id, scenario.testClass,
                        scenario.packageName, scenario.scenario, scenario.tags,
                        scenario.phases.stream().map(phase ->
                                new ReportExecutionModel.Phase(phase.name,
                                        phase.details.stream().toList(),
                                        phase.children.stream()
                                                .map(HtmlReportSemanticSnapshotMapper::mapNode)
                                                .toList(),
                                        phase.status)).toList(), scenario.status)).toList());
    }

    private static ReportExecutionModel.Node mapNode(HtmlReportSemanticDocument.NodeModel node) {
        if (node instanceof HtmlReportSemanticDocument.RequestModel request) {
            return new ReportExecutionModel.Request(request.title, request.method, request.path,
                    request.details.stream().toList(), request.json.stream().toList(),
                    request.executions, request.children.stream()
                            .map(HtmlReportSemanticSnapshotMapper::mapNode).toList(),
                    request.status, request.uiActionCount, request.screenshotCount,
                    request.networkAggregate);
        }
        var validation = (HtmlReportSemanticDocument.ValidationModel) node;
        return new ReportExecutionModel.Validation(validation.title, validation.assertions,
                validation.status, validation.uiActionCount, validation.screenshotCount,
                validation.children.stream()
                        .map(HtmlReportSemanticSnapshotMapper::mapNode)
                        .toList(), validation.uiActions, validation.screenshots, validation.evidenceOwner);
    }
}
