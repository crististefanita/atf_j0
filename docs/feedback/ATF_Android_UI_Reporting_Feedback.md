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

J4 primește perechea J13 distribuită de J0; [dovada J13](../ATF_Reporting_J13_Verification.md) separă proba neutră de acceptanțele istorice J12.
Aceasta nu închide matricea completă de mai jos sau acceptanța fizică Android.

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


## Restanță generică și handoff

- Proba sursă/JAR pentru corp TEXT peste 240 păstrează semne negative, Unicode și ultimul element integral, cu body display limit dezactivat explicit. Câmpurile rămân preview 240; limita corpului este separată.
- Proba sintetică de volum măsoară publicare, dimensiune, memorie și navigare pentru sute de checkpoint-uri cu PNG inline; verifică ultimul pas și cauza la eșecul publisherului.
- Numărătorile JUnit, scenariile, acțiunile și assertions rămân populații distincte; un FAIL funcțional nu este convertit de observația vizuală.

[Cerințele device/business și handoff-urile datate](../../../atf_j1/docs/feedback/ATF_Consumer_Reporting_Handoff_Feedback.md) sunt coordonate în J1, cu implementare și acceptanță în J4. J0 nu păstrează reguli Whist, seed-uri sau restanțe de execuție Android.

## REPORT-PUBLICATION-MEMORY-131 — copii integrale la publicare

Rezolvat în [J13](../ATF_Reporting_J13_Verification.md): spool privat, restaurare streaming,
321 PNG complete și ultima comparație, cu proba reală sintetică la 1 GiB pe sursă/JAR.
Măsurătorile și intrarea istorică J4 sunt în documentul de verificare; heap 3 GB nu este proba închiderii.

## REPORT-ACTION-FULL-EVIDENCE-132 — expected/observed dincolo de preview

Rezolvat pentru ACTION/OBSERVE în [contractul modulului](../../atf-reporting/README.md#actionobserve--preview-și-valori-complete).
Regresiile permanente verifică 240 plus valoarea completă redactată, suffix eliminat,
owneri omonimi, keyboard și randări fără suprapunere în ambele renderere, pe sursă/JAR.

## REPORT-UI-ASSERTION-FULL-EVIDENCE-133 — valori lungi capturate înainte de preview

**Prioritate: 88/100.** Criteriu distinct rezultat din completarea handoff-ului J4;
celulele ACTION/OBSERVE rezolvate nu îl închid.

- [ ] Delimitați valoarea integrală capturată de preview-ul callerului: Kit poate transmite deja numai text redus, pe care Reporting nu îl poate reconstitui.
- [ ] Definiți proiecția sigură pentru expected/actual UI lungi, cu preview 240 și disclosure corelat la scenariu/fază/checkpoint/comparație; fără reevaluare sau secrete ascunse.
- [ ] Păstrați regresie neutră în Reporting Tests: numai ultimul scalar eliminat, sibling omonim, verdict FAIL după 240, Unicode și redacție, Extent/fallback sursă/JAR.
- [ ] Verificați Escape/colapsare, revenire la aceeași comparație și ultimul rând; un tabel scurt cu ellipsis singur nu reconstruiește dovada.

J4 păstrează temporar corpurile TEXT native cu body limit explicit pentru valorile complete.
Adopția și proba aplicației rămân în [handoff-ul consumerului](../../../atf_j1/docs/feedback/ATF_Consumer_Reporting_Handoff_Feedback.md), fără assertions PASS artificiale.

Coordonarea accesului ADB este la [ownerul consumerului J1/J4](../../../atf_j1/docs/feedback/ATF_Consumer_Reporting_Handoff_Feedback.md#coordonarea-probei-j4-pe-dispozitiv-partajat--2026-11-02-1533); probele generice Reporting nu folosesc dispozitivul.


## 2026-11-02 — J4: captură fără comparație business, ruta THEN/OBSERVE (J13 înghețat)

Reproducere concretă: `C:/work/whist_back/ai-training/20261102-143525/publication-attempt-01/`. Raport J13 nativ, EACH_CHECKPOINT, 352 checkpoint-uri și verificare SHA-256 a PNG-urilor decodate. Opt viewport-uri Instructions lipsesc; toate sunt THEN, assertions=[], un action-record de tip OBSERVE și PNG valid. Capturile cu verificări și toate cadrele partidei sunt păstrate. Nu presupun că J15 are aceeași problemă; această observație este despre perechea binară înghețată J13.

J4 corectează compoziția: folosește API-ul public `uiValidationAsMedia` pentru captura observațională fără assertions, păstrând OBSERVE și fără comparații business inventate. Adaugă un contract consumer dedicat și păstrează controlul complet pe bytes PNG. Nu există patch HTML și nu se copiază cod Reporting.

Cerință J0: explică/testează explicit diferența dintre `step(THEN, PNG, assertions=[], OBSERVE)` și ruta media. O captură cerută prin policy nu trebuie pierdută în tăcere. Verifică INDIVIDUAL/UNIFIED_FLOW și observații fără ACTION; dacă ruta THEN este deliberat outcome-only, documentează limita și oferă diagnostic, nu recomanda assertions fictive pentru afișarea imaginii. Numărătorile trebuie separate de deduplicare: lipsa a opt hash-uri distincte nu este o optimizare de stocare.

J4 mai publică rezultatele materiale de componentă ca validări native expected/actual: 15.336 comparații de scor/serie/contor din 5.112 intrări și 425 stări din flow-ul complet independent. Inventarul JUnit rămâne metadata; nu este prezentat ca substitut al acestor comparații. Verdictul semantic Android rămâne J4.


## J4 — verificare consumer J15, 2 noiembrie 2026

Run `C:/work/whist_back/ai-training/20261102-round3/`: Core C3 / Reporting J15, fără modificări în bibliotecă. Publicarea nativă a trecut cu3.222comparații, replay independent complet și3PNG istorice exacte inlineBase64 prin `uiValidationAsMedia`, fără aserțiuni inventate. Experimentele respinse sunt WARNING și păstrează REGRESSION; succesul verificării HTML nu le transformă în PASS. Receipt:`corpus-report-verification.properties`; raport:`corpus-extent-report.html`. Această rulare confirmă ruta publică media pe J15; nu este dovadă de defect J15 pentru ruta THEN implicită discutată în observația J13 anterioară. Nu cerem un renderer separat sau copierea surselor Reporting în J4.
