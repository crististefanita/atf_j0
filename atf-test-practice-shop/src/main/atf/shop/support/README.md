# Shop support — contracte de ecran și dovezi cauzale

## Pe scurt

Pachetul adaptează observațiile Shop la contracte de ecran, validări și dovezi
API. Rămâne în main; harness-ul JUnit și selecția suitei rămân în test.
Modelul Shop este reperul consumer pentru această separare, nu o regulă de
copiere a endpoint-urilor sau a politicilor sale în AutoEx.

## Intrări și responsabilități

| Când investighezi | Citește întâi | Limita |
|---|---|---|
| ce se citește din ecran | [ShopPageValidator](validation/ShopPageValidator.java) | observă contractul paginii și produce rezultate typed |
| ce trafic aparține checkpoint-ului | [ShopApiNetworkJournal](api/ShopApiNetworkJournal.java) | jurnalul aceleiași sesiuni UI; nu generează alt coș |
| ce exchange se alege | [ShopApiExchangeSelector](api/ShopApiExchangeSelector.java) | selecție ordonată; verifică separat query-ul și payload-ul |
| dacă request-ul comenzii este nou | [ShopOrderRequestWindow](api/correlation/ShopOrderRequestWindow.java) | identitate la pornire, distinctă de ordinea răspunsurilor |
| confirmarea adăugării în coș | [ShopCartAdditionConfirmation](cart/ShopCartAdditionConfirmation.java) | request pornit după armare, payload deținut și răspunsul aceleiași cereri |
| de ce catalogul nu devine ready | [ShopCatalogReadiness](catalog/ShopCatalogReadiness.java) | readiness inițial GET, separat de confirmarea adăugării în coș |

## Invariante de păstrat

- Expected-ul vine din configurație/fixture; actual-ul se citește independent.
  Nu înlocui valorile lipsă și nu deriva expected-ul din preview-ul raportului.
- Cea mai recentă comandă eligibilă poate fi FAIL. Un `201` vechi nu acoperă
  un `500` ulterior; o captură începută înaintea markerului nu devine nouă la sosire.
- Marcajul numeric citește recepția după bariera browserului și salvează fence-ul
  la cheia returnată. O coliziune între ferestre diferite este ambiguă și refuzată;
  marcajul typed rămâne independent și este folosit de Confirm.
- Toast-ul nu confirmă mutația. Răspunsul propriului POST este așteptat în bugetul
  unicului click; coșul se validează apoi independent, fără replay.
- Achiziția browserului este în runtime, iar capturile sunt în evidence.
  `support` nu devine un al doilea composition root sau un registru JUnit.

## Probe și vecini

| Contract | Probă de pornire |
|---|---|
| acceptarea coșului fără toast | [ShopCartAcknowledgementComponentTest](../../../../test/atf/shop/tests/component/cart/ShopCartAcknowledgementComponentTest.java) |
| ownership la pornirea request-ului | [ShopOrderRequestStartComponentTest](../../../../test/atf/shop/flow/ShopOrderRequestStartComponentTest.java) |
| snapshot numeric și coliziuni | [ShopLegacyMarkerComponentTest](../../../../test/atf/shop/tests/api/network/causality/ShopLegacyMarkerComponentTest.java) |
| catalog nominal și refuz | [ShopCatalogReadinessComponentTest](../../../../test/atf/shop/tests/component/catalog/ShopCatalogReadinessComponentTest.java) |

Achiziția concretă începe în [ShopBrowserRuntime](../runtime/ShopBrowserRuntime.java).
Contractele și probele celorlalte checkpoint-uri rămân în [contractul modulului](../../../../../README.md).

Continuă orientarea în [atlasul de context ATF](../../../../../../docs/ATF_Context_Atlas.md).
