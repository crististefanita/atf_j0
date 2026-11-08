# Test Kit — bibliotecă comună, owner J0

## Pe scurt

J0 deține codul, API-ul, testele native, împachetarea și feedback-ul Test Kit. J1–J6 păstrează integrarea și oracolele lor; consumă perechea verificată din lib, fără surse ori native Kit paralele. [Contractul bibliotecii](../ATF_Test_Kit_Library.md) descrie distribuția și acceptanțele distincte.

<a id="kit-library-151--distribuție-unică-fără-surse-paralele"></a>

[Contractul bibliotecii](../ATF_Test_Kit_Library.md) păstrează distribuția, scope-urile, ghidul local și acceptanțele independente. Codul, nativele și guvernanța implementării rămân în J0; consumerii verifică binarul și oracolele proprii.

<a id="kit-assertion-overloads--compatibilitatea-capturii-cu-aserțiunea-originală"></a>

[Contractul aserțiunilor](../../lib/README.TestKit.md#scope-și-integrare) păstrează semantica JUnit, diagnosticul lazy și scope-ul adaptorului; regresiile native aparțin J0.

## KIT-ACCEPTANCE-DIAGNOSTICS-123 — reuse scope, preserve causes

**P2 · Test Kit J0; launcherele și oracolele rămân la consumeri.** J2 SqlReportAcceptance afișează unele erori ca DefaultFailure; Eventuate repetă save/restore, iar J3 repetă scope-uri și poate pierde diagnosticul procesului eșuat. ScopedAcceptanceRun și FailureAwareCleanup sunt mecanismele existente; primul scopează o proprietate și colectează artifacte numai după return normal.

- [ ] Demonstrați două adaptoare concrete înainte de extindere: rezultat neutru cu cauza originală, număr de containere/teste, selecție explicită și referințe diagnostice limitate; restaurare exactă inclusiv la Error, publicare și colectare eșuate.
- [ ] Păstrați controale pentru zero teste, container eșuat, assertion așteptat, excepție neașteptată și sink diagnostic eșuat. Eroarea primară rămâne aceeași; capturarea și cleanup-ul sunt secundare.
- [ ] JUnit launching rămâne la adaptor; nu introduceți renderer, Kafka/JDBC, ASR, context global mutabil sau modul nou doar pentru reutilizarea unei bucle. BoundedProcess și CleanupRegistry rămân primitive de producție cu alte contracte.

## Retenția comparațiilor — coordonare cu Reporting

RecordedAssertions stochează proiecția limitată și oferă observerul opțional Projection pentru valorile redactate integrale. O eroare de captură păstrează cauza și verdictul pe fază; ValidationTable și politica de retenție a consumerului rămân criterii distincte.

- [ ] Coordonați [dovada integrală Reporting](ATF_Android_UI_Reporting_Feedback.md#report-ui-assertion-full-evidence-133--valori-lungi-capturate-înainte-de-preview): captură sigură înaintea trunchierii, limite integrale explicite, fără comparație dublă și fără modificarea preview-ului 240. Controlul negativ are preview-uri identice și un sufix diferit.

## KIT-CONCURRENCY-152 — calificare Jupiter paralelă

**P2 · owner J0; adopție Android la J4.** Proba ScopedComponentAuditExtensionComponentTest dezactivează paralelismul Jupiter; controalele concurente ale primitivelor nu califică singure ciclul real. Criteriul este preluat din J4, fără declararea unei implementări noi.

- [ ] Clase și metode Jupiter concurente: creare/închidere o singură dată per owner, audit distinct per invocare, cauză originală păstrată la pregătire/publicare eșuată. Păstrați separat proprietarii clasă și suită.
- [ ] ScopedAcceptanceRun serializează numai apelurile sale; proba cu cititor extern concurent și eroare injectată trebuie să arate restaurarea și limita izolării proprietăților globale. Nu promiteți izolare globală printr-un monitor local.
