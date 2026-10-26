# ATF UI — geometria browserului și iframe-uri

## Pe scurt

Ownerul este `atf-ui`: coordonatele pointerului în iframe la zoom CSS non-nativ.
Recuperarea AutoEx prin activare DOM și publicarea configurației sunt contracte
distincte; succesul unui journey nu certifică geometria click-ului.

## UI-FRAME-POINTER-01 — coordonate corecte la zoom CSS non-nativ

**Prioritate:** 72/100 = impact 31 + lacună 25 + lucrabil 10 + documentare 6,
conform [registrului comun](README.md#top-feedback-uri-active).

**Stare: deschis.** Un click normal pe un control din iframe la zoom CSS 70%
poate fi respins cu `body/html intercepts pointer events`. Efectul a apărut
pe vignette-ul real AutoEx și se reproduce independent cu un control HTML
neutru în browserul de componentă `atf-ui`; nu este un mock al reclamei.

- Inițializarea nu trebuie să aplice încă o dată zoom-ul în documentele copil.
  Această separare previne compunerea scalei, dar nu rezolvă singură coordonatele
  pointerului când documentul părinte rămâne la zoom CSS non-nativ.
- AutoEx selectează 70% și folosește activare JavaScript explicită numai pentru
  Close; click-ul standard în iframe la scara nativă rămâne proba distinctă
  a pointerului, nu rezolvarea cerinței la 70%.

Proba neutră la owner distinge documentul principal de iframe: click-ul normal
în pagina principală trece la 70% și 100%, iar cel din iframe eșuează la 70%
și trece la 100%. Observația privește configurația locală Playwright 1.49.0,
nu toate versiunile sau toate browserele.

- Alternativă opt-in: `clickWithJavaScript(..., postcondition)` activează un
  control DOM și verifică efectul înainte de PASS; nu este fallback automat.
- Limită: activarea programatică nu repară coordonatele pointerului;
  [contractul consumerului](../../atf-test-auto-ex/README.md#vignette-și-activare-javascript)
  rămâne separat de acest contract generic.

Context upstream: [solicitarea de zoom nativ Playwright](https://github.com/microsoft/playwright/issues/31836)
distinge zoom-ul browserului de simularea CSS. Nu presupunem că un upgrade
rezolvă această regresie fără proba locală echivalentă.

### Criterii rămase

- [ ] Întăriți proba harness-ului Reporting pentru viewport și scala efectiv
      rezolvate, inclusiv override peste default. `UiReportEnvironmentSnapshotFactory`
      le publică deja; testul său nu afirmă încă aceste valori. Shop are proba
      `ShopReportSystemMetadataTest`, iar AutoEx publică și el configurația rezolvată;
      nu este necesară o nouă implementare de renderer în consumer.

- [ ] Alegeți și probați la owner o soluție pentru pointer și iframe la zoom
      non-nativ: compatibilitate driver/browser sau mecanism de scalare cu
      geometrie corectă; nu corecții de coordonate în page objects de produs.
- [ ] Verificați click real, focus, tastatură și captură pe documentul părinte
      și iframe-uri same-origin/cross-origin la limitele relevante ale scalei.
- [ ] Nu folosiți `force`, dispatch de evenimente sau modificarea DOM-ului
      pentru a declara pointerul reparat. Activarea DOM explicită are evidence
      distinct; 100% rămâne scara nativă pentru proba de pointer embedded.

Regulile stabile sunt în [README ATF UI](../../atf-ui/README.md#configurație-generică-fără-flag-redundant).
Recuperarea contului este normată în
[contractul AutoEx](../../atf-test-auto-ex/README.md#contul-creat-și-cleanup-ul-sub-vignette).
