# Reporting J14 — proiecție UI fidelă și redacție comună

## Pe scurt

J0 păstrează sursa, nativele și harness-ul permanent. J1–J4 consumă aceeași
pereche thin JAR/POM direct în lib, lângă Core C3 nemodificat.

| Identitate | Valoare |
|---|---|
| Reporting | atf.api:atf-reporting:1.0.0-j14 |
| SHA-256 JAR | 981E99045D5F2F37FEC30E08341C4414541E79DA01E6DC237835E43A95DD9FCB |
| SHA-256 POM | C47D339C7CC2A89A2E6F0B06FD4FE6B782DE5AEDA873125F7F95EADCE285B2F9 |
| API J13 → J14 | 6.595 simboluri public/protected păstrate; zero adăugate sau eliminate |
| Build | Java 21; două staging-uri parentless noi, JAR și POM identice |
| Preview implicit | 240 de caractere |

## Cauză și responsabilități

- AtfUiActionRecord aplica trim valorilor expected/observed. Modelul păstrează
  acum payload-ul exact; numai operation și target sunt metadate normalizate.
- ACTION publica operation fără redacția folosită de OBSERVE. Titlul PNG
  putea păstra același canar în atributul accesibil.
- Fallback reconstruia recorduri redactate și aplica din nou altă politică.
  Acum deleagă recordurile originale și aceeași politică rendererelor comune.
- UiEvidenceRowMarkup deține celulele comune; ACTION păstrează coloana TIME.
  Identitatea, ordinalul, verdictul capturat și bytes PNG rămân neschimbate.
- Celula integrală păstrează LF, CRLF, CR, taburi și whitespace final în DOM.
  CR este codificat ca referință HTML, iar primul LF din pre este conservat.

## Regresii și oracole

| Probă | Rezultat |
|---|---|
| Reproducere model înainte de corecție | două eșecuri așteptate pentru whitespace pierdut |
| Reproducere proiecție înainte de corecție | patru eșecuri așteptate: canar, politică și newline |
| Native standalone | 89/89 în fiecare build |
| Harness sursă | 416/416, fără failures, errors sau skips |
| Harness JAR fizic | 417/417, inclusiv CodeSource Reporting J14 și Core C3 |
| Audit browser | sursă/JAR Extent/fallback: Enter/Space, text integral și vecin închis |

Fixture-ul neutru fixează valorile așteptate independent de markup. Scanează
întregul HTML pentru canarul de credential, inclusiv canale ascunse; verifică
policy custom ne-idempotent, ordinal, status și Base64 PNG exact. Probele browser
compară textContent integral și ultima valoare, fără normalizarea oracolului.

Regresia extinsă acoperă timing, indicatorul assertions, profilul explicit,
failure publication, screenshot policy și publicarea de volum. Bugetele assets
și profilele de heap existente rămân neschimbate.

## Distribuție și sanity

Rezultatele fiecărui consumer sunt consemnate separat după verificarea
candidatului. După promovare se recapturează live în TMP și se execută
sanity pe bibliotecile efectiv distribuite; rezultatul sursei nu substituie
acceptanța J1/J2/J3/J4.

Dovezi TMP: ATF_REPORTING_J14_ACTIVE_DO_NOT_CLEAN_9ptTeB — logs red/green, XML,
reporting-binary-api.json, inventarul raw al producătorului și promovare guarded.

## Criteriu rămas distinct

Valorile lungi ale assertions UI pot fi reduse de caller înainte de Reporting.
Această livrare conservă dovezile ACTION/OBSERVE și nu închide acel boundary.
SQL typed și navigarea exact-row au de asemenea criterii independente în J0.

## Reconcilierea livrării

- Guvernanța J0 a trecut 156/156. Controlul permanent J14 este în baseline;
  codul respectă limita de 240 de caractere pe linie.
- Primul staging J1/J2 a refuzat hash-ul metadata și proza densă din README.
  Metadata este corectată conform hash-ului JSON existent; README folosește tabel.
  Regulile și pragurile de guvernanță rămân neschimbate.
- Snapshot-ul sursei are 414 intrări: pom.xml, README.md și src/**, citite raw.
  Manifestul separă hash-ul JSON, digestul fișierelor sursă și digestul publicării
  cu singura transformare de versiune. Cele 17 fișiere native sunt declarate.

## Acceptanța candidatului înainte de distribuție

| Consumer | Cazuri verzi | Scope |
|---|---:|---|
| J1 | 187 | Shop 22, AutoEx 9 și guvernanță 156 |
| J2 | 186 | SQL embedded 7, Eventuate MOCK 9 și guvernanță 170 |
| J3 | 43 | guvernanță speech/Markdown; componentele reale sunt în proba extinsă |
| J4 | 2 | contracte Core/Reporting, fără device Android |

Toate au zero failures, errors și skips. Sanity-ul post-distribuție are
snapshot-uri și rezultate proprii; nu este dedus din această acceptanță.

## Sanity după distribuția live

Snapshot-urile au fost recapturate după livrarea guarded din 2 noiembrie.
Primele gate-uri post-distribuție trec separat: J1 187, J2 186, J3 43 și J4 2.
Selecția inițială J3 a executat numai guvernanța; acceptanța Reporting J3 este
demonstrată suplimentar de componentele și inferența nativă de mai jos.

| Consumer | Probe suplimentare executate | Verdict și limită |
|---|---|---|
| J1 | Shop single-flow și AutoEx final-screenshot, câte un E2E live; două clase componentă, 31 cazuri | 2 E2E PASS; rapoarte cu 135, respectiv 49 assertions |
| J2 | două componente MOCK în fallback; E2E approved-order explicit | componente 2/2; E2E ERROR înaintea mutației, bootstrap Kafka nerezolvabil |
| J3 | PcmFile 7, CanonicalPreparation 1 și SherpaFile 6, în fiecare renderer | 14/14 Extent și 14/14 fallback; inferență reală și referințe neschimbate |
| J4 | trei contracte Android; bid-grid și resume prin ADB | JUnit 3+2 PASS; acceptanța resume este REFUZATĂ de PNG/XML |

Nu există un verdict global E2E verde pentru cele patru proiecte. J2 necesită
laborator disponibil; lease-ul Android activ interzice alte probe ADB
concurente. Testele încheiate și artifactele originale sunt păstrate.

## Audit browser al consumerilor

- Cincisprezece HTML-uri inventariate și inspectate în TAB MCP: componente,
  E2E și failure, cu Extent/fallback acolo unde consumerul oferă selecția.
- Shop are un singur flow, 68 ACTION, 24 OBSERVE și 135 assertions. Ultimul
  ordinal ACTION este 89 deoarece sursele împart secvența; nu se renumerotează.
- AutoEx are 81 ACTION, trei OBSERVE, 49 assertions și o singură captură
  terminală, deschisă în viewer din grupul ACTION original.
- Enter/Space deschid și reînchid grupurile; End ajunge la ultima comparație,
  fără deschiderea vecinilor. Tabelele păstrează patru coloane și scroll interior.
- J2 fallback separă 0,372 s execution de publication. FAIL-ul LIVE păstrează
  KafkaException și Fails → Tests la aceeași fază; cauza ConfigException
  completă rămâne în JUnit și necesită diagnostic mai bogat la caller.
- J3 păstrează modelul ZIPFORMER2, versiunea 1.13.5 și oracolul WER original.
  Prepararea fără recognizer nu devine recunoaștere ori quality PASS.

## Lacune consumer J4 reproduse

| Boundary | Dovadă și responsabilitate |
|---|---|
| oracol resume | JUnit PASS, dar PNG și XML arată meniul; Resume Game este disabled. Pragul de pixeli luminoși nu dovedește cărți ori joc reluat. |
| assertions legacy | callerul trimite listă goală la cele două screenshot-uri; rendererul nu inventează comparații |
| timing legacy | 37,476 s în testcase; circa 209 ms în HTML măsoară construirea raportului |
| individual bid-grid | PNG/XML și ui-flow properties complete, fără publicare HTML individuală |
| concurență device | avizul de lease marchează dispozitivul comun; nu se deduce acceptanță izolată din PASS-ul JUnit |

Cele două PNG ale resume-ului sunt păstrate byte cu byte în HTML; integritatea
transportului nu validează propoziția business. Remedierea oracolului, auditului,
timing-ului și a închiderii publisherului aparține compoziției J4, coordonată în
[feedback-ul J4](../../atf_j4/docs/feedback/ATF_Android_Alignment_Feedback.md).
J0 nu adaugă logică Android și nu recalculează verdictul după screenshot.

Dovezi suplimentare: post-delivery-sanity.json, consumer-render-audit.json,
consumer-artifact-inventory.json și consumer-png-audit.json în staging-ul datat.
Rezolvarea online a pluginului build-helper declarat de J4 a fost făcută numai
în cache-ul TMP; primul refuz offline este păstrat, fără schimbarea POM-ului.
