# ATF — verdicturi și configurare ale consumerilor

## Pe scurt

Restanța activă este configurarea Shop independentă de locale. Acceptanța Account/Cart/Purchase și ancorele ei rămân contracte
separate; probele noi nu redeschid generic cleanup-ul ori raportul comun.

## SHOP-LOCALE-70 — aceeași configurație în orice locale JVM

**Prioritate: 59/100** = impact 22 + lacună 15 + lucrabil 17 + documentare 5.
**Owner:** Shop configuration. **Nivel:** componentă prin `load(Path)` și parsere publice.

Cu locale JVM `tr-TR`, `ShopValidationProfile.from("UI_API")` eșuează.
Și YAML-ul livrat eșuează la încărcare: fallback-ul override-ului folosește
numele enumului, iar normalizarea implicită produce litere dotless.

- [ ] Folosiți normalizare independentă de locale la tokenuri de protocol/configurație; auditați și proiecțiile și checkpoint-urile vecine, fără a modifica textul business.
- [ ] Probați `tr-TR`, locale nominal și toate aliasurile deja acceptate, inclusiv fără override de sistem.
- [ ] Restaurați locale-ul și proprietățile în `finally`, cu lock pentru starea JVM; păstrați diagnosticul pentru un profil invalid real.

**Închidere:** [ShopValidationProfile](../../atf-test-practice-shop/src/main/atf/shop/config/ShopValidationProfile.java)
și [ShopConfigurationLoader](../../atf-test-practice-shop/src/main/atf/shop/config/ShopConfigurationLoader.java)
produc același snapshot valid în locale-urile verificate, fără fallback silențios la alt profil.

## CONSUMER-ACCEPTANCE-ORACLES-58 — dovadă pe fiecare invocare

Verdictul, cauza, checkpoint-ul și captura se verifică per invocare, inclusiv
în raportul comun. [Ghidul acceptanței](../ATF_Testing_Guide.md#acceptanța-business-individual-și-prin-suită)
definește proba comună; [Shop](../../atf-test-practice-shop/README.md#acceptanța-failure-urilor-business)
și [AutoEx](../../atf-test-auto-ex/README.md#acceptanța-failure-urilor-business)
dețin journey-urile, fixture-urile și cleanup-ul aplicației.

## CONSUMER-E2E-PROFILES-01 — variantele de evidence și acoperirea business

Canalul UI/UI_API nu schimbă intenția business. Ramurile de recalculare,
removal și eroarea după un checkpoint PASS au probe distincte. Orice reducere
a selecției cere [review separat](../ATF_Testing_Guide.md#acceptanța-business-individual-și-prin-suită);
o matrice acceptată nu certifică toate ramurile sau stabilitatea statistică.

## Contract și proces

[Procesul izolat](../ATF_Build_And_Execution_Safety.md) păstrează artifactele și
sync-ul selectiv; [baseline-ul](../ATF_Test_Changes_Baseline.md) deține controalele Git.
Oracolele neutre ale rendererului rămân la
[Reporting Tests](ATF_Reporting_Tests_Feedback.md), separat de acceptanța aplicației.
