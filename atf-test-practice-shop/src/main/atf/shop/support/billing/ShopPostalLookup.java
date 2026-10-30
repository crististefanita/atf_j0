package atf.shop.support.billing;

import atf.shop.api.ShopApiRoutes;
import atf.shop.api.contract.ShopApiQueryParameter;
import atf.shop.support.ShopJsonEvidence;
import atf.core.http.request.ApiQueryParameters;
import atf.ui.playwright.PlaywrightActionSequence.ObservationBudget;
import atf.ui.playwright.lifecycle.UiListenerScope;
import atf.ui.recovery.UiRecoveryOperation;
import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.*;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.*;

/** Passive input-owned lookup and autofill settlement; never sends a request or repeats native input. */
public final class ShopPostalLookup implements AutoCloseable {
    public record Address(String street, String city, String state) { }
    private static final int MAX_PENDING = 64;
    private static final long INPUT_DEBOUNCE_NANOS = Duration.ofMillis(300).toNanos();
    private final Page page;
    private final Frame frame;
    private final URI endpoint;
    private final String origin;
    private final Map<String,String> expected;
    private final UiListenerScope listeners = new UiListenerScope();
    private final Set<Request> pending = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Consumer<Request> started = this::started;
    private final Consumer<Request> finished = request -> completed(request, false);
    private final Consumer<Request> failed = request -> completed(request, true);
    private final Consumer<Frame> navigated;
    private Request current, loadedFor;
    private Address response;
    private RuntimeException failure, observationFailure;
    private Integer status;
    private boolean documentChanged, evaluated, currentCompleted;
    private long inputsAppliedAt, starts, completions;

    public ShopPostalLookup(Page page, URI endpoint, String country, String postcode, String houseNumber) {
        this.page=Objects.requireNonNull(page);frame=page.mainFrame();
        this.endpoint=Objects.requireNonNull(endpoint);origin=UiRecoveryOperation.origin(endpoint);
        if(!ShopApiRoutes.POSTCODE_LOOKUP.equals(endpoint.getRawPath()) || endpoint.getRawQuery()!=null || endpoint.getRawFragment()!=null)
            throw new IllegalArgumentException("An explicit postcode lookup endpoint without query is required");
        expected=Map.of(ShopApiQueryParameter.COUNTRY.wireValue(),required(country),
                ShopApiQueryParameter.POSTCODE.wireValue(),required(postcode),
                ShopApiQueryParameter.HOUSE_NUMBER.wireValue(),required(houseNumber));
        navigated=changed->{if(listeners.active() && changed==frame) documentChanged=true;};
        listeners.add(()->page.onRequest(started),()->page.offRequest(started))
                .add(()->page.onRequestFinished(finished),()->page.offRequestFinished(finished))
                .add(()->page.onRequestFailed(failed),()->page.offRequestFailed(failed))
                .add(()->page.onFrameNavigated(navigated),()->page.offFrameNavigated(navigated));
        listeners.attach();
    }
    /** The application's 300ms input debounce is consumed inside the existing observation deadline. */
    public void inputsApplied() {
        if(!listeners.active() || evaluated || inputsAppliedAt!=0) throw new IllegalStateException("Lookup inputs are already closed or applied");
        inputsAppliedAt=System.nanoTime();
    }
    private boolean relevant(Request request) {
        URI actual=URI.create(request.url());
        return "GET".equals(request.method()) && endpoint.getRawPath().equals(actual.getRawPath())
                && origin.equals(UiRecoveryOperation.origin(actual)) && request.frame()==frame;
    }
    private void started(Request request) {
        if(!listeners.active()) return;
        try {
            if(!relevant(request)) return;
            if(pending.size()>=MAX_PENDING) throw new IllegalStateException("Too many concurrent postcode lookups");
            pending.add(request);starts++;
            if(expected.equals(query(URI.create(request.url())))) {
                current=request;response=null;failure=null;status=null;currentCompleted=false;loadedFor=null;
            }
        } catch(RuntimeException unavailable) {observationFailure=unavailable;}
    }
    private void completed(Request request,boolean networkFailed) {
        if(!listeners.active()) return;
        if(!pending.remove(request)) {
            try {if(relevant(request)) observationFailure=new IllegalStateException("Postcode completion has no scope-owned request start");}
            catch(RuntimeException unavailable) {observationFailure=unavailable;}
            return;
        }
        completions++;
        if(request!=current) return;
        currentCompleted=true;
        if(networkFailed) failure=new IllegalStateException("Current postcode lookup failed before its response completed");
    }
    /** Driver reads happen outside event callbacks; a newer request cannot inherit an older body. */
    private void readCurrent() {
        if(current==null || !currentCompleted || loadedFor==current || failure!=null) return;
        Request reading=current;Address address=null;Integer capturedStatus=null;RuntimeException problem=null;
        try {
            Response captured=reading.response();
            if(captured==null) throw new IllegalStateException("Current postcode lookup has no response");
            capturedStatus=captured.status();
            if(capturedStatus<200 || capturedStatus>=300) throw new IllegalStateException("Current postcode lookup failed: HTTP "+capturedStatus);
            JsonNode body=ShopJsonEvidence.parse(captured.text());
            if(body==null) throw new IllegalStateException("Current postcode lookup returned invalid JSON");
            address=new Address(field(body,"street"),field(body,"city"),field(body,"state"));
        } catch(RuntimeException unavailable) {problem=unavailable;}
        if(current==reading) {loadedFor=reading;response=address;failure=problem;status=capturedStatus;}
    }
    public Address await(Supplier<Address> dom, ObservationBudget budget) {
        var observed=new java.util.concurrent.atomic.AtomicReference<Address>();
        evaluate(()->{
            Address expectedAddress=response;
            if(expectedAddress==null) return false;
            Address actual=dom.get();
            if(!expectedAddress.equals(actual)) return false;
            observed.set(actual);return true;
        },budget);
        return observed.get();
    }
    /** Configured fields own final values; they wait out autofill without requiring an invented lookup. */
    public void settleInputs(BooleanSupplier appIdle,ObservationBudget budget) {
        if(inputsAppliedAt==0) throw new IllegalStateException("Native billing inputs must be applied before settlement");
        evaluate(appIdle,budget);
    }
    private void evaluate(BooleanSupplier condition,ObservationBudget budget) {
        if(!listeners.active() || evaluated) throw new IllegalStateException("Postcode lookup observation is single-use");
        evaluated=true;
        try {
            while(true) {
                var ready=new java.util.concurrent.atomic.AtomicBoolean();
                var candidate=new java.util.concurrent.atomic.AtomicReference<Request>();
                page.waitForCondition(()->{
                    // Playwright can evaluate its Java predicate again when resolving the wait race.
                    if(ready.get()) return true;
                    requireScope();
                    if(inputsAppliedAt!=0 && System.nanoTime()-inputsAppliedAt<INPUT_DEBOUNCE_NANOS || !pending.isEmpty()) return false;
                    readCurrent();if(failure!=null) throw failure;
                    Request before=current;
                    if(!condition.getAsBoolean() || !pending.isEmpty() || current!=before) return false;
                    candidate.set(before);ready.set(true);return true;
                },new Page.WaitForConditionOptions().setTimeout(budget.remainingMilliseconds()));
                requireScope();budget.remainingMilliseconds();
                if(pending.isEmpty() && candidate.get()==current) return;
            }
        } catch(RuntimeException unavailable) {
            throw new IllegalStateException("Postcode lookup not confirmed; "+summary(),unavailable);
        }
    }
    private void requireScope() {
        if(!listeners.active() || documentChanged || page.mainFrame()!=frame) throw new IllegalStateException("Postcode lookup document changed or closed");
        if(observationFailure!=null) throw observationFailure;
    }
    public String summary() {return "owned starts="+starts+"; completed="+completions+"; pending="+pending.size()
            +"; current status="+(status==null?"not observed":status)+"; input debounce=300ms; direct requests=0";}
    private static String field(JsonNode body,String name) {
        JsonNode value=body.get(name);
        if(value==null || !value.isTextual() || value.textValue().isBlank()) throw new IllegalStateException("Current postcode lookup has no non-empty "+name);
        return value.textValue().trim();
    }
    private static String required(String value) {
        if(value==null || value.isBlank()) throw new IllegalArgumentException("Postcode lookup input must not be blank");
        return value.trim();
    }
    private static Map<String,String> query(URI uri) {
        var result=new HashMap<String,String>();
        for(var value:ApiQueryParameters.fromUri(uri).values()) if(result.putIfAbsent(value.name(),value.value())!=null) return Map.of();
        return result;
    }
    @Override public void close() {pending.clear();current=null;response=null;listeners.close();}
}
