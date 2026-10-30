# Shop support — contracte de ecran și dovezi cauzale

## Pe scurt

Pachetul adaptează observațiile Shop la contracte de ecran, validări și dovezi
API. Rămâne în main; harness-ul JUnit și selecția suitei rămân în test.
Modelul Shop este reperul consumer pentru această separare, nu o regulă de
copiere a endpoint-urilor sau a politicilor sale în AutoEx.

## Intrări și responsabilități

Conversia din [ShopApiEvidenceMapper](api/ShopApiEvidenceMapper.java) păstrează
[query-ul typed](../../../../../../atf-reporting/README.md#fidelitatea-contractului-query)
pentru identitatea agregatului; view-ul legacy rămâne separat. Perechile disponibile
se păstrează integral. Pentru captura browser redactată, jurnalul și `withTrace`
conservă numai amprenta opacă; nu reconstruiesc valori sau parametri din placeholder.

Rutele, politica personal-field și alegerea comenzii aparțin Shop.

Factory-ul runtime transmite originea API configurată către captura de răspuns.
Aceasta păstrează livrarea la `response` folosită de gate-urile Shop; abonarea
la inspectorul UI `request-finished` cere reconcilierea explicită a momentului consumului.

| Intrare | Limită |
|---|---|
| `attach(Page, URI)` | originea API configurată, cu schemă/host/port normalizate; folosită de runtime |
| `attach(Page)` | compatibilitate pentru probele adaptorului existent, cu hostul demo istoric |
| `attach(UiNetworkInspector)` | abonare neutră disponibilă; callerul trebuie să consume observațiile finalizate înaintea gate-ului business |

Publicarea grupului non-HTTP și sumarele sigure sunt delegate la Reporting.
`ShopApiReportPublisher` păstrează titlul, expected-urile și rezultatul business;
un grup de aserțiuni nu mai este reprezentat printr-un request `ASSERT` artificial.

| Când investighezi | Citește întâi | Limita |
|---|---|---|
| ce se citește din ecran | [ShopPageValidator](validation/ShopPageValidator.java) | observă contractul paginii și produce rezultate typed |
| ce trafic aparține checkpoint-ului | [ShopApiNetworkJournal](api/ShopApiNetworkJournal.java) | jurnalul aceleiași sesiuni UI; nu generează alt coș |
| ce exchange se alege | [ShopApiExchangeSelector](api/ShopApiExchangeSelector.java) | selecție ordonată; verifică separat query-ul și payload-ul |
| dacă request-ul comenzii este nou | [ShopOrderRequestWindow](api/correlation/ShopOrderRequestWindow.java) | identitate la pornire, distinctă de ordinea răspunsurilor |
| confirmarea adăugării în coș | [ShopCartAdditionConfirmation](cart/ShopCartAdditionConfirmation.java) | request pornit după armare, payload deținut și răspunsul aceleiași cereri |
| de ce catalogul nu devine ready | [ShopCatalogReadiness](catalog/ShopCatalogReadiness.java) | observă GET/QUERY; permite cel mult un GET home după tranzitoriu confirmat |
| ce fapt a produs catalogul | [ShopCatalogObservation](catalog/ShopCatalogObservation.java) | listener-e, request curent, receipt și document; fără decizie de refresh |
| de unde vine adresa observată | [ShopPostalLookup](billing/ShopPostalLookup.java) și [ShopBillingPage](../pages/ShopBillingPage.java) | cerere nouă, tuple curent, răspuns complet și DOM concordant |

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
- Billing-ul armează observația înaintea input-urilor și consumă debounce-ul
  aplicației în bugetul existent. Configured-fields drenează autofill-ul posibil,
  apoi tastează o dată; postal-lookup cere răspuns curent și DOM concordant.
- Catalogul și lookup-ul compun scope-ul UI: callback-urile sunt inerte la
  atașare parțială și după close; retragerile păstrează cauza primară și erorile distincte.
- Observația catalogului este separată de clasificarea tranzitoriului și de
  unicul refresh home. Readiness-ul consumă același `ObservationBudget`; GET/QUERY
  și ultimul request eligibil rămân politica Shop, distinctă de observerul BE strict.

## Probe și vecini

| Contract | Probă de pornire |
|---|---|
| acceptarea coșului fără toast | [ShopCartAcknowledgementComponentTest](../../../../test/atf/shop/tests/component/cart/ShopCartAcknowledgementComponentTest.java) |
| ownership la pornirea request-ului | [ShopOrderRequestStartComponentTest](../../../../test/atf/shop/flow/ShopOrderRequestStartComponentTest.java) |
| snapshot numeric și coliziuni | [ShopLegacyMarkerComponentTest](../../../../test/atf/shop/tests/api/network/causality/ShopLegacyMarkerComponentTest.java) |
| catalog nominal și refuz | [ShopCatalogReadinessComponentTest](../../../../test/atf/shop/tests/component/catalog/ShopCatalogReadinessComponentTest.java) |
| erori de lifecycle catalog | [ShopCatalogLifecycleComponentTest](../../../../test/atf/shop/tests/component/catalog/lifecycle/ShopCatalogLifecycleComponentTest.java) |
| cursa autofill/input și refuzuri | [ShopBillingSynchronizationComponentTest](../../../../test/atf/shop/tests/component/billing/ShopBillingSynchronizationComponentTest.java) |
| adresa lookup-ului curent | [ShopPostalLookupComponentTest](../../../../test/atf/shop/tests/component/billing/ShopPostalLookupComponentTest.java) |

Achiziția concretă începe în [ShopBrowserRuntime](../runtime/ShopBrowserRuntime.java).
Contractele și probele celorlalte checkpoint-uri rămân în [contractul modulului](../../../../../README.md).

Scope-ul reutilizabil are [contract la UI](../../../../../../atf-ui/README.md#scope-ul-listener-elor-de-observație).
Lookup-ul observă traficul aplicației; nu execută un al doilea GET și nu suprascrie câmpurile generate.

Continuă orientarea în [atlasul de context ATF](../../../../../../docs/ATF_Context_Atlas.md).
