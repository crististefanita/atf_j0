# ATF — echivalența profilelor consumerilor

## Pe scurt

Achiziția și rollback-ul Shop au [contract și probă negativă la owner](../../atf-test-practice-shop/README.md#limita-suportului-partajat).
Compoziția comună INFO, observația catalogului și cleanup-ul au ownership
explicit în [atlas](../ATF_Context_Atlas.md#reutilizare-fără-mutarea-politicii-aplicației).

- Primitivele neutre rămân în Kit/UI/Reporting, suportul aplicației în main,
  iar harness-ul, Store-ul și fixture-ul controlat rămân în test.
- Probele locale de profile verifică extensiile E2E reale, HTTP 500/200,
  browser/context proaspăt, localStorage izolat și un HTML individual/comun.
  Ele nu înlocuiesc failure-ul dintr-un anumit punct business al journey-ului.

Contractele sunt la [Shop](../../atf-test-practice-shop/README.md#limita-suportului-partajat),
[AutoEx](../../atf-test-auto-ex/README.md#profile-uiapi-și-acoperirea-network-inspector)
și [ghidul auditului componentelor](../ATF_Testing_Guide.md#auditul-componentelor-consumerilor).

## CONSUMER-E2E-PROFILES-01 — variantele de evidence și acoperirea business

**Prioritate: 47/100** = impact 22 + lacună 8 + lucrabil 12 + documentare 5.
**Owner:** consumerii pentru selecția business; Reporting Tests pentru
contractele neutre de evidence și capturi.

| Boundary demonstrat | Ce nu închide singur |
|---|---|
| `ShopEvidenceProfileIsolationComponentTest` și `AutoExEvidenceProfileIsolationComponentTest` | failure individual UI/UI_API și failure → succes în profil opus; nu injectează eroarea în checkout-ul ori cleanup-ul unui cont real |
| deadline-ul UI determinist, tranziția produsului și adaptorul Purchase AutoEx | probele locale separă ceasul de browser și păstrează cauza și oprirea înaintea add-ului; nu înlocuiesc failure-ul business Account/Cart/Purchase pe site-ul real |
| aceeași intrare de journey pentru Account/Cart/Purchase UI și UI_API | reutilizarea codului nu demonstrează fiecare ramură de failure a adaptorului business |
| `AutoExAccountCleanupRecoveryComponentTest`: confirmarea comenzii respinsă în UI/UI_API, validator/checkpoint reale, POST unic, captura păstrată și cleanup owned; invocare ulterioară independentă | contul acestei ramuri este prearmat; înregistrarea prin pagina reală are proba distinctă de mai jos |
| același test: înregistrare nativă prin `AccountPage`, apoi aceeași eroare controlată UI/UI_API; signup/create unice, cleanup numai pentru identitatea proprie, refuz străin secundar, captură originală și vecin sănătos | documentele și răspunsurile sunt locale; nu certifică înregistrarea remote sau alternarea profilelor prin extensia și suita E2E complete |
| același test: gate-ul coșului UI/UI_API prin pagina/adaptorul/flow-ul reale, preț DOM greșit, aceeași cauză și expected, fără checkout/replay; captură originală, cleanup owned și vecin sănătos | adăugarea locală este controlată; nu certifică înregistrarea remote, adăugarea publică din catalog, recalcularea, restaurarea sesiunii sau toate ramurile Cart |
| suitele canonice nemodificate, cu profile și conturi | o execuție integrală verde nu măsoară stabilitatea în multe repetări și nu autorizează reducerea selecției |

- [ ] Înaintea eliminării unui E2E repetitiv, completați probele locale Account,
      Cart și Purchase cu alternarea UI/UI_API prin extensia E2E reală și resursa
      de raport a suitei. Păstrați cauza/faza, cleanup-ul owned, captura inițială
      și vecinul sănătos; matricea locală și smoke-ul extensiei nu se compun
      implicit într-o probă a journey-ului complet.
- [ ] Delimitați ramurile Cart încă neacoperite de această probă: adăugare din
      catalogul public, recalculare și restaurare sesiune. Pentru fiecare caz
      selectat cereți expected independent și zero replay al mutației.
- [ ] Verificați în raportul E2E punctul business efectiv eșuat: checkpoint și
      comparație expandabilă când eșuează validarea; diagnostic și captura
      originală când eroarea este ridicată după un checkpoint reușit.
      Contractele neutre de evidence nu substituie această integrare.
- [ ] Reduceți inventarul protejat numai după aceste dovezi și păstrați lansarea
      manuală ori profilul echivalent. `ConsumerSuiteCoverageGovernanceTest`,
      opt-in-urile și oracolele existente nu se relaxează pentru a ascunde cazuri.

Selecțiile canonice sunt păstrate. Această restanță condiționează o optimizare
ulterioară a repetării live; nu declară un nou incident al site-ului extern.

## Acceptanță pentru reevaluarea Shop și AutoEx

- Expected-ul rămâne independent de actual și de preview-ul redactat; unit
  tests nu primesc artificial contractul Extent al componentelor/E2E.
- Componentele auditate publică pasul standard colapsabil; un HTML și calea
  absolută după flush se păstrează individual/prin suită, cu default preview 240.
- Păstrați aceeași configurație business, deadline-urile și cele 12 module;
  captura ori canalul de evidence nu schimbă politica operației executate.

Reevaluarea urmează [rubrica de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor).
Controlul Git verificat se păstrează în [baseline](../ATF_Test_Changes_Baseline.md),
iar build-ul și promovarea urmează [procesul izolat](../ATF_Build_And_Execution_Safety.md).
