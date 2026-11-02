# Android UI evidence — handoff contract pentru Reporting J10+

## Decizie și boundary

**Prioritate: 88/100** = impact 35 + lacună de handoff 26 + lucrabil 18 + documentare 9.
**Owner:** J0 pentru API-ul neutru, testele Reporting și perechea JAR/POM;
J4 pentru ADB, captură, oracole Contract Whist și adaptarea consumerului.

Acesta este un criteriu de integrare și distribuție, nu o cerere de a muta
Android/ADB în Reporting. J4 trebuie să publice un test E2E în forma UI ATF:
faze `SETUP`, `TEST BODY`, `TEARDOWN`; pași Given/When/Then; acțiuni ordonate;
assertions cu expected/actual/status; screenshot PNG Base64 lângă checkpoint-ul
care l-a produs; și observația vizuală păstrată ca informație separată.

| Boundary observat pe J10 | Contract și lacună |
|---|---|
| API disponibil | UiExtentReportSession.UiPhase.step și uiValidation acceptă assertions, AtfUiActionRecord, captură și capture point; ReportObservation păstrează faptele vizuale, iar ReportEvidenceProfile permite provider custom |
| compoziție J4 | flow-urile folosesc uiValidation cu emptyAssertions/PASS și un sumar generic; nu consumă overload-urile compuse |
| consecință | acțiunile, expected/actual și corespondența screenshot–pas trebuie furnizate de adaptor; nu s-a demonstrat încă o funcție lipsă în bibliotecă |

În J4 există perechea J10 cu SHA-256
`CE527BA085FC216656B7E4DFBF11C71193FBACE2CEE17C2B40E2AEDECB0F0AC9`, importată
din consumatorii existenți. În folderul J0 nu am găsit o pereche JAR/POM
distribuibilă pentru acest handoff. Înainte de orice API nou, probați contractul
actual folosind JAR-ul produs de J0; schimbați suprafața doar dacă proba refuză
o cerință de mai jos.

## Cerințe obligatorii

1. **Checkpoint compus, nu galerie detașată.** Un pas UI trebuie să păstreze
   împreună ordinalul, cuvântul Gherkin, titlul, target-ul, lista de acțiuni,
   assertions și screenshot-ul acelei stări. `AtfUiActionRecord` conține cel
   puțin operation, target, expected, observed, duration și status; ordinea din
   raport trebuie să fie exact ordinea furnizată de consumer.
2. **Comparații auditabile.** Fiecare verificare funcțională se publică drept
   `AtfAssertion(name, expected, actual, passed/resultStatus)`. Un text de sumar
   de tipul „all checks passed” nu substituie comparațiile individuale. Un
   expected absent, actual absent sau verdict contradictoriu nu poate deveni
   PASS prin normalizare în renderer.
3. **Captură legată de pas și embedded.** PNG-ul primit de API trebuie să fie
   Base64 intact și să apară inline în checkpoint-ul potrivit. Linkul local la
   `.png`, URI `file:`, imaginea mutată într-o galerie separată ori markerul
   `data:image/png;base64,` găsit în JavaScript/template nu satisfac criteriul.
   Oracolul compară payload-ul complet al fiecărei capturi cu identitatea
   scenariu/fază/pas.
4. **Observația vizuală nu devine verdict funcțional.** `Visual acceptance
   observation` rămâne `ReportObservation` factual: display, density, versiune,
   defect vizual, captură de referință și limită. Dacă defectul afectează
   acceptanța, consumerul publică WARNING explicit; Reporting nu inventează
   expected-uri UI și nu convertește un FAIL funcțional în PASS/WARNING.
5. **Cauza și dovada parțială supraviețuiesc eșecului.** O excepție la tap,
   timeout, aserțiune sau captură trebuie să publice acțiunile deja consemnate,
   comparațiile efectuate, captura disponibilă și cauza originală. Eșecul
   publisher-ului nu poate masca eroarea business și nu poate șterge checkpoint-
   urile anterioare.
6. **Identitate și agregare.** Rapoartele pot conține separat testul original,
   reluarea jocului și flow-ul aplicației paralele. Titluri similare nu se
   asociază numai după text; Overview, Tests, Fails, taguri și faze păstrează
   aceeași identitate/status/count. Un test are un singur verdict; fiecare
   checkpoint nu devine test separat în totaluri.
7. **Paritate Extent/fallback.** Pentru același snapshot, ordinal, text,
   expected/actual, status, observație și capturi, Extent și rendererul fallback
   păstrează aceeași semantică și navigare. Aspectul vizual poate diferi numai
   prin integrarea publisher-ului deja permisă de contract.
8. **Suprafață neutră și binar real.** Nu adăugați `android.jar`, ADB, emulator,
   Playwright sau așteptări Whist în runtime-ul `atf-reporting`. Providerul de
   captură și oracolele business aparțin consumerului; J0 folosește fixture-uri
   sintetice. Testul de consumer trebuie să compileze și să ruleze contra
   perechii JAR/POM fizice, fără sursa Reporting pe test classpath.
9. **Javadoc fără owner inventat.** `AtfUiActionRecord` este documentat acum ca
   rând „Playwright/audit”. Clarificați explicit că modelul reprezintă o acțiune
   UI neutră, indiferent dacă adapterul folosește Playwright, ADB sau alt
   transport susținut. Nu adăugați enum `ANDROID` doar ca să repetați proveniența
   care aparține metadata-ului/environment-ului consumerului.

## Matricea minimă de probă J0

Proba fixture trebuie să ruleze aceeași secvență atât din source gate, cât și
din consumerul extern al JAR-ului produs:

| Scenariu | Dovadă necesară | Control negativ obligatoriu |
|---|---|---|
| checkpoint funcțional | 2 acțiuni ordonate, 2 assertions distincte, captură Base64 exactă | inversează ordinalul sau elimină numai assertion-ul 2 |
| rezultat negativ | assertion FAIL cu expected/actual și status terminal FAIL; captura rămâne atașată | înlocuiește verdictul numai cu PASS și cere oracolului să refuze |
| observație vizuală | observație neutră + captura de referință + WARNING separat de pass-ul funcțional | mută observația la sibling și verifică identitatea pasului |
| failure intermediar | acțiuni anterioare, captura disponibilă și throwable original în artifact | aruncă la publish/close și verifică să nu dispară cauza business |
| mai multe flow-uri | două scenarii cu screenshot-uri omonime, faze și statusuri diferite | schimbă ordinea/identitatea și verifică Overview, Tests și Fails |
| transport JAR | consumer curat, numai POM+JAR, fără source/output target din J0 | elimină un asset din staging ori pune sursa pe classpath și refuză testul |

Validarea nu se închide cu `html.contains("data:image/png;base64,")`, numărul de
imagini găsite ori un build verde. Se verifică fiecare payload complet,
scenariul lui, acțiunile/assertions și statusurile publicate în DOM-ul activ.

## Handoff cerut către J4

După ce proba de consumer demonstrează API-ul, livrați în `../../../atf_j4/lib`
perechea versionată `atf-reporting-<version>.jar` + `.pom`, SHA-256 și o notă
scurtă de compatibilitate/consum. Dacă nu se schimbă codul/API-ul, nu inventați
un release doar pentru a cosmetiza feedback-ul: consemnați explicit că J10
acoperă contractul și că J4 trebuie să adopte overload-urile existente. Dacă
proba găsește un gol real, adăugați numai extensia neutră minimă și închideți
matricea de mai sus înainte de a cere J4 să o consume.

**Închidere:** contractul UI este demonstrat prin source și JAR; raportul are
acțiuni, assertions, screenshot-uri inline și observația vizuală cu identități
exacte; perechea fizică este hash-uită și consumabilă extern; limitele business
și testul Android rămân în J4.

## Dovezi de referință curente

- J1 UI: `../../../atf_j1/artifacts/reports/ui/ui-atf-practice-shop-e2e_2026-10-31_09-49-29-899.html`.
- J4 baseline verificat (dovezi mutate în afara sursei): `../../../../whist_back/deep-app-test/runs/20261101-222629/final-extent-report.html`.
- J4 flow source: `../../../atf_j4/atf-test-android/src/test/java/atf/android/e2e`.

## 2026-11-01 — partidă completă și dovezi de stare, consumator J11

- J4 a primit perechea fizică J11.
- Proba producerului cu PNG sintetic nu închide acceptanța Android a consumatorului.
- Ținta live este 19 mâini, 109 levate și 436 cărți, apoi aceleași distribuții/oferte/mutări în Whist Lab cu rezultate calculate de motorul local.
- Rularea este în curs: prima mână are 40 de cărți contabilizate și scoruri observate `[10,15,2,3]`, reconciliate independent.
- Scenariul complet nu este încă acceptat.

**Cerințe ferme pentru contract și probă, fără logică Whist în Reporting:**

1. **Proveniență per afirmație.** OBSERVE poate descrie starea produsului și
   calculul independent, pe lângă captura UI. XML UI vechi, pixel schimbat și
   stare salvată proaspătă sunt dovezi diferite. Nu transformați OBSERVE în
   ACTION sau interpretarea într-un fapt citit automat din PNG. Parsarea stării
   și oracolul rămân în J4; modelul Reporting rămâne neutru.
2. **Reproducere din raport.** Inputul, condiția regulii, expected/actual,
   verdictul, captura inline exactă și artifact-ul sursă cu SHA-256 rămân
   corelate cu pasul/scenariul lor. Preview-ul rămâne 240 de caractere;
   comparația integrală trebuie să poată fi deschisă și citită. Listele de
   valori legale nu pot dispărea din HTML.
3. **Numărători stabile.** Testele JUnit, scenariile live, checkpoint-urile,
   assertions, mutările și capturile au populații diferite. Sute de assertions
   nu devin sute de teste în Overview. Un FAIL de regulă/scor rămâne FAIL și
   când acceptanța vizuală este WARNING.
4. **Probă de volum.** Cele două aplicații produc sute de checkpoint-uri cu
   PNG 1080×1920 inline Base64. Măsurați publish/close, dimensiunea HTML,
   memoria și navigarea în browser cu o probă sintetică echivalentă. Verificați
   media și assertions din ultimul pas. Dacă imaginile ascunse sunt decodate
   toate la deschidere, propuneți încărcare la nevoie păstrând HTML-ul autonom
   și payload-urile complete.
5. **Timing neinstrumentat.** Diferențiați observația/calculul fără timing
   instrumentat de o acțiune măsurată ca 0 ms. Dacă suprafața actuală permite
   metadata/observations, documentați folosirea ei înainte de a extinde API-ul.
6. **Continuare auditabilă.** Eșecul harness-ului păstrează trace-ul și cauza.
   Continuarea revalidează checkpoint-urile reținute și păstrează logul inițial.
   Nu asociați capturi după simplul nume de fișier și nu ascundeți proveniența
   fazelor la publicarea verdictului final.

- J4 folosește API-ul existent J11.
- Dacă cerințele sunt acoperite, livrați proba pe JAR și instrucțiuni concrete de consum.
- Dacă apare un gol, numai J0 modifică, testează și împachetează biblioteca.
- Backup-urile `.ab` brute pot conține preferințe ale aplicației; J4 atașează în raport numai starea jocului și dovezile necesare, fără arhivele brute.

## 2026-11-02 — J4: reguli românești, componente și AI înainte de Android

Consumerul J4 adaugă raportul nativ `RuleComponentExtentReportTest`, fără
renderer HTML privat. Profilul românesc este separat de Oh Hell; testele JVM
produc intrări, stări și expected/actual la fiecare carte, fără emulator.
Contractele Reporting necesare pentru acest consum sunt concrete:

1. Overview trebuie să distingă cazuri JUnit, partide model, comparații AI,
   mutări și ramuri JaCoCo. 6.375 configurații dintr-un test nu sunt 6.375
   cazuri JUnit. Nici un pas al unei partide nu devine automat un test nou.
2. Un flow de componentă are SETUP/TEST BODY/TEARDOWN și stări de domeniu, fără
   screenshot. Lipsa unei capturi UI aici este normală, nu warning de produs.
   Când revenim la Android, pozele rămân PNG reale inline Base64.
3. Expected/Actual trebuie să păstreze integral mâna, lista cărților legale,
   atuul, actorul, câștigătorul, levatele și totalurile. Pentru profilul românesc
   scorurile negative sunt valide; formatarea nu trebuie să ascundă semnul.
4. Verdictul AI are două dimensiuni: legalitate și performanță măsurată.
   Eșantionul pereche, seed-ul, locul, adversarii și scorurile trebuie să poată
   fi citite. Un scor mai bun în 32 de perechi nu înseamnă AI optim global.
5. Raportul offline și raportul Android au acceptanțe distincte. Nu completați
   automat cu PASS o execuție Android lipsă pe baza componentelor verzi.

J4 consumă numai JAR-ul J11 livrat de J0. Orice schimbare generică de layout,
numărători, comparație sau navigare rămâne de implementat, verificat și
împachetat în J0. Rularea curentă și verificarea HTML sunt păstrate sub
`../../../../whist_back/rules-components/20261102-final/`.

### Reproducere concretă: observation.fields este doar preview

- Verificarea strictă a consumerului a eșuat în `../../../../whist_back/rules-components/20261102-acceptance/extent-publisher.log`: `Report omitted complete component evidence`, distribuția mâinii 5.
- În HTML, `INPUTS / HAND / LEGAL CHOICES` se termină cu elipsă după 240 caractere; valoarea integrală nu era în raport deoarece consumerul folosise numai ReportField.
- Corecția J4 folosește `ReportObservation.Body(..., Format.TEXT)` pentru intrări/expected/actual complete și păstrează câmpul ca preview.
- Nu ridicăm plafonul global și nu copiem rendererul.
- J0: documentați explicit această semnificație a câmpurilor și adăugați o probă binară pentru corp TEXT de peste 240 caractere, cu semne negative, cărți Unicode și ultimul element vizibil.

- Corecție de configurare confirmată prin proba binară J11: și Body TEXT are un plafon implicit separat de 100 de caractere.
- Consumerul trebuie să ceară `new ReportingSettings(240, ReportingSettings.DISABLE_BODY_DISPLAY_LIMIT)` pentru dovezi complete.
- J4 a făcut asta numai în publisherul său offline; preview-ul rămâne 240.
- Verificarea tuturor valorilor a trecut: `../../../../whist_back/rules-components/20261102-complete/component-report-verification.properties`, 184 cazuri, 88 partide model, 40.304 mutări, HTML 4.604.237 bytes.
- Benchmarkul final are 144 perechi la 3–6 locuri, nu doar proba inițială de 32.
- Această configurare trebuie documentată clar de J0 pentru consumerii cu dovezi lungi; nu este nevoie de un renderer privat sau de modificarea JAR-ului în J4.