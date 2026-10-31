# Playwright — operații, deadline și sesiune de browser

## Pe scurt

Pachetul este adaptorul browser generic. Primește configurație și sink explicit,
înregistrează operații și expune observații; nu decide rezultatul unui checkout
și nu deține rendererul sau runnerul JUnit.

## Intrări și responsabilități

| Când investighezi | Citește întâi | Limita |
|---|---|---|
| achiziție și release | [PlaywrightUiSession](PlaywrightUiSession.java) | deține Playwright/browser/context/page și inspectorul |
| scală și coordonate iframe | [UiViewportScale](../config/geometry/UiViewportScale.java) → [PlaywrightUiSession](PlaywrightUiSession.java) | ieșire cerută, viewport CSS și DPR native; fără CSS injectat |
| vocabularul acțiunii | [PlaywrightActionSequence](PlaywrightActionSequence.java) | fațadă publică, nu implementarea tuturor operațiilor |
| download și cleanup-ul observației | [PlaywrightBrowserActions](PlaywrightBrowserActions.java) | listener armat înainte de click, deadline comun și eveniment real |
| readiness și buget | [PlaywrightBrowserActions](PlaywrightBrowserActions.java) | așteptări browser și observație în buget |
| locator și interacțiune | [PlaywrightLocatorActions](PlaywrightLocatorActions.java) | operații asupra locatorului |
| select înlocuit în timpul așteptării | [PlaywrightSelectAction](selection/PlaywrightSelectAction.java) | observație read-only înaintea unicului apel nativ, fără replay |
| observația BE pasivă și dezabonarea | [UiBackendRecovery](recovery/UiBackendRecovery.java) și [UiRecoveryObserver](recovery/UiRecoveryObserver.java) | metadata scoped; fără dispatch, replay sau body capturat |
| atașare parțială și erori la retragere | [UiListenerScope](lifecycle/UiListenerScope.java) | activ numai după atașarea completă; toate retragerile încercate și cauzele distincte păstrate |
| cauză, timp și diagnostic | [UiActionRecorder](UiActionRecorder.java) | înregistrare și politica de retry a operației |

## Invariante de păstrat

- Readiness-ul primește bugetul rămas. O nouă citire nu primește automat încă
  un timeout complet; expected-ul business nu este predicat de sincronizare.
- Nu echivala clickul reușit cu finalizarea efectului. Download-ul, navigarea
  și răspunsul HTTP sunt observații distincte care cer context și timp propriu.
- Consultă politica de retry înainte de a modifica o operație; nu adăuga retry
  local la o mutație pentru a ascunde o postcondiție lipsă.
- Selectul reobservă locatorul strict și opțiunea fără a activa controlul vechi.
  Evenimentul nativ este permis o singură dată, în restul deadline-ului; un rezultat
  gol sau o excepție după dispatch rămân FAIL și nu autorizează încă o selecție.
- Diagnosticul unei acțiuni sensibile rămâne sigur. Proiecția HTML și preview-ul
  aparțin bibliotecii Reporting, nu acestui adaptor.

Pregătirea [gate-ului de geometrie](../../../../../../README.md#gate-ul-fixture-ului-de-geometrie)
aparține fixture-ului din test: documente inițializate, layout stabil și captură în bugetul
navigării, apoi pointer la 850 ms. Runtime-ul nu primește un warm-up implicit.

## Probe și vecini

| Contract | Probă de pornire |
|---|---|
| vocabular și înregistrarea acțiunilor | [PlaywrightActionSequenceTest](../../../../../test/java/atf/ui/playwright/PlaywrightActionSequenceTest.java) |
| pointer trusted, focus, captură și refuz la 70% | [PlaywrightPointerGeometryComponentTest](../../../../../test/java/atf/ui/playwright/geometry/PlaywrightPointerGeometryComponentTest.java) |
| download, eveniment și cleanup | [PlaywrightDownloadComponentTest](../../../../../test/java/atf/ui/playwright/download/PlaywrightDownloadComponentTest.java) |
| continuitatea selectului și refuzul replay-ului | [PlaywrightSelectContinuityComponentTest](../../../../../test/java/atf/ui/playwright/selection/PlaywrightSelectContinuityComponentTest.java) |
| achiziția sesiunii, cauze simultane și close-once | [PlaywrightUiSessionLifecycleComponentTest](../../../../../test/java/atf/ui/playwright/PlaywrightUiSessionTest.java); resurse returnate cu succes, release complet și aceeași cauză |
| atașarea și retragerea listener-elor | [UiListenerScopeComponentTest](../../../../../test/java/atf/ui/playwright/lifecycle/UiListenerScopeComponentTest.java) și [UiRecoveryObserverLifecycleComponentTest](../../../../../test/java/atf/ui/playwright/recovery/UiRecoveryObserverLifecycleComponentTest.java) |

Pentru ownership și capturi continuă în [README-ul UI](../../../../../../README.md);
pentru operația business continuă la pagina/flow-ul consumerului. Un fixture
local nu certifică readiness-ul întregii aplicații externe.

Callback-ul verifică `active()` înaintea oricărui acces la request/response/frame.
Contractul scope-ului este la [UI](../../../../../../README.md#scope-ul-listener-elor-de-observație);
inspectorul de rețea își păstrează separat ownership-ul și ordinea de release.

Continuă orientarea în [atlasul de context ATF](../../../../../../../docs/ATF_Context_Atlas.md).

