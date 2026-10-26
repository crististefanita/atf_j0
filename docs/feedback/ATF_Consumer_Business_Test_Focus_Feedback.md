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
| `CONSUMER-RUNTIME-BOUNDARY-03` | 82 = 31 + 23 + 20 + 8 | runtime în main la owner, adaptor JUnit minim |
| `CONSUMER-EVIDENCE-CONFIG-04` | 80 = 30 + 22 + 20 + 8 | un journey, axe de evidence independente și configurabile |
| `CONSUMER-TEST-INTENT-05` | 67 = 27 + 18 + 16 + 6 | teste declarative și contracte mici, fără mascarea aserțiunilor |
| `REPORTING-LAUNCHER-06` | 58 = 23 + 14 + 17 + 4 | un launcher canonic pentru aceeași selecție browser |
| `CONSUMER-E2E-PROFILES-01` | 57 = 24 + 10 + 17 + 6 | reducerea repetării live numai după echivalență demonstrată |

Ordinea urmărește ownership → runtime → configurare → intenții → selecții.
Nu optimizăm numărul de clase înainte de a proteja scope-ul și verdictul.

## CONSUMER-RUNTIME-BOUNDARY-03 — mai puțin suport local, nu un harness ascuns

**Owner:** consumerul pentru runtime specific, Kit/UI/Reporting pentru
capabilitățile neutre. Mutarea de fișiere fără separarea responsabilităților
doar ascunde complexitatea.

| Candidat live | Destinație de evaluat | Condiție înainte de mutare |
|---|---|---|
| `ShopReportSystemMetadata` | runtime/config Shop în main numai după separarea inputurilor | fără citire implicită a stării JVM, JUnit sau Kit |
| `AutoExEvidenceProfile` | factory cu input Reporting la owner; bridge de adnotări minim | importă enum-uri Kit: nu poate fi mutat neschimbat în main AutoEx |
| `ShopE2eContext` | runtime mic plus adaptor JUnit subțire | separați achiziția, observarea, publicarea și cleanup-ul, păstrând fațada |
| scope-uri, inițializare, cleanup neutru | `atf-test-kit/src/main` existent | callbacks tipizate; fără Playwright, Reporting sau clase ale aplicației |

Separarea curentă este protejată de
[ConsumerModuleArchitectureGovernanceTest](../../atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerModuleArchitectureGovernanceTest.java).
Acesta păstrează harness-ul și metadata Shop în test, cere cele două factory-uri
pure în main și interzice JUnit/Kit în runtime-ul consumerilor. Ownership-ul
livrat este în [README-ul Shop](../../atf-test-practice-shop/README.md#ce-deține-modulul).

Runtime-ul AutoEx și profilul rezolvat sunt descrise în
[README-ul AutoEx](../../atf-test-auto-ex/README.md#runtime-în-main-și-granița-junit).
Guard-ul cere runtime-ul AutoEx în main și adaptoarele JUnit în `extension`;
aici rămân numai responsabilitățile de separat.

- [ ] Extrageți câte o responsabilitate, păstrând testele business și API-ul
      fațadei până când ambele adaptoare dovedesc noul contract.
- [ ] Reutilizați `TestRunInitialization`, `CloseOnceResource`,
      `FailureAwareCleanup`, `FailureEvidenceCleanup` și `ArmedCleanup`;
      nu creați al doilea lifecycle sau un `BaseE2eTest` universal.
- [ ] Păstrați condițiile JUnit, Store-ul și ParameterResolver-ul la granița de
      execuție; promovați în Kit numai o extensie neutră cu contract comun demonstrat.
- [ ] Nu lărgiți scope-ul Maven al Kit doar ca să compileze un helper mutat.
      Reporting rămâne standalone; Kit și UI nu capătă dependențe circulare.
- [ ] Pentru mapping-ul network → Reporting, păstrați un bridge explicit:
      UI nu importă Reporting, iar biblioteca Reporting nu importă UI/Core.
      Nu copiați mapperul între journey-uri și nu mutați bridge-ul în Kit.
- [ ] Probați achiziție parțială și eșecuri la captură, publish, summary și close;
      browserul se închide, resursa împrumutată nu se închide prematur.
- [ ] Modificați guard-ul de source-set numai într-un increment explicit,
      cu contractul echivalent și probele negative; fără allowlist sau prag mărit.

## CONSUMER-EVIDENCE-CONFIG-04 — același business, altă prezentare

**Owner:** consumerii pentru rezolvare și wiring; Reporting pentru model și
publicare. Canalul, gruparea, capturile și detaliul acțiunilor sunt axe diferite.

| Axă | Opțiuni urmărite | Ce nu trebuie să schimbe |
|---|---|---|
| Canal | `UI` / `UI_API` | pașii și rezultatul business |
| Grupare | `SCREEN_CHECKPOINTS` / `UNIFIED_FLOW` | executarea validărilor intermediare |
| Capturi | `EACH_CHECKPOINT` / `FINAL_ONLY` / `FINAL_AND_FAILURE` | proveniența imaginii și verdictul |
| Acțiuni | incluse colapsat / sumar | înregistrarea acțiunilor și cauza eșecului |

<details>
<summary><strong>Ce există și unde configurarea este incompletă</strong></summary>

- [AtfUiEvidence](../../atf-test-kit/src/main/java/atf/api/testkit/evidence/AtfUiEvidence.java)
  declară flow și capturi; `AtfTestChannel` declară separat canalul.
- Shop și AutoEx mapează capturile `CONFIGURED` din politica Reporting;
  gruparea implicită este construită ca `SCREEN_CHECKPOINTS`.
- [AutoExEvidenceProfileAdapter.forRun](../../atf-test-auto-ex/src/test/java/atf/ui/tests/autoex/extension/AutoExEvidenceProfileAdapter.java)
  tratează `CONFIGURED` și `SCREEN_CHECKPOINTS` identic: nu primește un flow YAML.
- Shop construiește baza prin
  [ShopScreenshotPolicy](../../atf-test-practice-shop/src/main/java/atf/ui/practiceshop/reporting/ShopScreenshotPolicy.java),
  apoi aplică adnotarea în `ShopE2eTestSupport.reportProfile`.
- Clasele exemplu schimbă aceste axe, dar nu există încă un plan rezolvat comun
  care să garanteze toate combinațiile din YAML în ambii consumatori.

</details>

- [ ] Definiți la owner un plan imutabil, rezolvat o singură dată înainte de
      resurse: configurație de bază, override explicit per axă și diagnostic.
      Nu documentați chei YAML noi ca și cum ar fi deja implementate.
- [ ] Expuneți gruparea și detaliile acțiunilor prin configurație, nu prin
      ramuri în fiecare test. `CONFIGURED` citește baza reală; conflictele
      explicite se resping înainte de browser, report sau mutații.
- [ ] Canalul declarat nu este suprascris în tăcere de YAML. Orice profil de
      lansare configurabil trebuie validat față de canalele permise ale testului.
- [ ] UI per pas atașează starea capturată la checkpoint-ul său; nu copiază
      ultima poză la toate nodurile. Stări identice pot avea imagini identice.
- [ ] UI cu `FINAL_ONLY` produce o captură la succes, după confirmarea comenzii;
      `FINAL_AND_FAILURE` păstrează separat și captura unui eșec.
- [ ] `UNIFIED_FLOW` păstrează toate verificările și acțiunile colapsate, fără
      subpropoziții Gherkin publicate; forma compactă nu scurtează purchase-ul.
- [ ] UI-only nu publică API validation, exchange-uri sau journey inspector și
      nu execută precondiții API ascunse. Nu înseamnă blocarea rețelei aplicației.
- [ ] UI+API păstrează exchange-ul la checkpoint-ul cauzal, validări UI/API
      distincte și sumarul terminal. Retry-ul aceluiași exchange rămâne în Execution;
      request-uri legitime diferite nu se comasează numai după metodă și rută.
- [ ] Probați configurație/adnotare → plan → captură/publicare pe succes și
      failure, inclusiv profile diferite în același raport partajat.

## CONSUMER-TEST-INTENT-05 — teste scurte, intenție completă

**Owner:** Shop/AutoEx pentru business, ownerul capabilității pentru suport.
Mai puține linii nu înseamnă mai puține aserțiuni sau un `execute()` opac.

- [ ] Testul declară scenariul, precondiția, canalul și rezultatul urmărit;
      runtime-ul pregătește resursele, journey-ul de business execută traseul.
- [ ] Compoziția adnotărilor reduce repetiția fără să ascundă efectele externe.
      Resolverul de evidence trebuie probat înainte de a muta declarații directe
      în meta-adnotări; simpla citire prin `getAnnotation` nu le traversează.
- [ ] Reutilizați constructori/factory-uri și assertion-uri cu input observat;
      static imports îmbunătățesc lectura, nu ascund starea ori accesul la rețea.
- [ ] Nu introduceți JSON parsing, selectori, format HTML, retry sau close în
      metodele de test; păstrați-le la capabilitatea care deține contractul.
- [ ] Păstrați componentele business chiar cu transport mock. Rendererul generic,
      disclosure-urile și capturile neutre se probează în Reporting Tests, nu în Shop.
- [ ] Probați același journey până la confirmarea comenzii/facturii pentru
      profilurile compatibile; `payment check` singur nu este purchase complet.

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
