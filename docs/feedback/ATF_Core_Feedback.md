# Core — contracte și trasee de mentenanță

## Pe scurt

J0 deține execuția transporturilor, anularea, redactarea și promovarea fișierelor. Contractele și probele permanente sunt în [Core README](../../atf-core/README.md); consumerii păstrează acceptanța propriului deployment.

<a id="core-urlconnection-cancel-126--body-subscription-and-dispatch-ownership"></a>
<a id="core-console-redaction-124--sanitize-every-emitted-console-channel"></a>
<a id="core-transport-cancel-125--retain-ownership-of-the-upstream-http-future"></a>

## Contracte păstrate la owner

| Boundary | Contract și probă permanentă |
|---|---|
| HttpClient | [Anularea upstream](../../atf-core/README.md#proiecția-console-și-anularea-transportului), un singur rezultat terminal și cleanup suppressed |
| URLConnection | Subscription body, worker virtual și conexiune deținute de aceeași operație; body-ul întârziat nu poate începe un dispatch după anulare sau deadline |
| Deadline | Colectarea body-ului, connect și read folosesc bugetul original; [regresiile](../../atf-core/src/test/java/atf/core/http/transport/UrlConnectionDeadlineTest.java) păstrează 250 ms; [izolarea JVM](../../atf-core/src/test/java/atf/core/http/transport/UrlConnectionDeadlineIsolationTest.java) probează progresul când un carrier virtual este blocat |
| Erori și curse | [Publisher controlat și loopback](../../atf-core/src/test/java/atf/core/http/transport/UrlConnectionCancellationTest.java): cauză originală, suppressed, finalizare concurentă și lipsa unui dispatch ulterior |
| Distribuție | [Contractul consumerului](../ATF_Core_Consumer.md): native sursă/JAR, POM încorporat identic și perechi dependente coerente |

Anularea unei operații deja expediate nu confirmă rollback și nu autorizează replay. Probele Core nu certifică aplicația, brokerul, dispozitivul ori ASR-ul consumerului.

## Unde se înregistrează următoarea lacună

- Trigger, coordonată/hash, reproducere minimă, așteptat/observat și criteriu negativ; o problemă a unui transport nu se generalizează la celălalt.
- [Reporting](ATF_Reporting_Feedback.md) deține rendererul; [Reporting Tests](ATF_Reporting_Tests_Feedback.md) deține oracolele neutre de randare.
- [Registrul J1](../../../atf_j1/docs/feedback/README.md) coordonează criteriile comune nonbibliotecă; consumerii păstrează acceptanța domeniului.
- [Baseline-ul tehnic](../ATF_Test_Changes_Baseline.md) păstrează controalele Git separat de backlog.
