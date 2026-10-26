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

## CONSUMER-EVIDENCE-CONFIG-04 — același business, configurare completă

**Owner:** consumerii rezolvă configurația și declarațiile; Reporting deține
modelul și publicarea. Canalul, gruparea, capturile și acțiunile sunt axe distincte.

| Lacună | Sursa curentă | Criteriu de închidere |
|---|---|---|
| Grupare configurată | [AutoExEvidenceProfileAdapter](../../atf-test-auto-ex/src/test/java/atf/ui/tests/autoex/extension/AutoExEvidenceProfileAdapter.java) mapează `CONFIGURED` la `SCREEN_CHECKPOINTS`; [ShopScreenshotPolicy](../../atf-test-practice-shop/src/main/java/atf/ui/practiceshop/reporting/ShopScreenshotPolicy.java) construiește aceeași bază | flow-ul implicit vine din configurația rezolvată, nu dintr-un default ascuns |
| Detaliile acțiunilor | Shop are override prin `@WithE2eHarness`; nu există un plan YAML complet pentru ambii consumeri | configurare per axă, cu precedență și conflict explicit înaintea resurselor |
| `CONSUMER-OVERVIEW-PROFILE-08` | System-ul raportului Shop partajat păstrează profilul primului scenariu, inclusiv API/capturi, deși frații pot fi UI-only sau unified | valorile comune sunt corecte pentru întregul artifact; diferențele sunt declarate ca mixte sau mutate la scenariul proprietar |

- [ ] Rezolvați un plan imutabil per scenariu înainte de browser/report/mutații:
      bază configurată, override explicit, diagnostic și canale permise.
- [ ] Extindeți YAML-ul numai împreună cu loaderul și mapping-ul verificat.
      `CONFIGURED` moștenește baza reală; canalul declarat nu este suprascris tacit.
- [ ] Probați combinațiile și izolarea fraților în același artifact pe succes și
      failure, inclusiv Overview/System. Precondiția API standalone nu dovedește
      autentificarea sesiunii browser.
- [ ] Pentru compactarea UI-only într-un singur set de tabele cross-phase,
      păstrați testul existent și urmați [cerința Reporting](ATF_Reporting_Feedback.md#report-unified-tables-01--un-singur-set-de-tabele-pentru-flow-ul-ui-only).
      Titlul unic și captura finală sunt necesare, dar nu închid agregarea.
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

- [AtfReportingUiAuditSuite](../../atf-reporting-tests/src/test/java/atf/api/reporting/governance/AtfReportingUiAuditSuite.java)
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
