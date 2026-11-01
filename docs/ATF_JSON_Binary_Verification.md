# JSON și biblioteci Core C2 / Reporting J9 — acceptanță

## Pe scurt

J0 păstrează sursele și nativele; J1/J2/J3 consumă aceiași bytes C2/J9.
Perechile JAR/POM sunt direct în lib, câte o versiune per bibliotecă; J3 are
șapte module și nu mai păstrează Core sursă.

| Bibliotecă | SHA-256 JAR |
|---|---|
| atf.api:atf-core:1.0.0-c2 | 91080FE996CCB358B7E42B8F147322B3D4468D84C364DFD259371EEEF9AFD5DB |
| atf.api:atf-reporting:1.0.0-j9 | 2B58E5B871CCB78AF53C5BF77F1A21B6F6EAE92D18EF5F66CAC6FF9CCA154322 |

## Contract și dovadă

| Suprafață | Rezultat și limită |
|---|---|
| Core sursă și JAR C2 | 276 native PASS pe fiecare; document complet, duplicate, null/write, redacție exactă și factory înainte de submission |
| control Core C1 | 37 cazuri noi, 20 FAIL așteptate; aceeași regresie pe sursa veche și JAR fizic |
| Mock și Validation | suite complete pe sursă și separat în J1/J2/J3; route fallback independent, scalar YAML exact, tipuri și raw body păstrate |
| Reporting nativ | 64 PASS per build; redactor și formatter probate separat, cu numere și duplicate |
| control Reporting J8 fizic | 16 cazuri, 15 FAIL așteptate; eroarea numerică și pierderea multiplicității sunt reproduse |
| Reporting J9 fizic | 39 PASS: native JSON, browser JSON, disclosure API și CodeSource; fără implementare Reporting în harness |
| unit-uri Reporting pe sursă | 486 PASS; distinct de cele opt componente JSON și cele 22 API disclosure |
| Kit | 133 PASS separat în fiecare workspace; cele două probe requirement sunt separate semantic, fără prag relaxat |
| J1 | guvernanță completă și câte un component Shop/AutoEx; nu declară o nouă execuție de journey extern |
| J2 | MOCK individual/suită, Extent/fallback, plus noua suită SQL embedded; fără certificare Kafka remote |
| J3 | guvernanță și Speech component suite fără model; nu reprezintă reexecuție ASR ori microfon |
| publicare | două build-uri curate identice pentru fiecare JAR; comparația API nu elimină simboluri publice |

## Randare și limite rămase

- Browserul automat verifică Extent/fallback, individual/agregat, ownerul activ, tokenurile numerice și toate aparițiile duplicate.
- Mouse, Enter și Space păstrează disclosure-ul auditat; ultimul rând și verdicturile FAIL/FAIL/PASS sunt accesibile.
- Assertions păstrează default 240; fixture-ul numeric declară body 240, fără schimbarea default-ului standalone 100.
- Oracolul refuză rotunjirea ori eliminarea unui duplicat chiar când statusurile rămân identice.
- TAB MCP confirmă valorile și fallback keyboard; închiderea unui vecin după Expand all expune separat REPORT-SIBLING-DISCLOSURE-114.

Cele patru criterii Reporting preexistente de coeziune/importuri rămân la J0.
Timing-ul capturat/publicare, profilul inspectorului, contrastul și lățimea
fallback păstrează criteriile lor; fidelitatea JSON nu le închide implicit.

## Reproducere și sincronizare

[Core Consumer](ATF_Core_Consumer.md) descrie contractul, bootstrap-ul Maven/IDE
și păstrarea istoricului. [Sincronizarea](ATF_Workspace_Synchronization.md)
cere manifest explicit, verificarea drift-ului și promovare selectivă.

Dovezile acestui increment sunt în staging-ul extern
ATF_JSON_C2_J9_ACTIVE_DO_NOT_CLEAN_f8f75e12: acceptance-evidence.json, logurile,
manifestele publicațiilor și cele patru HTML-uri din publication. Sursa retrasă
și perechile vechi se arhivează recuperabil; output-urile nu intră în live.
