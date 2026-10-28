# ATF UI — sincronizare și recuperare ghidată de backend

## Pe scurt

**Stare: sincronizare nativă și politică BE read-only livrate; readiness-ul
business și auditul agregat rămân deschise.**
Ownerii sunt UI pentru sincronizare, Kit pentru declarația neutră,
consumerii pentru semantica operațiilor și Reporting pentru dovezile randate.

- Primitivele așteaptă prin operația nativă strictă; rămân precondițiile business
  ale tranziției și continuitatea unui select înlocuit înaintea activării.
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
| `UI-LOCATOR-READINESS-01` | 79 = 32 + 23 + 17 + 7 | identitatea și coșul sunt validate înaintea checkout-ului, individual și suite |
| `UI-RECOVERY-AUDIT-01` | 78 = 33 + 22 + 16 + 7 | încercări vizibile, verdict reconciliat și expandare completă |
| `UI-NATIVE-SELECT-DETACH-01` | 74 = 31 + 20 + 16 + 7 | selectul înlocuit poate continua numai cu rezultat confirmat și activare unică |

## UI-LOCATOR-READINESS-01 — așteptare înaintea activării unice

**Owner rămas:** AutoEx deține precondiția business a navigării spre checkout.

- Primitivele și bugetul nativ sunt normate în
  [UI](../../atf-ui/README.md#așteptare-nativă-și-unicitate).
- AutoEx aplică [readiness-ul local al checkout-ului](../../atf-test-auto-ex/README.md#readiness-înainte-de-checkout),
  cu probe pentru control întârziat, absent și ambiguu, fără click repetat.

Restanța de aici rămâne verificarea identității și a coșului înaintea tranziției
și matricea individual/suită. Succesul unui purchase nu probează refuzul unei
identități greșite ori al unui coș pierdut.

- [ ] După Continue, verificați identitatea contului deținut și starea coșului
      înainte de checkout. O cantitate greșită sau o sesiune pierdută rămâne FAIL;
      nu re-adăugați produsul și nu creați alt cont pentru a obține PASS.

**Închidere:** ținta întârziată produce un singur click, ținta ambiguă niciunul,
iar lipsa permanentă păstrează locatorul, documentul și cauza în raport.
Purchase individual și prin suită trebuie să confirme aceeași tranziție;
proba locală singură nu închide diferența remote.

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

## Contractul BE promovat la owneri

Politica și observația pasivă sunt documentate la
[UI](../../atf-ui/README.md#recuperare-be-prin-observație-pasivă), declarația la
[Kit](../../atf-test-kit/README.md#declarația-neutră-pentru-recuperare-be).

Operația catalogului și cheile YAML exacte sunt la
[AutoEx](../../atf-test-auto-ex/README.md#politica-be-a-catalogului) și
[Shop](../../atf-test-practice-shop/README.md#politica-be-a-catalogului).

Acest increment acoperă read-only în documentul curent, cu master OFF,
captură metadata-only opt-in, refuzuri explicite și deadline comun.
Nu activează automat recuperarea în journey-uri și nu autorizează replay
de cont, coș, checkout/payment sau cleanup.

## UI-RECOVERY-AUDIT-01 — încercări inspectabile în Extent

**Owner:** model/proiecție la Reporting; probe renderer la Reporting Tests;
adaptoare și aserțiuni business în main la cei doi consumeri.

- [x] Adaptoarele publică observații INFO cu politica, sursa, bugetul, receipt-ul,
      încercarea, decizia și timpii probe/backoff/total; corpul și credentialele
      nu sunt capturate. Proiecția agregată rămâne separată.
- [ ] Completați modelul/proiecția neutră cu politica efectivă și sursa ei, operația, indexul încercării,
      bugetul, cauza, decizia permis/refuzat, schimbul BE asociat și starea DOM.
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
      Verificați și inputul contradictoriu: un caller care completează PASS după
      o aserțiune UI FAIL poate păstra PASS în semanticSnapshot; consumerii curenți
      furnizează verdictul efectiv, dar proiecția neutră trebuie normată explicit.
- [ ] Probați expand/collapse până la fiecare încercare, acțiune, request și
      aserțiune; Fails → test/fază/pas exact și Overview trebuie să păstreze
      cauzele și totalurile coerente. Captura ulterioară nu înlocuiește starea
      consemnată la eroare.
- [ ] Verificați câte un caz relevant individual și prin suită în AutoEx și
      Shop, cu profil UI și UI+API, succes, recuperare/refuz și epuizare. Probele
      Extent neutre rămân în `atf-reporting-tests`; policy/resolver și adaptoarele
      au teste la owner, fără cuplarea testelor de business la HTML.

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

<details>
<summary><strong>Control tehnic — politica BE read-only</strong></summary>

```yaml
testChanges:
  - id: UI-BACKEND-RECOVERY-POLICY-02
    status: ACTIVE
    base: 74143eeb0c5260aada147fedf67212dc05834eb1
    expires: 2026-11-27
    reason: >-
      Adaugă politică YAML și adnotare neutră pentru reobservare BE read-only:
      master OFF, allowlist semantic, captură pasivă opt-in, deadline unic,
      corelare pe scope și refuz explicit fără replay de business.
      Probele browser și adaptoarele ambilor consumeri verifică dispatch unic,
      refuz/epuizare, INFO și aserțiuni auditabile; păstrează testele anterioare.
    proof: mvn -pl atf-test-kit,atf-ui,atf-test-auto-ex,atf-test-practice-shop,atf-reporting-tests,atf-tests -am "-Dtest=AtfUiRecoveryResolverTest,UiRecoveryConfigurationTest,UiRecoveryLoopTest,UiBackendRecoveryComponentTest,AutoExBackendRecoveryComponentTest,ShopBackendRecoveryComponentTest,BackendRecoveryObservationReportTest,ComponentAssertionStepReportTest,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=74143eeb0c5260aada147fedf67212dc05834eb1" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/recovery/BackendRecoveryObservationReportTest.java
      - atf-test-kit/src/test/java/atf/testkit/recovery/AtfUiRecoveryResolverTest.java
      - atf-ui/src/test/java/atf/ui/config/recovery/UiRecoveryConfigurationTest.java
      - atf-ui/src/test/java/atf/ui/recovery/UiRecoveryLoopTest.java
      - atf-ui/src/test/java/atf/ui/recovery/browser/RecoveryBrowserFixture.java
      - atf-ui/src/test/java/atf/ui/recovery/browser/UiBackendRecoveryComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/recovery/AutoExBackendRecoveryComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/extension/AutoExRunFactory.java
      - atf-test-auto-ex/src/test/resources/atf-auto-ex.yaml
```

</details>
