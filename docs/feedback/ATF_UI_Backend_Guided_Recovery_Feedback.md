# ATF UI — sincronizare și recuperare ghidată de backend

## Pe scurt

**Stare: sincronizare nativă și politică BE read-only livrate; proiecția typed
a observațiilor, continuitatea selectului și izolarea subscriberilor rămân deschise.**
Ownerii sunt UI pentru sincronizare, Kit pentru declarația neutră,
consumerii pentru semantica operațiilor și Reporting pentru dovezile randate.

- Primitivele așteaptă prin operația nativă strictă. Continuitatea selectului
  rămâne la UI, iar precondițiile business ale checkout-ului sunt la consumer.
- Reluarea întregului test, a creării contului, a coșului ori a plății rămâne
  exclusă din acest increment; un rezultat necunoscut al mutației oprește replay-ul.

Contractele existente sunt în [UI](../../atf-ui/README.md),
[AutoEx](../../atf-test-auto-ex/README.md#coeziunea-suportului-autoex)
și [Kit](../../atf-test-kit/README.md). Căutarea catalogului folosește
[clickul unic cu postcondiție](../../atf-ui/README.md#click-unic-cu-postcondiție-explicită),
cu [semantica AutoEx](../../atf-test-auto-ex/README.md#confirmarea-căutării-în-catalog).
Acest feedback deține diferențele rămase,
nu schimbă acele contracte prin simpla propunere a unei configurații.

## Priorități și dovadă

Scorul este prioritate: impact /40 + lacună /30 + lucrabil /20 + datorie
documentară /10. Nu reprezintă o notă a codului sau un procent de reproducere.

| ID | Scor /100 | Criteriul care lipsește |
|---|---:|---|
| `UI-RECOVERY-AUDIT-01` | 78 = 33 + 22 + 16 + 7 | încercări vizibile, verdict reconciliat și expandare completă |
| `UI-NATIVE-SELECT-DETACH-01` | 74 = 31 + 20 + 16 + 7 | selectul înlocuit poate continua numai cu rezultat confirmat și activare unică |
| `UI-OBSERVER-DELIVERY-02` | 69 = 28 + 18 + 16 + 7 | primul subscriber eșuat împiedică notificarea celui sănătos |

## UI-LOCATOR-READINESS-01 — așteptare înaintea activării unice

Contractul nativ este la [UI](../../atf-ui/README.md#așteptare-nativă-și-unicitate).
Restanța identității/coșului aparține exclusiv
[feedback-ului consumerilor](ATF_Consumer_Business_Test_Focus_Feedback.md#ui-locator-readiness-01--identitate-și-coș-înaintea-checkout-ului),
unde sunt criteriile business și matricea individual/suită.

## UI-NATIVE-SELECT-DETACH-01 — continuitate după înlocuirea selectului

**Owner:** integrarea Playwright din `atf-ui`; prioritate **74/100**.
În driverul fixat la `1.49.0`, un select dezactivat înlocuit în timpul așteptării
poate întoarce `[]`, fără excepție, fără eveniment și fără valoarea cerută.

- ATF respinge rezultatul gol, păstrând FAIL și selectorul. Continuitatea
  automată rămâne nedemonstrată; guard-ul nu reexecută selecția.
- Implementarea upstream a
  [`_selectOption`](https://github.com/microsoft/playwright/blob/v1.49.0/packages/playwright-core/src/server/dom.ts#L524)
  nu propagă rezultatul de detach înainte să returneze lista locală.
- Proba locală distinge înlocuirea cu o țintă de înlocuirea cu două ținte.

- [ ] Evaluați corecția upstream sau un adaptor bounded care dovedește că prima
      activare nu a avut loc; nu deduceți permisiunea de replay numai dintr-un timeout.
- [ ] Pentru înlocuire unică, confirmați valoarea și exact un eveniment; pentru
      înlocuire ambiguă ori rezultat necunoscut, păstrați zero activări și FAIL.
- [ ] Verificați și lipsa permanentă, document nou, select sensibil și redacția;
      o schimbare de driver cere regresiile pointer/iframe și rapoartelor existente.

## UI-RECOVERY-AUDIT-01 — încercări inspectabile în Extent

**Owner:** model/proiecție la Reporting; probe renderer la Reporting Tests;
adaptoare și aserțiuni business în main la cei doi consumeri.

Observațiile INFO livrate sunt normate la
[Reporting](../../atf-reporting/README.md#decizii-be-fără-verdict-de-succes-implicit).
`UiRecoveryTrace` deține deja operația, încercarea, decizia, motivul, semnalul BE
și duratele; adaptoarele AutoEx/Shop le publică în rânduri INFO. Restanța este
proiecția structurată comună și matricea de acceptanță, fără parsarea mesajelor
sau repetarea deciziei la renderer.

- [ ] Compuneți faptele typed din trace, policy și signal în proiecția comună:
      operație, încercare, decizie/motiv, sursa politicii, bugete și timpi măsurați.
      Signal-ul păstrează deja scope-ul invocare/operație/frame/document și secvența;
      rendererul primește corelarea neutră de la adaptor, fără parsarea mesajului.
      Starea DOM apare numai dacă a fost observată; lipsa ei rămâne necapturată,
      fără acțiuni sau capturi suplimentare pentru completarea tabelului.
      Distingerea observației de replay trebuie să fie vizibilă în raport.
      Separați durata încercării, backoff-ul și timpul total măsurat; o durată
      indisponibilă rămâne explicit necapturată, fără valoare HTTP inventată.
- [ ] Păstrați toate cauzele intermediare și capturile permise. Recuperarea
      înaintea eșecului terminal poate avea rânduri INFO explicite, conform
      recorderului existent; nu rescrieți un FAIL deja publicat în PASS și nu
      înlocuiți raportul original cu artifactul unei rulări ulterioare.
- [ ] Definiți verdictul logic și indicatorul de recuperare: un singur test
      în totaluri, succes inițial distinct de recuperare, epuizare FAIL și abort
      păstrat SKIP. Retry-ul infrastructurii de publicare nu rerulează business-ul.
      Inputul contradictoriu este reconciliat conform
      [contractului comun Reporting](../../atf-reporting/README.md#consistența-statusului-transmis-de-caller).
- [ ] Probați expand/collapse până la fiecare încercare, acțiune, request și
      aserțiune; Fails → test/fază/pas exact și Overview trebuie să păstreze
      cauzele și totalurile coerente. Captura ulterioară nu înlocuiește starea
      consemnată la eroare.
- [ ] Verificați câte un caz relevant individual și prin suită în AutoEx și
      Shop, cu profil UI și UI+API, succes, recuperare/refuz și epuizare. Probele
      Extent neutre rămân în `atf-reporting-tests`; policy/resolver și adaptoarele
      au teste la owner, fără cuplarea testelor de business la HTML.

## UI-OBSERVER-DELIVERY-02 — izolarea notificărilor de captură

**Prioritate: 69/100** = impact 28 + lacună 18 + lucrabil 16 + documentare 7.
**Owner:** `UiNetworkInspector`; consumerii păstrează selecția și verdictul business.

Proba browser controlată înscrie un subscriber care aruncă și apoi unul sănătos,
execută un fetch 200 și citește rezultatele:

| Limită observată | Rezultat |
|---|---|
| Browser | fetch 200 terminat normal |
| Journal raw | retained=1, dropped=0, complete=true |
| Subscriber sănătos, înscris după cel eșuat | zero notificări |

`subscribers.forEach(...)` se oprește la prima eroare;
`observeFinished()` tratează acea eroare ca diagnostic browser.

Contorii raw descriu corect retenția; problema este livrarea către adaptoare.
Proba existentă pentru subscriber eșuat verifică păstrarea observației, fără
a verifica un subscriber ulterior. Contractul actual este la
[UI](../../atf-ui/README.md#inspectorul-de-rețea--limită-generică-de-responsabilitate).

- [ ] Izolați notificările astfel încât un subscriber eșuat să nu împiedice
      ceilalți consumatori ai aceleiași observații; definiți explicit politica erorii.
- [ ] Păstrați cauza primară și diagnosticul subscriberului, distinct de o eroare
      de transport sau pierdere a observației. Nu modificați artificial contorii raw.
- [ ] Probați două ordine de înscriere, mai multe erori și închiderea contextului,
      prin `capture(...)` public și hook-ul browser, fără replay ori excepții înghițite
      care ar pretinde livrare reușită către toate adaptoarele.
- [ ] Păstrați redacția înaintea notificării, plafonul jurnalului și independența
      de Reporting/Shop/AutoEx; diagnosticul proiectat păstrează limita 240.

**Închidere:** observația reținută ajunge la subscriberii sănătoși, iar eșecul
unuia are diagnostic explicit și nu schimbă verdictul acțiunii UI.

## Contracte păstrate și criteriu de închidere

Auditul componentelor are deja evaluare JUnit unică și pas colapsabil standard,
conform [ghidului de testare](../ATF_Testing_Guide.md#auditul-componentelor-consumerilor).
Bugetul redactat de 240, paritatea rendererelor și lizibilitatea dovezilor sunt
contracte [Reporting](../../atf-reporting/README.md#proiecția-sigură-a-aserțiunilor),
inclusiv în [panouri înguste](../../atf-reporting/README.md#lizibilitatea-dovezilor-în-panouri-înguste).
Aceste invariante rămân obligatorii pentru viitoarele încercări de recuperare.

Politica validată și exemplele livrate sunt la ownerii UI, Kit, Reporting și
consumeri, cu precedența documentată în
[ghidul adnotărilor](../ATF_Test_Infrastructure_Annotations.md).
[Geometria iframe-urilor](ATF_UI_Browser_Geometry_Feedback.md) are criteriu separat.
Verificarea și publicarea urmează [procesul izolat](../ATF_Build_And_Execution_Safety.md);
o repetare manuală parțială nu închide criteriile de recuperare rămase.

Controlul politicii BE livrate este în
[baseline-ul verificat](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate).

## Acceptanță pentru reevaluarea UI

- Izolarea subscriberilor păstrează observația raw și diagnosticul fiecărei
  livrări defecte; contorul de retenție nu certifică recepția în consumer.
- Identitatea cererii înaintea răspunsului și scope-ul frame/document/invocare
  se probează cu cerere veche întârziată și acțiune nouă, la boundary-ul generic;
  semantica order și expected-ul rămân în Shop.
- Politica existentă de recuperare nu devine retry al întregului test. Selecția
  și geometria păstrează activarea unică, iar rezultatul necunoscut al unei
  mutații rămâne refuz, nu permisiune de replay.
- Rulați probele locale înaintea câte unui caz consumer relevant; browserul și
  Reporting nu intră în Kit, iar adaptorul business nu repară rendererul prin CSS local.

Nota se revede prin [rubrica de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor),
cu criteriile independente închise la owner; o singură componentă verde nu
certifică toate adaptările consumerilor.
