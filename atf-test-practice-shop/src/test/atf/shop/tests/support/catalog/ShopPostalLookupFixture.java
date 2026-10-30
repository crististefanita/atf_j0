package atf.shop.tests.support.catalog;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Owns every input, request and response; no public Shop or account mutation is involved. */
public final class ShopPostalLookupFixture implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private final CountDownLatch release = new CountDownLatch(1);
    public final AtomicInteger requests = new AtomicInteger();
    private final String mode;

    public ShopPostalLookupFixture(String mode) throws Exception {
        this.mode=mode; server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/", exchange -> {
            try(exchange) {
                byte[] body; int status=200;
                if(exchange.getRequestURI().getPath().equals("/postcode-lookup")) {
                    requests.incrementAndGet();
                    try {if(!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Fixture never released");}
                    catch(InterruptedException stopped){Thread.currentThread().interrupt();return;}
                    boolean old=mode.equals("same")||mode.equals("stale-only");
                    status=mode.equals("http-error")?500:200;
                    body=(mode.equals("malformed")?"PRIVATE_POSTCODE_CANARY": "{\"street\":\""+(mode.equals("blank")?"":(old?"Old":"New")+" street")
                            +"\",\"city\":\""+(old?"Old":"New")+" city\",\"state\":\""+(old?"Old":"New")+" state\"}")
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type","application/json");
                } else {
                    body=document().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type","text/html");
                }
                exchange.sendResponseHeaders(status,body.length);exchange.getResponseBody().write(body);
            }
        });
        server.setExecutor(workers);server.start();
    }
    private String document() {
        return """
                <select data-test='country'><option value='US'>United States</option></select>
                <input data-test='postal_code' value='99501'><input data-test='house_number' value='10'>
                <input data-test='street' value='Old street'><input data-test='city' value='Old city'>
                <input data-test='state' value='Old state'><output id='completions'>0</output>
                <script>
                const mode='%s';window.starts=0;window.inputs=0;
                function lookup(){
                  window.starts++;
                  const postal=mode==='wrong-query'?'111':document.querySelector('[data-test=postal_code]').value;
                  fetch('/postcode-lookup?country=US&postcode='+postal+'&house_number=10'+(mode==='duplicate-query'?'&country=US':'')).then(r=>{
                    if(!r.ok)throw new Error('HTTP '+r.status);return r.json();
                  }).then(address=>{
                    for(const key of ['street','city','state'])document.querySelector('[data-test='+key+']').value=address[key];
                    document.querySelector('#completions').textContent=String(Number(document.querySelector('#completions').textContent)+1);
                  }).catch(()=>{});
                }
                window.startOld=()=>{lookup();};
                document.querySelector('[data-test=house_number]').addEventListener('input',()=>{
                  window.inputs++;if(mode!=='missing'&&mode!=='stale-only')lookup();
                });
                </script>
                """.formatted(mode);
    }
    public URI home(){return URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/");}
    public URI endpoint(){return home().resolve("postcode-lookup");}
    public void release(){release.countDown();}
    public String completions(com.microsoft.playwright.Page page){return page.evaluate("document.getElementById('completions').textContent").toString();}
    @Override public void close(){release();try{server.stop(0);}finally{workers.shutdownNow();}}
}
