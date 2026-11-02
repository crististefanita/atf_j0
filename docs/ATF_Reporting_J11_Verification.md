# Reporting J11 — System și navigare fallback

## Pe scurt

J0 deține implementarea și toate probele Reporting. J1/J2/J3/J4 consumă
aceeași pereche JAR/POM direct în lib; Core C2 este păstrat.

| Livrabil | Valoare |
|---|---|
| coordonată | atf.api:atf-reporting:1.0.0-j11 |
| SHA-256 JAR | 20072F4DF0B9326476D7DAD3C889DCAF824BC1930B595AFBEE0E1C5253AD867F |
| reproducibilitate | două publicații curate, bytes identici, 70 native fiecare |
| default assertions | 240; comparația integrală și verdictul preced preview-ul |
| context | șase facts comune; Reporting indică rendererul efectiv |
| index | text neutru, badge separat, nume integrale, href/ID păstrate |

## Regresii și limite

- J10 fizic reproduce patru FAIL din șase nominale, fără ERROR/SKIP: metadata fallback și badge-ul indexului.
- Noile opt componente includ controale negative pentru un fact eliminat și badge aplicat întregului link.
- Navigarea compară fragmentul URI decodat; codificarea ID-ului nu schimbă identitatea scenariului.
- Suitele unit/component existente și consumatorul binar păstrează JSON, API assertions, disclosure și ABI legacy.
- Constructorii existenți rămân; updateEnvironment typed este o extensie fallback. API-ul public existent rămâne în candidat.

Consumerii au acceptanțe proprii: J1 Shop/AutoEx, J2 API/SQL embedded, J3 PCM
cu fixture și J4 contracte Reporting cu PNG sintetic. Aceste selecții nu sunt
un nou E2E Android, test de broker remote sau benchmark ASR.

## Criterii păstrate deschise

Profilul inspectorului 62, timing-ul 83, SQL 116, geometria RESULT 101 și
handoff-ul Android rămân distincte. Biblioteca nu inventează acțiuni/assertions
pe care un adaptor consumer nu le furnizează.

Probele sunt în TMP: ATF_REPORTING_J11_ACTIVE_DO_NOT_CLEAN_RRYvMT, logurile
gate-urilor, XML-urile, publication-inputs.json, binary-api.json și receipt-ul
auditului browser. [Protocolul de sincronizare](ATF_Workspace_Synchronization.md)
guvernează promovarea selectivă, hash-urile și păstrarea schimbărilor locale.

## Rezultatele incrementului J11

| Nivel | Rezultat verificat |
|---|---|
| Reporting nativ | 70/70 în fiecare dintre două publicații curate; JAR identic |
| Reporting pe sursă | 541 cazuri în inventarul XML final; zero FAIL/ERROR/SKIP nerezolvate |
| Reporting pe JAR fizic | 61/61; disclosure API/JSON/vecini, ABI legacy și noile componente |
| Focus tastatură | cele două viewport-uri rerulate pe sursă și JAR, 2/2 pe fiecare |
| J0/J1 guvernanță | 156/156 fiecare |
| J1 | Shop și AutoEx: câte o componentă, 2/2 |
| J2 | API/SQL prin suite: 6/6 Extent și 6/6 fallback; 170/170 guvernanță |
| J3 | PCM: 7/7 Extent și 7/7 fallback; 58/58 workspace și Markdown |
| J4 | 3/3 contracte Core/Reporting și publicare PNG sintetic; fără device |
| Compatibilitate | zero intrări public/protected eliminate față de J10; o metodă plus descriptor adăugate |

- Rularea largă inițială a păstrat două erori ale noului oracol URI; fallback codifică semnul egal, fără să schimbe identitatea scenariului.
- Comparația folosește fragmentul decodat; clasele afectate au fost rerulate, iar inventarul final nu are eșecuri nerezolvate.
- Logul și statutul inițiale rămân păstrate; contractul rendererului nu a fost relaxat.
- Auditul TAB pe JAR fizic verifică șase facts System, badge-uri separate și titluri integrale în fallback.
- Enter ajunge la scenariul FAIL; Space deschide comparația originală, iar Extent păstrează populația PASS/FAIL/SKIP.

Observația REPORT-FALLBACK-ASSERTION-AFFORDANCE-124 rămâne deschisă: disclosure-ul
funcționează, dar controlul de assertions nu are indicator vizual de stare.
PUBLIC-API-43 și ARTIFACT-PROFILE-62 păstrează și contraexemplele noi privind
rezultatele de inspecție vide ori contradictorii. Acestea nu sunt închise de J11.
