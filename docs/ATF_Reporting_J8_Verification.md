# Reporting J8 — validare API colapsabilă

## Pe scurt

Livrarea este `atf.api:atf-reporting:1.0.0-j8`, Java 21, thin JAR/POM.
J0 păstrează sursa și regresiile; J1/J2/J3 consumă aceeași pereche direct în
`lib`, cu o singură versiune Reporting. Core nu se reconstruiește.

| Boundary | Contract |
|---|---|
| cauză J7 | fallback publica direct assertions API; disclosure-ul era activ numai în prezentarea UI compactă |
| corecție | tabelul nevid folosește shell-ul standard, închis inițial; validarea goală nu inventează conținut |
| accesibilitate | mouse și Enter/Space comută conținutul; aria-expanded urmărește starea nativă, inclusiv după backlink |
| integritate | owner API, patru coloane, comparații și PASS/FAIL/WARNING originale; redacție înainte de preview 240 |
| Extent | cardul validation existent rămâne controlul de colapsare; diagnosticul nu substituie tabelul original |
| compatibilitate | 4267 simboluri public/protected înainte și după; zero eliminări sau adăugări |

## Dovada livrării

SHA-256 JAR: `F8176C6D35B39998D8289A53026F526D87DC3082049064DEE4C57BEF94463C90`.
SHA-256 POM: `E0DA4D9B0A9BA7BB090B954E74A880B9865BE5A18F7F6EEDB987F377D63DE5E2`.

| Probă TMP | Rezultat |
|---|---|
| reproducere J7 | 3/4 native refuzate; toate cele șase nominale fallback refuzate, Extent verde |
| native J8 | 56, zero eșecuri/erori/skip; două build-uri curate cu bytes identici |
| unit Reporting | 486, zero eșecuri/erori/skip |
| regresii adiacente | 25 probe: assertions de componentă, UI setup, unified flow, tabele, disclosure, selectors și distribuție sursă |
| browser pe sursă | 22: 12 nominale, șase mutanți disclosure și patru cazuri media cu verdict mixt; Extent/fallback |
| suită pe JAR | aceleași 22 și identitatea fizică a JAR-ului; fără surse Reporting |
| standalone pe JAR | un gate: runtime fără Core/Kit/JUnit/Playwright, ambele renderere; asset lipsă refuzat și artifact anterior păstrat |
| verdict per rând | patru cazuri media refuză schimbul PASS ↔ FAIL/WARNING între rânduri, chiar când totalul negativ rămâne identic |
| buget assets | JS publicat 106990 → 107384 bytes; +394 pentru ARIA, plafon 107500 și owner fallback 2848; mutant +1 refuzat |
| J1 | 22 ShopConfiguration, opt AutoEx publisher, 155 guvernanță |
| J2 | trei componente MOCK prin suită în fiecare renderer, una individual fallback, 11 contracte terminale, 40 artifacte Validation, 14 artifacte Mock și 166 guvernanță |
| J3 | patru contracte fixture/observations și șapte guvernanță; fără recognizer real |
| TAB MCP | raportul scalar J2 original și nou, individual și agregat; colapsare, mouse/Enter/Space, ultima comparație și backlink/reload |
| guvernanță sursă J0 | 155 controale, patru eșecuri existente de coeziune/lizibilitate; criteriul REPORT-J7-GOVERNANCE-100 rămâne deschis |

## Probe și limite

Acceptanța este focalizată și executată în TMP.

| Boundary | Limită observabilă |
|---|---|
| proveniență | hash-ul candidatului și istoricul J6/J7 rămân distincte; rezultatele vechi nu sunt moștenite |
| fixture terminal J2 | serializarea precede deadline-ul de execuție de 250 ms; pragul și excepția originală a sink-ului sunt păstrate |
| regresie permanentă | [ownerul J0](../../atf_j0/atf-reporting-tests/README.md#validarea-api-colapsabilă-în-fiecare-renderer) deține fixture-ul și oracolul neutru |
| scope | incrementul corectează colapsarea API; brokerul real, recognizerul speech și suitele business web complete cer probe separate |
| restanțe | timing-ul, aplicabilitatea inspectorului și geometria rămân în [feedback-ul Reporting J0](../../atf_j0/docs/feedback/ATF_Reporting_Feedback.md) |
| Warning la 1280 px | RESULT 56,99 px, badge 61,79 px, scrollWidth 65 și clientWidth 56; receipt-ul de valori nu certifică absența overflow-ului |
