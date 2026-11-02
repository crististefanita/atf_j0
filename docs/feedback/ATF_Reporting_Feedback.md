# ATF Reporting — contracte, responsabilități și pregătire JAR

## Pe scurt

Reporting deține publicarea neutră, sumarul sigur, query-ul typed și proiecția
cauzei terminale. Sursa izolată și JAR-ul fizic au gate separat în
[Reporting Tests](../../atf-reporting-tests/README.md#gate-ul-bibliotecii-distribuite).

- Pregătirea pentru release păstrează criterii distincte: API susținut,
  distribuție și documentație portabilă. Expected-urile, endpoint-urile,
  selecția business și captura Playwright rămân la ownerii lor.
- Reporting Tests rămâne modul intern de verificare. Fiecare increment închide
  împreună codul, ownership-ul, proba și documentația.

## Criterii rămase pentru consolidare

| Ordine | Implementare și probă de închidere | Owner al oracolului |
|---:|---|---|
| 1 | porturile declară capabilitățile și compatibilitatea susținută; nicio dovadă cerută nu dispare într-un default fără efect | consumer JAR și contracte neutre |
| 2 | proiecția recovery și flow-ul păstrează owner, cauze, timing și ordinal înainte/peste faze | comparație semantică, Extent/fallback și controale negative |
| 3 | distribuția declară API-ul, matricea vendor și documentele transportabile | gate de release și [Reporting Tests](ATF_Reporting_Tests_Feedback.md) |

Constructorul refuzat eliberează publicația, iar lifecycle-ul rendererelor este
la [contractul sesiunii](../../atf-reporting/README.md#publicare-și-ieșire).
Assets respectă inventarul executabil; aceste probe nu închid automat release-ul extern.

Fiecare extracție păstrează fațada și compatibilitatea necesară callerilor reali.
Justificați motivul de schimbare și proba independentă; o clasă nouă ori un
package mutat nu constituie singure o îmbunătățire de arhitectură.

## Ordinea pregătirii

Auditul structurii sursei aparține build-ului producătorului. Probele runtime și
consumerul Maven extern verifică aceeași bibliotecă și ca JAR; contractele livrate
sunt la ownerii lor, iar registrul păstrează numai criteriile încă deschise.


- Restanțele de mai jos decid următorul increment. Contractele livrate sunt
  legate la [ownerii lor](#contracte-existente); un gate focalizat nu închide
  automat compatibilitatea API sau pregătirea întregului release.
- Scorul este prioritate: impact /40 + lacună /30 + lucrabil /20 + datorie
  documentară /10. Nu este o notă a modulului ori un procent de acoperire.

| ID | Scor /100 | Criteriul care blochează închiderea |
|---|---:|---|
| [REPORT-SQL-STEPS-116](#report-sql-steps-116--query-uri-și-rezultate-auditabile) | 88 = 35 + 26 + 18 + 9 | pași SQL neutri, query/bind-uri și rezultate cu bugete independente; adoptare J2 după release |
| `REPORT-PUBLIC-API-43` | 81 = 31 + 24 + 17 + 9 | API-ul susținut, compatibilitatea și capabilitățile adaptoarelor nu sunt delimitate suficient |
| [Reporting Tests](ATF_Reporting_Tests_Feedback.md#report-test-audit-60--o-dovadă-identificabilă-pentru-fiecare-scenariu) | 80 = 31 + 24 + 18 + 7 | criteriu separat al consumerului: oracol per scenariu și containment fallback |
| `UI-RECOVERY-AUDIT-01` | 78 = 33 + 22 + 16 + 7 | proiecția typed a încercărilor, bugetelor și corelării rămâne la Reporting |
| `REPORT-RELEASE-CONTRACT-45` | 73 = 25 + 22 + 17 + 9 | lipsesc criteriile declarate ale pachetului și versiunii distribuite |
| `REPORT-PORTABLE-DOCS-46` | 72 = 24 + 22 + 17 + 9 | contractele și linkurile README depind încă de documente din afara modulului |
| [REPORT-FALLBACK-RESULT-WIDTH-101](#report-fallback-result-width-101--badge-ul-statusului-depășește-coloana) | 64 = 24 + 19 + 15 + 6 | badge-ul Warning depășește RESULT și produce overflow evitabil |
| `REPORT-FLOW-PROJECTION-36` | 62 = 24 + 17 + 16 + 5 | calculul proiecției și protocolul publicării rămân amestecate |

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":40,"padding":10,"diagramPadding":16}}}%%
flowchart LR
    Gap["Un criteriu"] -->|corectează| Code["Cod și owner"]
    Gap -->|demonstrează| Proof["Probă independentă"]
    Gap -->|aliniază| Docs["Contract README"]
    Code -->|livrează| Gate["Același increment"]
    Proof -->|validează| Gate
    Docs -->|explică| Gate
```

<details>
<summary><strong>Legendă — închiderea comună a codului și documentației</strong></summary>

- Regula observabilă și ownerul decid refactorizarea; numărul de clase sau linii
  nu este un motiv suficient pentru un helper ori modul nou.
- Proba refuză comportamentul anterior și consumă API-ul ori artefactul real.
  README-ul descrie numai rezultatul demonstrat, cu limitele păstrate.
- Un criteriu se închide după reconcilierea celor trei ramuri. Contractul stabil
  se mută la owner, iar controlul testelor în baseline; feedback-ul păstrează numai restanțele.

</details>



## REPORT-PUBLIC-API-43 — suprafață susținută și ownership

**Prioritate: 81/100** = impact 31 + lacună 24 + lucrabil 17 + documentare 9.
**Owner:** fațadele `AtfReportPorts`, `ExtentReportSession`, `HtmlReportSession`,
`UiExtentReportSession`, modelele și adaptoarele publice.

Accesul Java `public` include și implementare: `ReportAssetComposer`,
`ReportPublicationScope`, `ExtentReportTree` și adaptorul timestamp Spark.
Namespace-ul întreg nu poate deveni automat o promisiune de compatibilitate.

- `AtfReportPorts` referă `ReportNetworkInspector.JourneyAggregate`, în timp ce
  inspectorul de proiecție folosește contractele de assertions. Modelul de
  boundary rămâne legat de clasa care calculează sumarul.
- Compatibilitatea `Object`/reflection și numeroasele overload-uri UI sunt
  păstrate în producție. Schimbarea lor cere inventarul apelanților, inclusiv
  main-ul Shop/AutoEx; extragerea nu autorizează ștergerea acestor punți.
- Unele porturi opționale au implementări implicite fără efect. Un adaptor
  extern are nevoie să știe ce evidence este suportat înaintea publicării.
- `ReportAuditSnapshot.Failures` are cinci componente pentru cauze și scenarii
  distincte. Constructorul legacy și simbolurile binare sunt păstrate; un record
  pattern Java cu patru componente cere adaptare la recompilare, probată separat
  cu `javac`. Delimitați explicit evoluția recordurilor în API-ul susținut.

**Același increment:**

- [ ] Delimitați intrări susținute, modele publice, extensii, bridge-uri legacy
      și implementare internă. Inventariați folosirile reale înainte de restrângerea
      vizibilității ori mutarea pachetelor; păstrați punțile necesare.
- [ ] Evaluați separarea modelelor neutre de calculul lor și folosirea
      `UiExtentReportOptions` ca intrare coerentă, cu overload-uri vechi delegante.
      Nu creați clase sau module numai pentru a reduce dimensiunea fișierelor.
- [ ] Stabiliți capabilități și refuzuri explicite pentru adaptoare: timing,
      assertion-step, screenshot, aggregate și nested request. Compatibilitatea
      nu justifică o dovadă cerută și pierdută fără diagnostic.
- [ ] Automatizați compatibilitatea suprafeței susținute și politica versiunilor.
      Consumerul extern și comparația binară a incrementului există; ele nu
      declară automat suport pentru fiecare clasă Java publică.

## REPORT-RELEASE-CONTRACT-45 — ce conține și ce promite pachetul

**Prioritate: 73/100** = impact 25 + lacună 22 + lucrabil 17 + documentare 9.
**Owner:** [POM Reporting](../../atf-reporting/pom.xml), manifestul de assets și procesul de release.

JAR-ul inspectat conține clasele de producție și toate resursele CSS/JS din manifestul finit;
loaderul le citește din classpath. Nu este demonstrată o lipsă de asset în
artefactul curent și nu propunem mutarea lor în Java sau într-un modul nou.

- POM-ul fixează compiler/Surefire/JAR, timestamp-ul arhivei și dependențele
  directe, inclusiv FreeMarker. Sources/JavaDoc și metadatele de licență/SCM
  cer încă o decizie explicită pentru distribuția externă.
- Adaptorul timestamp citește un template intern Spark. Compatibilitatea
  Extent trebuie urmărită; macro-ul refuză expresia vendorului schimbată.

Două build-uri forțate `clean package` cu aceleași surse și aceeași toolchain
au produs bytes identici. Restanța privește toolchain-ul declarat și gate-ul
automat de release, împreună cu metadatele și dependențele efectiv susținute.

**Același increment:**

- [ ] Declarați matricea runtime/vendor susținută și politica versiunilor.
      Java 21, JAR thin/POM și dependențele directe sunt deja declarate;
      compatibilitatea viitoare cere o probă la fiecare versiune susținută.
- [ ] Automatizați gate-ul de release cu două build-uri forțate și intrări
      identice, toolchain declarată și provenance verificată. Pluginurile fixate
      și proba locală repetabilă nu înlocuiesc politica livrării externe.
- [ ] Verificați conținutul JAR/POM, inventarul assets, lipsa testelor/dependențelor
      ATF din runtime și rezolvarea externă. Decideți sursele/JavaDoc și metadatele
      de distribuție înainte de publicare; fără alegere implicită a licenței proiectului.
- [ ] Folosiți J0 ca owner al candidatului și validați separat J1/J2 după inițializarea fiecărei ținte; aceeași pereche JAR/POM și hash, fără moștenirea rezultatelor altui consumer sau rescrierea provenienței istorice.
- [ ] Documentați instalarea/consumul real și diagnosticul unei incompatibilități
      de runtime ori vendor; `mvn package` verde rămâne doar o parte a acceptanței.

## REPORT-PORTABLE-DOCS-46 — adopție externă și documente transportabile

**Prioritate: 72/100** = impact 24 + lacună 22 + lucrabil 17 + documentare 9.
**Owner:** README Reporting și contractele de raportare din documentația proiectului.

README-ul are linkuri relative spre directoare părinte, inclusiv contracte
normative din `../docs`. Copierea numai
a modulului păstrează codul, dar nu transportă acei owneri documentari.

- Exemplul minim JAR din README este acum compilat și executat extern, cu un
  control PASS și unul FAIL; nu închide migrarea tuturor contractelor.
- [Atlasul pachetelor](../../atf-reporting/src/main/java/atf/reporting/README.md)
  oferă context local pentru UI, API, modele, renderere, evidence și runtime.
  Linkurile sale către probele interne nu reprezintă încă un pachet documentar de release.
- [Contextul portabil](../../atf-reporting/docs/README.md) are numai destinații
  interne modulului și explică separat sursa izolată, JAR-ul thin, POM-ul și
  autonomia HTML. Această intrare este pregătită; portabilitatea tuturor
  README-urilor și includerea în distribuție rămân deschise.
- README-ul separă acum adopția, mentenanța și probele prin trasee explicite
  și disclosure-uri. Acest lucru îmbunătățește orientarea în ATF; nu transportă
  automat contractele externe în pachetul distribuit.

**Același increment:**

- [ ] Inventariați ownerii contractelor și destinația lor după extragere.
      Păstrați o singură definiție normativă, cu linkuri funcționale atât în ATF,
      cât și în livrarea externă; fără duplicarea a două versiuni divergente.
- [ ] Includeți traseul de adopție și contractele lui în livrarea declarată.
      Actualizați exemplele odată cu API-ul și probați-le pe consumerul extern.
- [ ] Verificați din copia izolată README-ul, linkurile, fragmentele compilabile,
      configurația și randarea diagramelor; guard-ul structural nu dovedește adevărul.
- [ ] Mențineți [controlul factual selectiv](../../atf-tests/README.md#documente-și-responsabilități)
      ca regulă comună; criteriul de aici deține numai portabilitatea documentației Reporting.

## REPORT-FLOW-PROJECTION-36 — plan de dovezi separat de publicare

**Prioritate: 62/100** = impact 24 + lacună 17 + lucrabil 16 + documentare 5.
**Owner:** Reporting; oracolele neutre și browserul rămân în Reporting Tests.

Extragerea `ApiEvidencePublisher` și conservarea cauzei sunt livrate.
`UiEvidencePublisher.publishUnifiedFlow(...)` și `publishCrossPhaseUnifiedFlow(...)`
reunesc încă selecția propoziției, compactarea, agregarea verdictelor UI/API,
selectarea capturii și deschiderea/închiderea porturilor de publicare.

| Motiv de schimbare | Boundary de păstrat |
|---|---|
| Modificarea grupării checkpoint-urilor | transformare pură peste intrările typed, cu ordine și identitate păstrate |
| Politica screenshot/compact UI-only | decizie Reporting explicită; nicio captură nouă la renderer |
| Failure la body/complete/close | publicare peste porturile existente, cu aceeași cauză și fără repetare |

- [ ] Caracterizați independent rezultatul proiecției pentru o fază și peste
      faze, UI/API/UI+API, compact și necompact. Păstrați faza sursă, titlul,
      ownership-ul, verdictul, ordinea comparațiilor și captura originală.
- [ ] Extrageți numai calculul care poate fi probat fără porturi, HTML ori
      browser, dacă acest lucru separă efectiv motivele de schimbare. Fațada
      rămâne stabilă; nu introduceți un API public, un modul sau o clasă numai
      pentru reducerea numărului de linii.
- [ ] Publicarea consumă rezultatul typed și reutilizează publisherul API și
      protocolul scope-urilor; nu reface decizia din textul compactat și nu
      schimbă păstrarea cauzelor pentru a simplifica bucla.
- [ ] Păstrați `UiCrossPhaseCompactFlowComponentTest`,
      `ExtentUnifiedFlowMultipleScreenshotTest`, `UiPublicationFailureComponentTest`
      și inventarul independent al tabelelor. Probați FAIL/SKIP într-o fază
      intermediară și checkpoint-uri cu API fără UI, nu doar flow-ul final PASS.
- [ ] Comparați Extent/fallback și navigarea între taburi pe același snapshot;
      bugetul configurat, timing-ul, un singur artifact și toate expandările accesibile
      rămân criterii, inclusiv [backlink-ul exact](../../atf-reporting/README.md#revenirea-la-comparația-eșuată).

**Închidere:** politica de grupare poate fi schimbată și probată separat de
protocolul porturilor, cu ieșire observabilă identică și fără evaluări repetate.

## UI-RECOVERY-AUDIT-01 — încercări inspectabile în Extent

**Prioritate: 78/100** = impact 33 + lacună 22 + lucrabil 16 + documentare 7.
**Owner:** model/proiecție la Reporting; probe renderer la Reporting Tests;
adaptoare și aserțiuni business în main la cei doi consumeri.

Observațiile INFO livrate sunt normate la
[Reporting](../../atf-reporting/README.md#decizii-be-fără-verdict-de-succes-implicit).
`UiRecoveryTrace` deține deja operația, încercarea, decizia, motivul, semnalul BE
și duratele; adaptoarele AutoEx/Shop le publică în rânduri INFO. Restanța este
proiecția structurată comună și matricea de acceptanță, fără parsarea mesajelor
sau repetarea deciziei la renderer.

- [ ] Compuneți faptele typed din trace, policy și signal în proiecția comună:
      operație, încercare, decizie/motiv, sursa politicii, bugete și timpi măsurați.
      Signal-ul păstrează deja scope-ul invocare/operație/frame/document și secvența;
      rendererul primește corelarea neutră de la adaptor, fără parsarea mesajului.
      Starea DOM apare numai dacă a fost observată; lipsa ei rămâne necapturată,
      fără acțiuni sau capturi suplimentare pentru completarea tabelului.
      Distingerea observației de replay trebuie să fie vizibilă în raport.
      Separați durata încercării, backoff-ul și timpul total măsurat; o durată
      indisponibilă rămâne explicit necapturată, fără valoare HTTP inventată.
- [ ] Păstrați toate cauzele intermediare și capturile permise. Recuperarea
      înaintea eșecului terminal poate avea rânduri INFO explicite, conform
      recorderului existent; nu rescrieți un FAIL deja publicat în PASS și nu
      înlocuiți raportul original cu artifactul unei rulări ulterioare.
- [ ] Definiți verdictul logic și indicatorul de recuperare: un singur test
      în totaluri, succes inițial distinct de recuperare, epuizare FAIL și abort
      păstrat SKIP. Retry-ul infrastructurii de publicare nu rerulează business-ul.
      Inputul contradictoriu este reconciliat conform
      [contractului comun Reporting](../../atf-reporting/README.md#consistența-statusului-transmis-de-caller).
- [ ] Probați expand/collapse până la fiecare încercare, acțiune, request și
      aserțiune; Fails → test/fază/pas exact și Overview trebuie să păstreze
      cauzele și totalurile coerente. Captura ulterioară nu înlocuiește starea
      consemnată la eroare.
- [ ] Verificați câte un caz relevant individual și prin suită în AutoEx și
      Shop, cu profil UI și UI+API, succes, recuperare/refuz și epuizare. Probele
      Extent neutre rămân în `atf-reporting-tests`; policy/resolver și adaptoarele
      au teste la owner, fără cuplarea testelor de business la HTML.


## Contracte existente


| Informație | Owner |
|---|---|
| Exchange terminal în Tests/Fails și backlink către execution evidence | [Reporting](../../atf-reporting/README.md#execuție-api-terminată-fără-răspuns) |
| JSON semantic, canary-uri și degradarea inputului invalid | [Security](../../atf-reporting/src/main/java/atf/reporting/security/README.md) |
| YAML strict și defaults legitime | [Reporting](../../atf-reporting/README.md#intrări-și-configurare-în-bibliotecă) |
| Sursa izolată și JAR-ul fizic consumat extern | [Reporting Tests](../../atf-reporting-tests/README.md#gate-ul-bibliotecii-distribuite) |
| Buget configurabil și valoarea efectivă în System | [Reporting](../../atf-reporting/README.md#proiecția-sigură-a-aserțiunilor) |
| Lifecycle și refuzul constructorului | [Reporting](../../atf-reporting/README.md#publicare-și-ieșire) |
| Cauza publicării directe și închiderea scope-urilor | [Reporting](../../atf-reporting/README.md#cauza-publicării-și-scope-urile-deținute) |
| Tranziții de scope și refuz fără mutație | [Reporting](../../atf-reporting/README.md#atomicitatea-tranzițiilor-de-scope) |
| Timing explicit, capturi și publicare | [Reporting](../../atf-reporting/README.md#timing-e2e-la-ownerul-runtime) |
| Setup și dovezi UI în fallback | [Reporting](../../atf-reporting/README.md#evidence-ui-și-setup-în-fallback) |
| Coloane dinamice și scroll local | [Reporting](../../atf-reporting/README.md#lizibilitatea-dovezilor-în-panouri-înguste) |
| Probe de compatibilitate și browser | [Reporting Tests](../../atf-reporting-tests/README.md) |
| Oracole, fixture-uri binare și dovezi exacte | [Feedback Reporting Tests](ATF_Reporting_Tests_Feedback.md) |
| Inventar UI/API/UI+API și controale negative de randare | [Reporting Tests](../../atf-reporting-tests/README.md#tabele-publicate-și-inventar-independent) |
| Controale Git verificate | [Baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate) |
| Contractele consumerilor | [Acceptanța business](../ATF_Testing_Guide.md#acceptanța-business-individual-și-prin-suită) |
| Identitatea query-ului între modele | [Reporting Tests](../../atf-reporting-tests/README.md#fidelitatea-query-ului-și-asocierea-mock) |

Refactorizările păstrează contractele ownerilor de mai sus și urmează
[ghidul de probă](../ATF_Testing_Guide.md#proba-fiecărui-owner-de-resurse).
Reevaluarea folosește [rubrica comună de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor),
cu randare inspectată și regresiile ownerului trecute în TMP.

## REPORT-ARTIFACT-PROFILE-62 — aplicabilitate explicită a inspectorului

Contract consolidat în [README-ul Reporting](../../atf-reporting/README.md#inspecția-artifactului-cu-intenție-explicită), cu [probe J12](../ATF_Reporting_J12_Verification.md). Intenția callerului și rezultatul complet sunt explicite; overload-ul legacy rămâne Extent/HTTP. Regresia neutră și binară refuză inventarul degradat; adopția overload-ului în acceptorii aplicațiilor rămâne responsabilitatea consumerului.

## REPORT-RECORDED-TIMING-83 — execuție capturată și cost de publicare

Contract consolidat la [originea duratelor](../../atf-reporting/README.md#originea-duratelor-sub-o-fază-capturată). [J12](../ATF_Reporting_J12_Verification.md) păstrează execuția capturată, transportul și publicarea distincte, cu barieră controlată și probe individual/agregat în ambele renderere.

## REPORT-FALLBACK-RESULT-WIDTH-101 — badge-ul statusului depășește coloana

**Prioritate: 64/100** = impact 24 + lacună 19 + lucrabil 15 + documentare 6.
**Owner:** Reporting pentru layout; Reporting Tests pentru componenta neutră.

Pe J7, tabelul API cu trei comparații PASS și un WARNING afișează un scrollbar
orizontal mic la viewport de 1280 px. Coloana RESULT măsoară aproximativ 56,9 px,
iar badge-ul Warning 61,8 px; overflow-ul tabelului ajunge la 877 px pentru
un părinte de 870 px. Valorile și verdictul sunt corecte.

- Calculați minimumul util din footprint-ul stilizat al celulei, inclusiv badge, padding și border; lungimea textului singură nu dovedește că rezultatul încape.
- Refolosiți politica dinamică existentă pentru toate coloanele și rendererele. Nu impuneți o proporție globală fixă și nu ascundeți overflow-ul sau textul ca să eliminați scrollbarul.
- Păstrați wrapping, patru coloane, un singur tabel și preview 240; un panou într-adevăr îngust poate avea scroll limitat la tabel, cu ultima comparație accesibilă.

**Închidere:** fixture-ul neutru PASS/FAIL/WARNING încape la lățimea normală
fără overflow provocat de badge; panoul îngust rămâne navigabil. Mutația care
restrânge RESULT sub dimensiunea badge-ului trebuie refuzată separat de verdict.

<details>
<summary><strong>Reproducere consumer — fără schimbarea rendererului</strong></summary>

- Increment TMP atf-j2-media-cd3973d608; media-matrix/fallback-media-warning.html.
- HTML SHA-256: 59cccb14ff23d6fcb659591510e1e982e9a3b315f04a89b799bc2fe0d29f1ed4.
- JAR J7 SHA-256: 569d2babe6b9ec091cb1df6e1207110c611327ea15f1521955e847c837d9eb2a.
- Browser: Tests, patru rânduri; Content-Type JSON are WARNING și ceilalți trei PASS.
- DOM măsurat: RESULT clientWidth 56, scrollWidth 65; badge width 61,8; tabel width 869,6 și scrollWidth 877.
- Receipt-ul de valori/verdicturi trece; el nu certifică geometria, care cere regresia de componentă dedicată.

</details>

Revalidarea J8 păstrează criteriul 101 deschis: pe raportul media-warning J2,
TAB MCP măsoară la viewport 1280 px RESULT 56,99 px și badge Warning 61,79 px
(clientWidth 56, scrollWidth 65). Colapsarea și verdicturile sunt corecte;
regresia geometrică neutră cerută rămâne distinctă de testele de disclosure.


## REPORT-SQL-STEPS-116 — query-uri și rezultate auditabile

**Prioritate: 88/100** = impact 35 + lacună 26 + lucrabil 18 + documentare 9.
**Owner:** Reporting pentru contractul neutru și ambele renderere; Reporting
Tests pentru [probele dedicate](ATF_Reporting_Tests_Feedback.md#report-test-sql-steps-116--probe-dedicate-sql).
Acesta este un increment cerut, fără implementare sau versiune JAR livrată.

J2 are acum opt module sursă, prin adăugarea explicit cerută a bibliotecii atf-sql JDK-only: template, binds typed, metadata ordonată și celule capturate din aceeași execuție. Proiecția veche SqlEffectsSnapshot rămâne separată; noul adaptor publică deocamdată observations JSON, fără tabel SQL semantic.

- AtfReportPorts.Phase nu are un pas SQL semantic.
- Query-ul typed deja existent în Reporting descrie parametrii URI HTTP, nu interogări SQL.

| Responsabilitate | Contract cerut |
|---|---|
| consumer JDBC | capturează o singură execuție, query template, bind-uri ordonate și tipizate, schema și rândurile înainte de închiderea resurselor |
| Reporting | primește fapte imuabile și publică pas SQL colapsabil, la același nivel semantic cu UI/API, fără a executa interogări |
| assertions | comparația integrală și verdictul rămân la evaluator; aceeași zonă standard cu patru coloane și preview implicit 240 |
| distribuție | API cu capabilitate explicită și compatibilitate documentată; JAR/POM și acceptanță J2 după release-ul J0 |

Nu introduceți driver H2/JDBC ori dependențe Eventuate în Reporting. Nu păstrați
Connection, Statement sau ResultSet și nu reluați query-ul, COUNT sau comparația
pentru completarea raportului. Expected-urile și politica de business rămân la consumer.

<details>
<summary><strong>Contractul faptelor și al randării SQL</strong></summary>

- Un ID de execuție leagă faza, query-ul, parametrii, rezultatele, timing-ul și
  cauza; titlurile identice cu bind-uri diferite nu permit asocierea după text.
- Query template și bind-urile sunt separate; nu construiți SQL executabil prin
  interpolare. Bind-urile păstrează poziția/numele, tipul, NULL și politica de
  sensibilitate; conexiunea este un alias opac, fără URL cu credențiale.
- Schema este o listă ordonată cu ordinal, etichetă și tip. Etichetele duplicate
  nu devin chei Map care pierd coloane; rândurile păstrează ordinea și multiplicitatea.
- NULL, textul null, șirul gol, binarul și numerele exacte sunt distincte.
  BigDecimal și întregii mari nu trec prin Double; redacția și pretty-print-ul
  respectă [contractul numeric livrat](../../atf-reporting/src/main/java/atf/reporting/security/README.md).
- Separați execuția eșuată/anulată, rezultatul complet gol, captura parțială și
  trunchierea de afișare. Numărul total necunoscut rămâne necunoscut, fără COUNT
  suplimentar; un rezultat tăiat nu este prezentat ca zero rânduri complete.
- Un SQL error așteptat poate aparține unui test PASS; observația nu inventează
  verdictul. Eșecul neinterceptat rămâne FAIL; cleanup-ul și publicarea nu
  înlocuiesc cauza originală și nu provoacă replay.
- Timing-ul explicit separă execuția/captura de publicare, păstrând necunoscut
  versus zero. Nu închideți contractul execuției capturate printr-o durată calculată din HTML.
- Query, bind-uri, Results și Assertions au disclosure-uri accesibile și owner
  precis. Extent și fallback păstrează coloane dinamice pe lățimea disponibilă,
  scroll până la ultima coloană/rând, Enter/Space și Fails către pasul original.
- Un profil SQL-only legitim nu primește HTTP fictiv pentru inspector;
  profilul explicit J12 se declară la caller; apelul legacy rămâne strict.

</details>

### Bugete configurabile pentru payload-ul SQL

Valorile următoare sunt defaults propuse pentru noul contract, nu proprietăți
deja disponibile în J9. Query-ul și rezultatele au configurări independente;
limita 240 a preview-ului assertion-urilor rămâne neschimbată.

| Buget | Default propus | Unitatea și aplicarea |
|---|---:|---|
| query | 4.096 | caractere vizibile pentru template-ul unei execuții |
| bind-uri | 2.048 | caractere vizibile cumulate pentru parametrii unei execuții |
| rezultate | 16.384 | caractere vizibile cumulate pentru schema și toate celulele unei execuții; nu per celulă |
| payload SQL variabil per raport | 1.048.576 | unități UTF-16 serializate pentru query/bind-uri/schema/celule/omisiuni, inclusiv copii DOM/JSON și escaping; assets și metadatele standard de identitate/verdict sunt excluse |

- Numărați preview-ul în code points Unicode după redacție și formatare;
  tăierea păstrează grapheme/surrogate valide și nu depășește limita aleasă.
  Bugetul serializat distinct previne amplificarea prin escaping ori copii DOM.
- Validați configurația înainte de publicare: întregi pozitivi, fără overflow,
  lipsă de valoare sau dezactivare implicită a plafonului; capturați politica
  imuabil la crearea sesiunii. Numele finale și intervalele acceptate sunt ale API-ului J0.
- Numerele maxime includ markerul și indicatoarele de omisiune din zona
  plafonată. Rezervați structură HTML validă, ID și diagnostic minimal bounded;
  nu tăiați HTML/JSON serializat în mijlocul tagului sau al valorii.
- O epuizare indică explicit afișat/original când originalul este cunoscut,
  câte rânduri/coloane sunt omise și motivul. Trunchierea capturii și cea de
  prezentare rămân distincte; verdictul integral nu se reevaluează pe preview.
- Plafonul cumulat are contabilizare deterministă per sesiune și rezervare
  înaintea serializării, inclusiv la publicare concurentă. După epuizare nu
  repetați un marker voluminos pentru fiecare pas; rezervați un sumar bounded
  pentru omisiuni și păstrați identitatea/verdictul în metadatele standard.
- Interziceți payload-ul integral ascuns în script, atribute, DOM, base64 ori
  indexul de căutare. Un link la un artifact extern complet cere politică
  explicită, hash și redacție proprie; nu se include automat versiunea integrală în HTML.
- Redactați înainte de măsurare, în toate canalele. Template-ul și bind-urile
  primesc sensibilitate explicită de la sursă; un regex nu certifică ștergerea
  tuturor literalelor secrete din orice dialect SQL.

**Închidere:** model și capabilitate publică, limite validate, randări Extent și
fallback și componentele neutre de mai sus trec pe sursă și JAR fizic. Publicați
compatibilitatea și proveniența candidatului; apoi J2 capturează query/bind-uri
și adoptă separat contractul cu witness-ul real H2. Până atunci, observations
generice rămân comportamentul livrat; această cerere nu certifică SQL LIVE.

### Handoff producer J2 — 2026-11-01

- [Contractul SQL J2](../../../atf_j2/atf-sql/README.md) oferă acum faptele typed și un ID per execuție. Reporting primește o proiecție neutră, fără dependență către JDBC, H2 sau modulul SQL consumer.
- Matricea J2 are 12 HTML-uri/36 invocări și patru FAIL-uri JDBC controlate, cu oracol pe query, binds, duplicate labels/rows, NULL/gol, DECIMAL exact și ownership. Mutanții ID/query lipsă și număr rotunjit sunt refuzați; aceasta nu substituie componentele permanente J0 ale noului API SQL.
- Captura JDBC implicită are 4.194.304 unități conservative; nu este plafon HTML. Păstrați cerința distinctă pentru query 4.096, binds 2.048, rezultate 16.384 și total SQL 1.048.576, toate configurabile, fără payload complet ascuns. Preview-ul assertions rămâne 240.
- [Dovada J2](../../../atf_j2/docs/ATF_SQL_Live_Verification.md) separă H2, Kafka/MySQL reale și aplicația Eventuate NOT RUN. Nu certificați saga sau dialecte SQL prin fixture-ul neutru al rendererului.

## Reconfirmarea J2 — criterii încă deschise

The current J2 native SQL matrix again has 12 legitimate SQL-only artifacts:
36 invocations, four intended SQL failures and passing independent oracles/
mutants. These historical J10 artifacts were noncompliant under the legacy Extent/HTTP inspector. J12 adds an explicit profile; SQL-STEPS-116 remains open for neutral SQL steps/budgets and consumer adoption.

- Proof roots in TMP: j2-quality-64e05d574c24 and j3-host-run-c79f1aaffbba.
- These observations do not execute the J0 native/browser regression suite.


## Diagnostic bounded al assertions — criteriu separat de verdict

- Two different long values can correctly yield FAIL while their 240-character
  previews are identical. The new browser fixture preserves that FAIL, its exact
  Fails backlink and the neighboring PASS comparison.
- If richer diagnosis is required, capture a safe reference or bounded redacted
  mismatch context before information is discarded. Keep 240; do not publish
  entire hidden originals for already-bounded assertions or derive a verdict from display text.
- Kit's component Check already stores sanitized previews. Reporting cannot
  reconstruct a missing suffix from those values. Keep neutral capture at the
  test/evaluator boundary and rendering in Reporting; no second comparison.
- Treat differences hidden by redaction separately. Avoid raw secret digests,
  lengths or context as an accidental disclosure; an explicit withheld reason
  can be the correct safe evidence.

Evidence root: TMP j0-deep-audit-1057ca2f02ed, reporting-probe.log and
source-reports. Two browser-audited source artifacts are controlled fixtures,
not SQL, HTTP, ASR or device acceptance. No implementation changed.

## REPORT-FALLBACK-ASSERTION-AFFORDANCE-124 — indicator vizual pentru assertions

Contract livrat [J12](../ATF_Reporting_J12_Verification.md): disclosure comun, indicator vizibil închis/deschis/reînchis, mouse/Enter/Space și mutant care ascunde numai glyph-ul. Assertions, statusurile și preview-ul 240 rămân conservate.

## Increment J12 — contracte și dovezi

[Dovada J12](../ATF_Reporting_J12_Verification.md) deține hash-ul final, selecțiile executate, limitele și acceptanțele distincte J1–J4. Criteriile 62, 83 și 124 nu mai sunt ToDo-uri. PUBLIC-API-43 rămâne deschis pentru delimitarea API-ului susținut și capabilitățile adaptoarelor; invarianta rezultatului de inspecție este rezolvată.

## Contractul rândului UI

Fidelitatea și redacția comună sunt în [contractul modulului](../../atf-reporting/README.md#fidelitatea-rândului-ui-și-politica-de-redactare), cu [regresii sursă/JAR J14](../ATF_Reporting_J14_Verification.md). Criteriul 135 este verificat și nu mai reprezintă un ToDo.

[Contractul inline](../ATF_Inline_Audit_Contract.md) și [regresia J15](../ATF_Reporting_J15_Verification.md) închid fidelitatea PRE pentru TEXT. Criteriul 133 al assertions deja limitate și remedierea semanticii consumerilor rămân separate.

## REPORT-SCREENSHOT-RETURN-137 — PNG reutilizat schimbă ownerul și pierde expandările

**Prioritate P1; observat pe JAR-ul fizic J15.** Un click pe o captură deduplicată trebuie să deschidă aceiași bytes și să revină la aceeași execuție, fază și poziție de audit.

| Dovadă | Rezultat observat |
|---|---|
| raport J4 real recompus | după viewer-ul PNG final, arborele se colapsează; scroll-ul rămâne mult sub conținut și panoul pare gol |
| fixture neutru, două scenarii, aceeași imagine | ownerul activ 1 devine 0 la deschiderea referinței; 82 de noduri deschise devin zero |
| după închiderea viewer-ului | panoul are circa 201 px, dar top-ul este circa −10.866 px; nu este pierdere de bytes PNG |
| boundary | sursa și oracolele Android nu participă la fixture; implementarea și regresia au owner în J0 |

Traseul suspect este [openReferencedScreenshot](../../atf-reporting/src/main/resources/atf/reporting/javascript/extent-evidence-disclosure.js): handlerul apelează original.click() pe ancora canonică. Aceasta poate declanșa și selecția ownerului sursă; remedierea trebuie să separe deschiderea imaginii de navigarea arborelui.

- [ ] Deschideți PNG-ul rezolvat fără a selecta execuția ancorei canonice; păstrați referința și deduplicarea, fără alterarea capturii.
- [ ] Păstrați test ID, scenario index, disclosure-urile și scroll-ul ownerului activ după open/close, Escape și revenirea din viewer.
- [ ] Verificați owner diferit și același owner, caption-uri identice, referință canonică ascunsă și ultimul checkpoint după scroll lung.
- [ ] Închideți numai cu regresii permanente sursă/JAR și audit Extent/fallback; contractele de test sunt în [Reporting Tests](ATF_Reporting_Tests_Feedback.md#report-test-screenshot-return-137--contextul-viewer-ului-pentru-png-reutilizat).

[Auditul J4](../../../atf_j4/docs/ATF_J4_Report_Audit.md) păstrează probele și separă problema rendererului de acceptorii consumerului. Fixture-ul temporar este J4RepeatedPngProbe, cu două scenarii și 40 de checkpoint-uri în al doilea; folosește numai API-urile Reporting și PNG sintetic 2×2.

## REPORT-SCREENSHOT-VIEWPORT-138 — imagine integrală în spațiul disponibil

**P1; cerință explicită de utilizare, nu pierdere demonstrată de bytes.** În viewer-ul J15, PNG-ul Android integral 1080×1920 este limitat la dialogul central; imaginea deschisă trebuie să folosească spațiul disponibil al browserului, cu întregul ecran vizibil.

- [ ] Folosiți un overlay la dimensiunea viewport-ului, cu controale compacte și imagine fit/contain în ambele axe; fără crop, deformare ori scroll interior în vederea inițială.
- [ ] Păstrați bytes originali; portrait într-un viewport landscape poate avea margini laterale. Zoom/original-size sunt moduri explicite, nu înlocuiesc vederea integrală inițială.
- [ ] Verificați portrait/landscape, resize și panou îngust, Enter/Space/Escape, focus și revenirea la același owner/scroll/disclosures. Acoperiți atât PNG direct cât și deduplicat, fără regresia [137](#report-screenshot-return-137--png-reutilizat-schimbă-ownerul-și-pierde-expandările).
- [ ] Păstrați aceeași acceptanță Extent/fallback, pe sursă și JAR fizic, cu fixture neutru; [Reporting Tests](ATF_Reporting_Tests_Feedback.md#report-test-screenshot-viewport-138--fit-integral-fără-crop) deține regresiile permanente.

Contractul țintă este în [auditul inline](../ATF_Inline_Audit_Contract.md#full-image-viewer-target). Captura întregului display și verificarea dimensiunilor sunt responsabilitatea [consumerului Android](../../../atf_j4/docs/ATF_Android_Report_Contract.md#full-display-android-screenshots); J0 nu capturează și nu interpretează ecranul dispozitivului.
