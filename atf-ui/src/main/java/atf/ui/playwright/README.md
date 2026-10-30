# Playwright — operații, deadline și sesiune de browser

## Pe scurt

Pachetul este adaptorul browser generic. Primește configurație și sink explicit,
înregistrează operații și expune observații; nu decide rezultatul unui checkout
și nu deține rendererul sau runnerul JUnit.

## Intrări și responsabilități

| Când investighezi | Citește întâi | Limita |
|---|---|---|
| achiziție și release | [PlaywrightUiSession](PlaywrightUiSession.java) | deține Playwright/browser/context/page și inspectorul |
| vocabularul acțiunii | [PlaywrightActionSequence](PlaywrightActionSequence.java) | fațadă publică, nu implementarea tuturor operațiilor |
| download și cleanup-ul observației | [PlaywrightBrowserActions](PlaywrightBrowserActions.java) | listener armat înainte de click, deadline comun și eveniment real |
| readiness și buget | [PlaywrightBrowserActions](PlaywrightBrowserActions.java) | așteptări browser și observație în buget |
| locator și interacțiune | [PlaywrightLocatorActions](PlaywrightLocatorActions.java) | operații asupra locatorului |
| cauză, timp și diagnostic | [UiActionRecorder](UiActionRecorder.java) | înregistrare și politica de retry a operației |

## Invariante de păstrat

- Readiness-ul primește bugetul rămas. O nouă citire nu primește automat încă
  un timeout complet; expected-ul business nu este predicat de sincronizare.
- Nu echivala clickul reușit cu finalizarea efectului. Download-ul, navigarea
  și răspunsul HTTP sunt observații distincte care cer context și timp propriu.
- Consultă politica de retry înainte de a modifica o operație; nu adăuga retry
  local la o mutație pentru a ascunde o postcondiție lipsă.
- Diagnosticul unei acțiuni sensibile rămâne sigur. Proiecția HTML și preview-ul
  aparțin bibliotecii Reporting, nu acestui adaptor.

## Probe și vecini

Pornește de la [PlaywrightActionSequenceTest](../../../../../test/java/atf/ui/playwright/PlaywrightActionSequenceTest.java).
Pentru ownership și capturi continuă în [README-ul UI](../../../../../../README.md);
pentru operația business continuă la pagina/flow-ul consumerului. Un fixture
local nu certifică readiness-ul întregii aplicații externe.

Continuă orientarea în [atlasul de context ATF](../../../../../../../docs/ATF_Context_Atlas.md).

Operația Download este protejată separat de [PlaywrightDownloadComponentTest](../../../../../test/java/atf/ui/playwright/download/PlaywrightDownloadComponentTest.java).
