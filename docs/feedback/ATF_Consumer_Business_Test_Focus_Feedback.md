# ATF — teste business simple și suport la owner

Ținta este un test scurt care exprimă intenția, un runtime reutilizabil și
suite ușor de lansat. Aceasta este o cerință de refactorizare, nu o mutare deja
implementată și nici reducerea verificărilor business.

- În `src/test`: teste business, suite și numai adaptoarele/fixture-urile test-only inevitabile.
- În `src/main`: capabilități reale ale ownerului, nu un pachet `support` mutat mecanic.
- Fără modul nou: Kit existent pentru lifecycle neutru, UI pentru browser,
  Reporting pentru model/publicare și Reporting Tests pentru probele neutre.

## Ordinea refactorizării

Scorul este prioritate: impact /40 + lacună /30 + lucrabil /20 + datorie
documentară /10. Nu reprezintă o notă a codului sau un rezultat de test.

| ID | Scor /100 | Intervenție |
|---|---:|---|
| `CONSUMER-FLOW-LAYERS-09` | 88 = 36 + 24 + 20 + 8 | business → UI Flow → acțiuni compuse → operații instrumentate; gate-uri și ownership explicite |
| `CONSUMER-EVIDENCE-CONFIG-04` | 80 = 30 + 22 + 20 + 8 | configurare completă a evidence-ului și Overview fidel profilurilor |
| `AUTOEX-LIVE-AVAILABILITY-07` | 78 = 30 + 20 + 20 + 8 | corelarea răspunsului mutativ cu UI, fără replay |
| `CONSUMER-RUNTIME-BOUNDARY-03` | 77 = 31 + 18 + 20 + 8 | separarea responsabilităților rămase în Shop |
| `CONSUMER-TEST-INTENT-05` | 67 = 27 + 18 + 16 + 6 | compoziție declarativă cu resolver de evidence verificat |
| `REPORTING-LAUNCHER-06` | 58 = 23 + 14 + 17 + 4 | un launcher canonic pentru aceeași selecție browser |
| `CONSUMER-E2E-PROFILES-01` | 57 = 24 + 10 + 17 + 6 | reducerea repetării live numai după echivalență demonstrată |

Contractele livrate de scope, runtime și prezentare sunt în README-urile
[Shop](../../atf-test-practice-shop/README.md),
[AutoEx](../../atf-test-auto-ex/README.md) și
[Kit](../../atf-test-kit/README.md). Aici rămân diferențele de implementat.

## CONSUMER-FLOW-LAYERS-09 — business, traseu UI și validare separate

**Prioritate:** 88/100 = impact 36 + lacună 24 + lucrabil 20 + documentare 8.
**Owner:** Shop/AutoEx pentru business și adaptoare; UI, Reporting și Kit pentru
capabilitățile neutre existente. Este un contract țintă, încă nelivrat.

```text
Test E2E → Business Flow → port îngust de operații
                                  ↓ implementat de
                               UI Flow → Page Object / acțiune compusă
                                              ↓
                                  PlaywrightActionSequence → Playwright
Observație → snapshot → validator → gate înaintea următoarei mutații
```

O metodă precum „completează adresa” poate rămâne în Page Object; fiecare
săgeată nu cere o clasă, un pachet sau un modul nou. Straturile interne nu
devin automat niveluri suplimentare în raportul Extent.

<details>
<summary><strong>Constatări în codul curent și limitele concluziei</strong></summary>

| Sursă | Constatare și consecință |
|---|---|
| [ShopPurchaseFlow](../../atf-test-practice-shop/src/main/atf/shop/flow/ShopPurchaseFlow.java), [ShopPurchaseJourneyContext](../../atf-test-practice-shop/src/main/atf/shop/flow/ShopPurchaseJourneyContext.java) | primesc Page, recorder, configurație completă, observer și jurnal API; etapele nu sunt încă un business flow independent de browser/reporting |
| [AutoExPurchaseSession](../../atf-test-auto-ex/src/main/java/atf/autoex/support/journey/AutoExPurchaseSession.java) | expune operații UI, Page, checkpoint, capturi, API și cleanup; Page Objects primesc un port mai larg decât necesarul lor |
| [AccountPage](../../atf-test-auto-ex/src/main/java/atf/autoex/page/AccountPage.java), [AutoExPurchaseJourney](../../atf-test-auto-ex/src/main/java/atf/autoex/support/journey/AutoExPurchaseJourney.java) | starea creare/ștergere și callback-ul de cleanup aparțin unei instanțe Page Object, iar journey-ul folosește instanțe diferite; există risc de stare fragmentată, de confirmat prin probă negativă |
| [CheckoutPage](../../atf-test-auto-ex/src/main/java/atf/autoex/page/CheckoutPage.java) | descarcă și citește factura, aplică reguli business și publică un checkpoint; transportul artifactului, validarea și evidence-ul sunt amestecate |
| [ShopCheckoutPurchaseStage](../../atf-test-practice-shop/src/main/atf/shop/flow/ShopCheckoutPurchaseStage.java) | `apiEvidenceRequired()` alege completarea explicită ori lookup-ul adresei; canalul de evidence modifică traseul UI |
| [PlaywrightActionSequence](../../atf-ui/src/main/java/atf/ui/playwright/PlaywrightActionSequence.java) | operațiile atomice, recorderul și delegarea există; nu justifică încă un wrapper echivalent pentru fiecare click |

- Sunt constatări statice; nu demonstrează un incident nou de ștergere repetată.
- Mutarea unui Run în main sau redenumirea package-ului nu închide separarea
  semantică. `CONSUMER-RUNTIME-BOUNDARY-03` privește separat infrastructura rămasă.

</details>

### Contractul țintă al straturilor

| Strat | Input / rezultat | Limită |
|---|---|---|
| Test E2E | scenariu, precondiții, așteptări independente și rezultat terminal | exprimă intenția; nu asamblează browser, selectori sau HTML |
| Business Flow | date business și un port îngust; rezultat tipizat | ordonează pașii și gate-urile; fără Page/Locator, Page Objects, recorder ori sesiune de raport |
| UI Flow | implementează portul prin navigare și pagini; întoarce snapshot-uri observate | sincronizarea și citirea DOM rămân aici; fără alegerea politicii business din canal |
| Page Object / acțiune UI compusă | o intenție coerentă de ecran | grupează select/fill/click/wait; nu deține întregul cont, verdictul facturii sau publicarea |
| Operație atomică instrumentată | țintă UI, acțiune și postcondiție tehnică | reutilizează executori/recorder din UI, cu o singură înregistrare a acțiunii |

Portul poate avea un rol precum `PurchaseOperations`; acesta este un nume
orientativ, nu un API livrat. Nu expune Run/Context, configurația completă ori
un `Object` care ascunde aceleași dependențe.

### Validările sunt independente și executate la momentul potrivit

| Categorie | Owner | Rezultat și moment |
|---|---|---|
| Tehnică UI | operația / UI Flow | control vizibil/activ, navigare sau modal confirmat; înainte de citire ori continuare |
| Contract de ecran | cititor DOM + validator de snapshot | structură, câmpuri și identitate observate; validatorul nu navighează și nu publică |
| Business | validator pur + gate în Business Flow | produs, cantitate, preț/total, client, comandă și factură; înaintea următoarei mutații dependente |
| API suplimentară | validator API și adaptor de evidence | compară request/response observate; verdict distinct de cel UI, sub checkpoint-ul cauzal |

- Snapshot-ul nu își completează valorile lipsă din expected. Cantitățile,
  sumele și identitățile au tipuri și proveniență explicite; validarea nu
  folosește preview-ul trunchiat al raportului ca observație completă.
- În profilul compact se schimbă publicarea, nu momentul gate-urilor.
  Verificarea finală confirmă comanda/factura, fără a înlocui validările intermediare.
- Evaluați contractele neutre existente din Core/Validation înainte de un nou
  model de verdict. Business-ul nu importă publisheri, Extent/HTML, JUnit ori Playwright.

<details>
<summary><strong>ToDo-uri în ordinea implementării și probele de acceptanță</strong></summary>

- [ ] **09-A — baseline comportamental.** Inventariați operațiile, observațiile,
      gate-urile, precondițiile și mutațiile fiecărui traseu guest/autentificat.
      Păstrați selecțiile de suite și toate cazurile/aserțiunile existente.
      Includeți de la început proba de cleanup cu instanțe Page Object diferite.

- [ ] **09-B — pilot pe coș.** Extrageți un singur traseu complet:
      Business Flow → port → UI Flow → Page Object → recorder atomic.
      Întoarceți un snapshot real al coșului și păstrați temporar fațada existentă;
      nu creați un BaseE2eTest universal sau un modul nou.

- [ ] **09-C — snapshot și gate pur.** Separați observația de expected și
      validarea de navigare/publicare. Probați produs greșit, cantitate/total
      greșit și câmp lipsă; nici checkout-ul, nici plata nu pornesc după gate FAIL.

- [ ] **09-D — politică business independentă de evidence.** Declarați separat
      completarea explicită/lookup-ul adresei și sursa contului. Pentru același
      scenariu/politică, UI și UI_API păstrează aceeași intenție și aceleași
      mutații UI; capturile/gruparea nu aleg traseul.
      O precondiție API suplimentară este declarată în SETUP, nu ascunsă în flow;
      nu pretindeți că toate profilurile au același trafic ori aceleași validări.

- [ ] **09-E — checkout și factură.** Migrați Shop după pilot, apoi separați
      în AutoEx descărcarea/citirea artifactului, snapshot-ul facturii,
      validatorul pur și publicarea. O plată verificată nu este comandă finală;
      păstrați Confirm, numărul/confirmarea comenzii și regulile facturii.

- [ ] **09-F — lifecycle de cont pe execuție.** Scoateți ownership-ul și starea
      creare/ștergere din Page Object. Același owner urmărește toate instanțele
      paginilor și cleanup-ul; fixture-ul împrumutat nu se șterge.
      Probați două Page Objects, create incert, identitate greșită, delete început
      fără confirmare și eșec ulterior: cleanup-ul nu repetă mutația.
      Nu deduceți idempotency numai din GET/POST ori numele butonului.

- [ ] **09-G — adaptor de evidence separat.** Păstrați identitatea checkpoint-ului,
      ordinea acțiunilor și fereastra cauzală a exchange-ului. UI/API rămân frați,
      retry-ul aceluiași exchange rămâne în Execution, iar captura aparține stării
      observate. Nu dublați evenimentele la fiecare strat și nu creați un card
      Extent pentru fiecare metodă internă.
      Dacă publicarea eșuează, păstrați explicit failure-ul de infrastructură;
      un failure business anterior rămâne primar, fără replay sau PASS fabricat.

- [ ] **09-H — probe și guvernanță înaintea migrării generale.** Business Flow
      se probează cu un port fals controlat, UI Flow cu DOM/browser local,
      iar publisherul generic în Reporting Tests cu date neutre.
      Probați ordinea, gate FAIL, observație lipsă, timeout, mutație incertă și
      eșec simultan de business/publicare/cleanup.
      Guard-ul noului strat business respinge importuri și semnături care expun
      Playwright, UI, JUnit sau Reporting; includeți o probă negativă a guard-ului.
      Rulați apoi componentele, guvernanța neschimbată și E2E-urile relevante;
      o probă locală nu certifică aplicația externă.

</details>

### Limita extragerii și închiderea cerinței

- Business/UI Flow și modelele aplicației rămân în main la consumer;
  extensia, suitele, fixture-urile artificiale și oracolele sunt în test.
- UI deține mecanica browserului, Reporting publicarea, iar Kit existent
  lifecycle-ul neutru. Porturile business nu se mută în Kit pentru similitudine de nume.
- Extrageți o capabilitate comună numai după demonstrarea aceluiași contract;
  păstrați limita de opt clase per pachet și evitați ierarhii sau clase triviale.
- Închiderea cere mapare vechi → nou, cazuri/aserțiuni păstrate, failure-path
  verificat, inventar de suite echivalent și README-uri owner aliniate.
  Forma raportului urmează contractul livrat, fără copierea ierarhiei codului.

## CONSUMER-EVIDENCE-CONFIG-04 — același business, configurare completă

**Owner:** consumerii rezolvă configurația și declarațiile; Reporting deține
modelul și publicarea. Canalul, gruparea, capturile și acțiunile sunt axe distincte.

| Lacună | Sursa curentă | Criteriu de închidere |
|---|---|---|
| Grupare configurată | [AutoExEvidenceProfileAdapter](../../atf-test-auto-ex/src/test/java/atf/autoex/tests/extension/AutoExEvidenceProfileAdapter.java) mapează `CONFIGURED` la `SCREEN_CHECKPOINTS`; [ShopScreenshotPolicy](../../atf-test-practice-shop/src/main/atf/shop/reporting/ShopScreenshotPolicy.java) construiește aceeași bază | flow-ul implicit vine din configurația rezolvată, nu dintr-un default ascuns |
| Detaliile acțiunilor | Shop are override prin `@WithE2eHarness`; nu există un plan YAML complet pentru ambii consumeri | configurare per axă, cu precedență și conflict explicit înaintea resurselor |
| `CONSUMER-OVERVIEW-PROFILE-08` | System-ul raportului Shop partajat păstrează profilul primului scenariu, inclusiv API/capturi, deși frații pot fi UI-only sau unified | valorile comune sunt corecte pentru întregul artifact; diferențele sunt declarate ca mixte sau mutate la scenariul proprietar |

- [ ] Rezolvați un plan imutabil per scenariu înainte de browser/report/mutații:
      bază configurată, override explicit, diagnostic și canale permise.
- [ ] Extindeți YAML-ul numai împreună cu loaderul și mapping-ul verificat.
      `CONFIGURED` moștenește baza reală; canalul declarat nu este suprascris tacit.
- [ ] Separați și comportamentul de prezentare: alegerea billing explicit/lookup
      nu aparține canalului API. Contractul și proba diferenței sunt în
      `CONSUMER-FLOW-LAYERS-09-D`, fără chei YAML pretinse ca deja livrate.
- [ ] Probați combinațiile și izolarea fraților în același artifact pe succes și
      failure, inclusiv Overview/System. Precondiția API standalone nu dovedește
      autentificarea sesiunii browser.
- [ ] Păstrați contractele existente: UI-only fără API evidence/inspector,
      unified fără subpropoziții publicate, captură terminală după comandă și
      UI/API sub checkpoint-ul cauzal. Detaliile sunt în
      [contractul de captură](../ATF_Evidence_Capture_Contract.md).

## AUTOEX-LIVE-AVAILABILITY-07 — indisponibilitatea nu este drift de selector

**Owner:** AutoEx pentru corelarea răspunsului observat cu starea UI.
Recunoașterea documentului queue-full, diagnosticul tipizat și recuperarea GET
configurată sunt livrate în
[README-ul modulului](../../atf-test-auto-ex/README.md#limita-disponibilității-serviciului-public).

- [ ] Corelați explicit răspunsul XHR al mutației cu postcondiția UI, inclusiv
      add-to-cart refuzat și modal absent. UI-only păstrează diagnosticul fără
      API evidence; UI+API păstrează exchange-ul sub acțiunea cauzală.
- [ ] Probați răspuns reușit, 5xx observat, request eșuat și lipsă de răspuns,
      cu vecini controlați. Numai timeout-ul nu autorizează inferarea unui 503.
- [ ] Păstrați FAIL și cauza observată, fără retry de click, creare/ștergere cont
      ori plată. O execuție nouă cu identitate separată nu reia mutația incertă.

## CONSUMER-RUNTIME-BOUNDARY-03 — mai puțin suport local, nu un harness ascuns

**Owner:** Shop pentru runtime-ul aplicației; Kit/UI/Reporting pentru
capabilitățile neutre. Boundary-ul AutoEx este normat în
[README-ul său](../../atf-test-auto-ex/README.md#coeziunea-suportului-autoex);
mutarea acestuia în main nu mai este un ToDo.

Acest gap privește resursele și compoziția harness-ului;
`CONSUMER-FLOW-LAYERS-09` deține separarea semantică business/UI și validarea.

| Candidat rămas | Separare cerută | Limită |
|---|---|---|
| `ShopReportSystemMetadata` | inputuri rezolvate explicit, fără citire implicită a stării JVM; snapshot de sesiune distinct de profilul scenariului | fără JUnit/Kit în runtime; fidelitatea Overview este în `CONSUMER-EVIDENCE-CONFIG-04` |
| `ShopE2eContext` | achiziție, publicare și lifecycle cu contracte mici, păstrând fațada business | nu mutați mecanic întregul context și nu dublați primitivele Kit |

- [ ] Extrageți o responsabilitate odată; declarațiile, Store-ul și
      ParameterResolver-ul rămân la granița JUnit.
- [ ] Reutilizați primitivele din Kit existent, cu callbacks tipizate și fără
      dependențe Kit către browser, Reporting sau aplicații.
- [ ] Verificați achiziție parțială și eșec la captură, summary, publish și close;
      cauza inițială și erorile secundare rămân disponibile.
- [ ] Păstrați probele adaptorului și boundary-ul întărit de
      [ConsumerModuleArchitectureGovernanceTest](../../atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerModuleArchitectureGovernanceTest.java),
      fără allowlist, prag mărit sau relaxarea scope-ului Maven.

## CONSUMER-TEST-INTENT-05 — compoziție declarativă fără efecte ascunse

**Owner:** Kit pentru vocabular/resolver neutru; consumerul pentru wiring.
Regulile testului scurt sunt în [bunele practici](../README_Atf_CodeBestPractices.md);
nu sunt un al doilea checklist de implementat.

- [ ] Rezolvați `@AtfUiEvidence` și prin adnotări compuse, cu diagnostic de
      conflict; adaptorii curenți citesc direct `getAnnotation` pe clasă.
- [ ] Probați declarație absentă/directă/compusă, axă configurată și conflict
      înainte de resurse; păstrați resolverul canalului separat.
- [ ] Abia apoi compuneți declarațiile repetitive ale consumerilor. Adnotarea
      clasifică ori configurează; efectele externe și activarea rămân explicite.

## REPORTING-LAUNCHER-06 — selecție egală, acoperire numărată o singură dată

**Owner:** Reporting Tests. Cele două launchere aleg același pachet `e2e`,
același tag și aceeași excludere de suite; numele lor nu separă probele.

- [AtfReportingUiAuditSuite](../../atf-reporting-tests/src/test/java/atf/reporting/governance/AtfReportingUiAuditSuite.java)
  și [ExtentReportingE2eSuite](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/e2e/ExtentReportingE2eSuite.java)
  nu sunt gate-uri de acoperire disjuncte.
- Al doilea declară și `@AtfTestChannel(UI)`; această diferență de metadata
  trebuie păstrată/verificată, nu confundată cu alt inventar de teste.

- [ ] Alegeți un launcher canonic; celălalt devine alias documentat de
      compatibilitate ori primește o selecție realmente distinctă, justificată.
- [ ] Nu lansați ambele în același gate; comparați identitățile JUnit descoperite,
      inclusiv invocările parametrizate, nu numai totalurile.
- [ ] Probați selecția și metadata canalului fără browser; păstrați separat
      proba browser care atestă randarea. Nu eliminați teste pentru a elimina un alias.

## CONSUMER-E2E-PROFILES-01 — variantele de evidence și acoperirea business

**Stare: parțial implementat.** Există trei gate-uri principale per consumer,
iar `AutoExE2eSuite` păstrează toate poveștile și variantele actuale.
Unificarea journey-ului nu a redus încă numărul de purchase-uri din gate.

- [ ] Inventariați separat poveștile, canalele și prezentările; UI+API poate
      avea precondiții/corelări absente în UI-only.
- [ ] Înainte de reducerea selecției, demonstrați smoke-ul adaptorului pe succes
      și failure, plus contractele generice
      `ReportEvidenceProfileIntegrationTest` și `UiScreenshotPolicyIntegrationTest`.
- [ ] Păstrați lansarea manuală a exemplelor ori înlocuiți-o cu un profil
      echivalent; clasele folosite de probele de mapping nu dispar incidental.
- [ ] Păstrați inițial suitele complete și opt-in-urile separate pentru API
      read-only/mutații; nu adăugați o suită pentru fiecare politică de captură.
- [ ] Actualizați inventarul protejat numai după dovada echivalenței; testele
      `ConsumerSuiteCoverageGovernanceTest` și `ConsumerModuleArchitectureGovernanceTest`
      nu se relaxează pentru a ascunde clase eliminate.

## Criteriul comun de închidere

- [ ] Testele business și componentele de adaptor păstrează inputurile,
      assertions și cazurile negative; mutările au mapare vechi → nou.
- [ ] Runtime-ul și helper-ele au owner și dependențe corecte; `src/test` nu
      devine depozit de infrastructură și `src/main` nu ascunde un runner.
- [ ] Fiecare test executabil aparține unui singur gate principal per consumer;
      limita de opt clase se păstrează și în main, și în test.
- [ ] Compilarea/reactorul, probele ownerilor și guvernanța sunt verzi;
      randarea este verificată pentru arbore, capturi, cauzalitate și failure.
- [ ] Contractul livrat se mută în README-ul ownerului; aici rămâne numai
      diferența încă neimplementată, fără istoric de rulări.

Regulile stabile sunt în [ghidul de testare](../ATF_Testing_Guide.md),
[bunele practici](../README_Atf_CodeBestPractices.md) și README-urile ownerilor.
