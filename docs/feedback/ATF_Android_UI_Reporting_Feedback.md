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

Auditul API-ului J10 din `atf-reporting` nu a arătat până acum o funcție lipsă:
`UiExtentReportSession.UiPhase.step(...)` acceptă assertions, `AtfUiActionRecord`,
captură și capture point; `uiValidation(...)` acceptă același model pentru un
checkpoint vizual; `ReportObservation` poate păstra faptele pentru
`Visual acceptance observation`; `ReportEvidenceProfile` permite provider
custom. J4 nu consumă însă aceste overload-uri: flow-urile atașează screenshot-uri
prin `uiValidation(..., emptyAssertions, PASS)` și publică rezumatul ca o singură
observație generică. Rezultatul final pierde în mare măsură acțiunile, comparațiile
expected/actual și corespondența screenshot–pas.

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

După ce proba de consumer demonstrează API-ul, livrați în `C:\work\1\atf_j4\lib`
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

- J1 UI: `C:\work\1\atf_j1\artifacts\reports\ui\ui-atf-practice-shop-e2e_2026-10-31_09-49-29-899.html`.
- J4 run: `C:\work\1\atf_j4\artifacts\deep-app-test\runs\20261101-195037\final-extent-report.html`.
- J4 flow source: `C:\work\1\atf_j4\atf-test-android\src\test\java\atf\android\e2e`.
