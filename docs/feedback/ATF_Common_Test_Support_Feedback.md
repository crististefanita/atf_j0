# Test Kit — bibliotecă comună, owner J0

## Pe scurt

J0 deține codul, API-ul, testele native, împachetarea și feedback-ul Test Kit. J1–J5 păstrează integrarea și oracolele lor; deocamdată folosesc module sursă. [Planul de adopție](../ATF_Test_Kit_Library.md) separă ținta de starea livrată.

## KIT-LIBRARY-151 — distribuție unică, fără surse paralele

**P1 · owner J0; adopție independentă în J1–J5.** Comparați inventarul și comportamentul API-urilor live înaintea împachetării; simpla egalitate a numelor claselor nu dovedește echivalența consumatorilor. Utilitarele ghidurilor și primitivele de execuție au contracte distincte.

POM-urile Kit depind încă de parent și versiunile moștenite; J5 are parent Rentz. Un build fără parent a fost verificat în TMP, fără modificarea proiectelor live.

| Poartă | Lacună și criteriu de închidere |
|---|---|
| Pachet independent | POM fără parent ATF, versiuni vendor explicite, JDK 21, build reproductibil; JAR thin numai atf.testkit, fără surse, native sau cod business. Testele native rămân în J0. |
| Console | Adaptorul declară Kit optional compile cu versiune SNAPSHOT; demonstrați integrarea cu versiunea publicată explicit la consumer. Dacă se schimbă POM-ul Console, publicați o coordonată nouă, fără rescrierea c1. Core/Mock folosesc Kit numai în teste. |
| Guvernanță | ConsumerModuleArchitectureGovernanceTest cere fișiere Kit .java; în consumerul binar verificați API, JAR, CodeSource/hash și catalogul de proveniență. Inspecția sursei și nativele rămân în J0. Refuzați shadow sources și versiunile multiple. |
| Adopție | Eliminați modulul numai după verificare; dependențele folosesc proprietatea Kit explicită, nu project.version. Bootstrap Maven/IntelliJ, scripturile scoped, inventarele și politicile JSON păstrează reactorul fiecărui consumer. AndroidComponentPublication din J4 folosește efectiv API Kit în main: permiteți compile explicit pentru suportul de test, fără propagare în APK/produs. În rest verificați utilizarea înainte de alegerea scope-ului. |
| Documentație | GuideBundle.publish enumeră explicit numai Core/Reporting/Console/Mock; extindeți controlat sandbox-ul pentru Kit și păstrați refuzul surselor paralele. Ghid local lib/README.TestKit.md cu adnotări, RecordedAssertions, lifecycle, configurație, scope, limite și exemplu executabil pe JAR/POM; accesul la J0 rămâne necesar doar mentenanței. |

- [ ] Construiți o singură pereche JAR/POM, verificată separat de reactor; aceeași pereche și provenance cu istoricul păstrat în toate cele cinci ținte. Inventarul lib acceptă explicit a cincea bibliotecă și ghidul ei; nu relaxați regula exactă a folderului.
- [ ] Păstrați regresii pentru niveluri/meta-adnotări, rezoluția politicilor, scope-uri paralele, lifecycle/cleanup cu cauza originală, configurație și aserțiuni obligatorii. Proba negativă elimină ori substituie numai Kit; nu trebuie să treacă printr-o copie sursă sau un cache ATF vechi.
- [ ] Verificați independent J1 web, J2 mesagerie, J3 speech, J4 Android și J5 Rentz: componentă, lansare individuală/suită, FAIL controlat, setup/cleanup și raport Extent/fallback unde adaptorul există. Nu introduceți Playwright în J2/J3 și nu confundați fixture-urile cu device/broker/ASR real.
- [ ] J5 nu are Git inițializat în locația analizată; utilizați snapshot-uri SHA și promovare guarded per fișier. Nu inventați HEAD, nu inițializați ori înlocuiți Git prin distribuirea bibliotecii.

**Închidere:** nativele sursă/JAR, exemplele portabile și acceptanțele distincte sunt verzi, sursa Kit rămâne numai în J0, iar toate cele cinci proiecte încarcă aceeași pereche declarată.

## KIT-ASSERTION-OVERLOADS — compatibilitatea capturii cu aserțiunea originală

**P2 · owner Test Kit; reconciliere înaintea adopției binare.** Agregarea componentelor J4 consumă și `assertFalse(boolean, Supplier<String>)`, `assertNull(Object, String)` și `assertSame(Object, Object, String)` din `AuditedAssertions`. Extensiile locale și regresia `messageOverloadsKeepIdentityNullAndLazySupplierSemantics` din `AuditedAssertionsTest` sunt intrări de reconciliere, nu surse de copiat integral peste producător.

- [ ] Verificați echivalența cu overload-ul JUnit: evaluare o singură dată, supplier leneș numai la eșec, identitate prin referință, valoare null și mesajul original; captura nu schimbă verdictul sau cauza.
- [ ] Păstrați primitivele fără Reporting ori cunoașterea lansatorului consumatorului. Împachetarea și acceptanța fiecărui consumator califică separat API-ul reconciliat.

## KIT-ACCEPTANCE-DIAGNOSTICS-123 — reuse scope, preserve causes

**P2 · Test Kit J0; launcherele și oracolele rămân la consumeri.** J2 SqlReportAcceptance afișează unele erori ca DefaultFailure; Eventuate repetă save/restore, iar J3 repetă scope-uri și poate pierde diagnosticul procesului eșuat. ScopedAcceptanceRun și FailureAwareCleanup sunt mecanismele existente; primul scopează o proprietate și colectează artifacte numai după return normal.

- [ ] Demonstrați două adaptoare concrete înainte de extindere: rezultat neutru cu cauza originală, număr de containere/teste, selecție explicită și referințe diagnostice limitate; restaurare exactă inclusiv la Error, publicare și colectare eșuate.
- [ ] Păstrați controale pentru zero teste, container eșuat, assertion așteptat, excepție neașteptată și sink diagnostic eșuat. Eroarea primară rămâne aceeași; capturarea și cleanup-ul sunt secundare.
- [ ] JUnit launching rămâne la adaptor; nu introduceți renderer, Kafka/JDBC, ASR, context global mutabil sau modul nou doar pentru reutilizarea unei bucle. BoundedProcess și CleanupRegistry rămân primitive de producție cu alte contracte.

## KIT-UNICODE-PREVIEW-127 — preserve valid text at the configured boundary

**P1 · AssertionValueDisplay în J0; ValidationTable în J1.** Prefixul ASCII de 238 de unități, un caracter suplimentar și un sufix care declanșează trunchierea produc, la limita 240, un high surrogate izolat; perechea care încape exact este controlul pozitiv. Reporting păstrează text valid; rendererul nu poate repara fidel valoarea deja tăiată de caller.

- [ ] Păstrați ordinea sanitizer → preview și limita implicită 240, exprimată în unități UTF-16; nu separați o pereche surrogate și nu măriți bugetul.
- [ ] Probați ambele poziții ale perechii, limita unu, măști literale, sanitizer eșuat fără callback repetat și comparația originală JUnit. Aplicați aceeași regulă în Validation prin ownerul J1, cu adopție distinctă în consumeri.

## Retenția comparațiilor — coordonare cu Reporting

RecordedAssertions stochează proiecția limitată, iar ValidationTable păstrează expected/actual reduse. FAIL-ul dincolo de preview rămâne corect, dar explicația completă poate lipsi.

- [ ] Coordonați [dovada integrală Reporting](ATF_Android_UI_Reporting_Feedback.md#report-ui-assertion-full-evidence-133--valori-lungi-capturate-înainte-de-preview): captură sigură înaintea trunchierii, limite integrale explicite, fără comparație dublă și fără modificarea preview-ului 240. Controlul negativ are preview-uri identice și un sufix diferit.

## KIT-CONCURRENCY-152 — calificare Jupiter paralelă

**P2 · owner J0; adopție Android la J4.** Proba ScopedComponentAuditExtensionComponentTest dezactivează paralelismul Jupiter; controalele concurente ale primitivelor nu califică singure ciclul real. Criteriul este preluat din J4, fără declararea unei implementări noi.

- [ ] Clase și metode Jupiter concurente: creare/închidere o singură dată per owner, audit distinct per invocare, cauză originală păstrată la pregătire/publicare eșuată. Păstrați separat proprietarii clasă și suită.
- [ ] ScopedAcceptanceRun serializează numai apelurile sale; proba cu cititor extern concurent și eroare injectată trebuie să arate restaurarea și limita izolării proprietăților globale. Nu promiteți izolare globală printr-un monitor local.
