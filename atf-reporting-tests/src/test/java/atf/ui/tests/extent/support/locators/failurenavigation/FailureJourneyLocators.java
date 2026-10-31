package atf.ui.tests.extent.support.locators.failurenavigation;

import com.microsoft.playwright.Locator;
import atf.ui.playwright.AtfLocator;
import java.util.Map;

/** DOM boundary for the exact source row and its disclosure ancestry. */
public final class FailureJourneyLocators {
    public Locator target(Locator scenario,String kind,int ordinal){
        if ("scenario".equals(kind) && ordinal == 1) return scenario;
        if(ordinal<1)throw new IllegalArgumentException("Failure ordinal must be positive");
        return scenario.locator(selector(kind)).nth(ordinal-1);
    }
    public AtfLocator targetAction(Locator scenario,String kind,int ordinal){
        String id=scenario.getAttribute("data-atf-scenario-id");
        if(id==null)throw new IllegalStateException("Failure scenario has no typed identity");
        String scope="[data-atf-scenario-id='"+id.replace("\\","\\\\").replace("'","\\'")+"']";
        String css="scenario".equals(kind)?scope:
                ":nth-match("+scope+" "+selector(kind)+","+ordinal+")";
        return new AtfLocator(target(scenario,kind,ordinal),css);
    }
    private static String selector(String kind){
        return switch(kind){
            case "diagnostic" -> "[class~='phase-diagnostics'] [data-atf-diagnostic-status='fail'] tbody > tr";
            case "execution" -> "[data-atf-terminal-failure='true']";
            case "assertion" -> "table[class~='helm-validation-table']:not([class~='atf-ui-failure-evidence'] table)"
                    + " > tbody > tr:has(> td:last-child:has-text('❌'))";
            default -> throw new IllegalArgumentException("Unknown failure kind: "+kind);
        };
    }

    @SuppressWarnings("unchecked")
    public PathState path(Locator target){
        Map<String,Object> facts=(Map<String,Object>)target.evaluate("""
                row => {
                  const state={phases:0,nodes:0,evidence:0,open:true,scenario:false};
                  for(let node=row;node;node=node.parentElement){
                    if(node.tagName==='DETAILS'){state.evidence++;state.open=state.open&&node.open;}
                    if(node.hasAttribute('data-atf-scenario-id')){
                      state.scenario=true;state.open=state.open&&node.getAttribute('data-atf-scenario-open')==='true';break;
                    }
                    const role=node.getAttribute('data-atf-node-role');
                    if(role==='phase'){state.phases++;state.open=state.open&&node.getAttribute('data-atf-phase-open')==='true';}
                    if(role==='request'||role==='validation'||role==='assertion-step'){
                      state.nodes++;state.open=state.open&&node.getAttribute('data-atf-node-open')==='true';
                    }
                  }
                  return state;
                }
                """);
        return new PathState(((Number)facts.get("phases")).intValue(),((Number)facts.get("nodes")).intValue(),
                ((Number)facts.get("evidence")).intValue(),Boolean.TRUE.equals(facts.get("open"))&&Boolean.TRUE.equals(facts.get("scenario")));
    }
    public Locator expandedNodeHeaders(Locator scope){
        return scope.locator(":is([data-atf-node-role='request'],[data-atf-node-role='validation'],"
                +"[data-atf-node-role='assertion-step']):visible > [class~='card-header'][role='button'][aria-expanded='true']");
    }
    public record PathState(int phases,int nodes,int evidence,boolean open){ }
}
