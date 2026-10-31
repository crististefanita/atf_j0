package atf.autoex.tests.e2e.failures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Test-only association of this consumer journey with its delivered artifact. */
final class AutoExBusinessArtifactAudit {
    private AutoExBusinessArtifactAudit() { }
    /** Consumer acceptance: associate the observed business cause and original capture with this exact invocation. */
    record ArtifactCase(String key, String owner, String scenario, boolean negative, String checkpoint,
                                String cause, int captureColor, int expectedCaptures, boolean afterSetup) { }

    static void auditExisting(Path manifest) throws Exception {
        var mapper=new ObjectMapper(); var input=mapper.readTree(manifest.toFile());
        List<ArtifactCase> cases=mapper.convertValue(input.path("cases"),new com.fasterxml.jackson.core.type.TypeReference<List<ArtifactCase>>() { });
        int rejected=verifyArtifact(Path.of(input.path("report").asText()),cases);
        atf.console.AtfConsoleReporter.configuredStdout().info("ARTIFACT_ACCEPTANCE=PASS; cases="+cases.size()+"; mutantsRejected="+rejected);
    }

    static int verifyArtifact(Path report, List<ArtifactCase> expected) throws Exception {
        try (var playwright=com.microsoft.playwright.Playwright.create();
             var browser=playwright.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
             var page=browser.newPage()) {
            String html=java.nio.file.Files.readString(report);
            String address="http://atf-artifact.invalid/report.html";
            page.route("**/*", route -> {
                if(route.request().url().equals(address)) route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                        .setStatus(200).setContentType("text/html; charset=utf-8").setBody(html));
                else route.abort();
            });
            page.navigate(address+"#atf-fails-view");
            verifyRendered(page,expected);
            int rejected=0;
            for (var item:expected) {
                for (String mutation:item.negative()?List.of("capture-remove","capture-reassign","verdict","cause"):List.of("verdict","inherited-cause")) {
                    var input=Map.of("owner",item.owner(),"scenario",item.scenario(),"mutation",mutation,"cause",item.cause());
                    page.evaluate("""
                            input => {
                                const roots=Array.from(document.querySelectorAll('.test-view .test-item[test-id]'));
                                const root=roots.find(r=>r.querySelector('.test-detail .name').textContent.trim()===input.owner);
                                const card=Array.from(root.querySelectorAll('.card')).find(c=>!c.parentElement.closest('.card')
                                    && c.querySelector('.card-header .node').textContent.trim()===input.scenario);
                                const links=Array.from(card.querySelectorAll('a[data-atf-screenshot="base64"]'));
                                const undo=[];
                                if(input.mutation==='capture-remove') {
                                    if(!links.length)throw Error('Control did not remove an actual capture');
                                    const a=links[0],p=a.parentNode,n=a.nextSibling;a.remove();undo.push(()=>p.insertBefore(a,n));
                                } else if(input.mutation==='capture-reassign') {
                                    const sibling=roots.flatMap(r=>Array.from(r.querySelectorAll('a[data-atf-screenshot="base64"]')))
                                        .find(a=>!card.contains(a));
                                    const replacement=sibling?.getAttribute('href')||document.querySelector('link[rel="shortcut icon"]').getAttribute('href');
                                    const a=links[0],old=a.getAttribute('href');a.setAttribute('href',replacement);undo.push(()=>a.setAttribute('href',old));
                                } else if(input.mutation==='verdict') {
                                    const badge=card.querySelector('.card-header .badge');
                                    const old=badge.textContent;badge.textContent=old.trim()==='Fail'?'Pass':'Fail';undo.push(()=>badge.textContent=old);
                                } else if(input.mutation==='inherited-cause') {
                                    const node=document.createElement('span');node.textContent='Controlled failure after account setup PASS';
                                    card.querySelector('[data-atf-phase-name="test-body"]').appendChild(node);undo.push(()=>node.remove());
                                } else {
                                    const walker=document.createTreeWalker(card,NodeFilter.SHOW_TEXT);let node,count=0;
                                    while(node=walker.nextNode())if(node.nodeValue.includes(input.cause)) {
                                        const target=node,old=node.nodeValue;target.nodeValue=old.replaceAll(input.cause,'REMOVED CONTROLLED CAUSE');
                                        undo.push(()=>target.nodeValue=old);count++;
                                    }
                                    if(!count)throw Error('Control did not remove an actual cause');
                                }
                                window.__atfAcceptanceRestore=()=>undo.reverse().forEach(action=>action());
                            }
                            """,input);
                    try {
                        var failure=assertThrows(IllegalStateException.class,()->verifyRendered(page,expected),item.key()+": "+mutation+" must be rejected");
                        String field=mutation.startsWith("capture")?"capture":mutation.equals("cause")?"cause":mutation.equals("inherited-cause")?"inheritedCause":"status";
                        assertTrue(failure.getMessage().startsWith(item.key()+"."+field+":"),"Focused oracle diagnostic: "+failure.getMessage());
                        rejected++;
                    } finally { page.evaluate("() => window.__atfAcceptanceRestore()"); }
                }
            }
            verifyRendered(page,expected);
            return rejected;
        }
    }

    private static void verifyRendered(Page page, List<ArtifactCase> expected) throws Exception {
        var facts=new ObjectMapper().valueToTree(page.evaluate("""
                () => {
                    const cases=[], owners=[];
                    for(const root of document.querySelectorAll('.test-view .test-item[test-id]')) {
                        const owner=root.querySelector('.test-detail .name').textContent.trim();
                        owners.push({owner,status:root.getAttribute('status')});
                        for(const card of root.querySelectorAll('.card')) {
                            if(card.parentElement.closest('.card'))continue;
                            const phase=card.querySelector('[data-atf-phase-name="test-body"]');
                            cases.push({owner,scenario:card.querySelector('.card-header .node').textContent.trim(),
                                status:card.querySelector('.card-header .badge').textContent.trim().toLowerCase(),
                                phaseStatus:phase?.querySelector('.card-header .badge')?.textContent.trim().toLowerCase(),
                                body:phase?.textContent||'',
                                checkpoints:Array.from(phase?.querySelectorAll('[data-atf-node-role="request"]')||[]).map(c=>({
                                    title:c.querySelector('.card-header .node')?.textContent.trim(),
                                    status:c.querySelector('.card-header .badge')?.textContent.trim().toLowerCase()})),
                                failures:phase?.querySelectorAll('[data-atf-evidence-status="fail"]').length||0,
                                captures:Array.from(card.querySelectorAll('a[data-atf-screenshot="base64"]')).map(a=>a.getAttribute('href'))});
                        }
                    }
                    const metrics=Object.fromEntries(Array.from(document.querySelectorAll('#atf-execution-metadata-grid .atf-secondary-metric'))
                        .map(c=>[c.querySelector('small').textContent.trim(),Number(c.querySelector('strong').textContent)]));
                    const fails=Array.from(document.querySelectorAll('#atf-fails-view .atf-failure')).map(c=>({
                        owner:c.querySelector('.test-detail .text-sm')?.textContent.trim(),
                        scenario:c.querySelector('.test-detail .name')?.textContent.trim()}));
                    return {cases,owners,metrics,fails};
                }
                """));
        requireArtifact(facts.path("cases").size()==expected.size(),"report.population","Every invocation is represented exactly once");
        for(var item:expected) {
            var matches=new ArrayList<com.fasterxml.jackson.databind.JsonNode>();
            for(var candidate:facts.path("cases")) if(candidate.path("owner").asText().equals(item.owner())
                    && candidate.path("scenario").asText().equals(item.scenario()))matches.add(candidate);
            requireArtifact(matches.size()==1,item.key()+".identity","Expected owner and scenario appear once");
            var actual=matches.getFirst();
            String status=item.negative()?"fail":"pass";
            requireArtifact(actual.path("status").asText().equals(status),item.key()+".status","Scenario verdict is "+status);
            requireArtifact(actual.path("phaseStatus").asText().equals(status),item.key()+".phase","TEST BODY verdict is "+status);
            if(item.negative()) {
                requireArtifact(actual.path("body").asText().contains(item.cause()),item.key()+".cause","Original business exception is published in its own phase");
                boolean checkpoint=false;
                for(var node:actual.path("checkpoints")) if(node.path("title").asText().contains(item.checkpoint())
                        && node.path("status").asText().equals(item.afterSetup()?"pass":"fail"))checkpoint=true;
                requireArtifact(checkpoint,item.key()+".checkpoint","Exact business checkpoint retains its own verdict");
                boolean capture=true;
                requireArtifact(actual.path("captures").size()==item.expectedCaptures(),item.key()+".capture","Every configured capture point remains represented");
                for(var image:actual.path("captures")) capture &= hasCaptureColor(image.asText(),item.captureColor());
                requireArtifact(capture,item.key()+".capture","Original invocation capture is attached to this scenario");
            } else {
                requireArtifact(actual.path("failures").asInt()==0,item.key()+".inheritedFailure","Healthy phase inherits no failed evidence");
                requireArtifact(!actual.path("body").asText().contains("Controlled failure after account setup PASS"),item.key()+".inheritedCause","Healthy successor inherits no controlled cause");
                for(var sibling:expected)if(sibling.negative())requireArtifact(!actual.path("body").asText().contains(sibling.cause()),
                        item.key()+".inheritedCause","Healthy successor inherits no sibling cause");
            }
            boolean inFails=false;
            for(var row:facts.path("fails"))if(row.path("owner").asText().equals(item.owner())&&row.path("scenario").asText().equals(item.scenario()))inFails=true;
            requireArtifact(inFails==item.negative(),item.key()+".fails","Fails lists only this invocation's rejected evidence");
        }
        long failures=expected.stream().filter(ArtifactCase::negative).count();
        requireArtifact(facts.path("metrics").path("Scenarios in this run").asInt(-1)==expected.size(),"report.overview.total","Overview population matches invocations");
        requireArtifact(facts.path("metrics").path("Scenario fail").asLong(-1)==failures,"report.overview.fail","Overview failure count matches JUnit");
        requireArtifact(facts.path("metrics").path("Scenario pass").asLong(-1)==expected.size()-failures,"report.overview.pass","Overview healthy count matches JUnit");
        for(var root:facts.path("owners")) {
            String owner=root.path("owner").asText();
            boolean failed=expected.stream().anyMatch(item->item.owner().equals(owner)&&item.negative());
            requireArtifact(root.path("status").asText().equals(failed?"fail":"pass"),owner+".owner","Owner verdict aggregates its own scenarios");
        }
    }

    private static boolean hasCaptureColor(String uri,int color) throws Exception {
        if(!uri.startsWith("data:image/png;base64,"))return false;
        var image=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(uri.substring(uri.indexOf(',')+1))));
        if(image==null)return false;
        int count=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)
            if((image.getRGB(x,y)&0xFFFFFF)==color && ++count>=500)return true;
        return false;
    }
    private static void requireArtifact(boolean valid,String key,String detail) {
        if(!valid)throw new IllegalStateException(key+": "+detail);
    }

}
