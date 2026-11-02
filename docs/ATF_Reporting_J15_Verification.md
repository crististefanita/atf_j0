# Reporting J15 — dovezi inline și fidelitatea textului

## Rezultatul incrementului

J15 repară păstrarea TEXT în PRE: newline inițial, CR/LF și whitespace final.

- Redacția și limitarea preced escaping-ul comun; preview-ul rămâne 240.
- Profilul fără fișiere externe folosește API-ul existent, cu artifacts gol.
- Core C3 este neschimbat; interpretarea Android rămâne la consumer.

| Livrabil | Valoare verificată |
|---|---|
| coordinate | atf.api:atf-reporting:1.0.0-j15 |
| JAR SHA-256 | 7B3C9DF7D22B4DD50EEFEB1E72358B4BAEC35CC3B24AC13D531BD1A101D3ADF0 |
| POM SHA-256 | 4643B3E3A705B2F5928BA9AC5F0956B6ECFE61E27D1CAB691AA8B6153814B44A |
| producător | J0, baza e077ed643f2167b56e711a0acdf9ad6619833a0d plus incrementul revizuit |
| intrări | 414 fișiere brute; 17 fișiere Java native; catalogul complet în proveniență |
| publicare | două build-uri parentless independente, JAR identic; numai versiunea proiectului este transformată |
| compatibilitate J14 | 6.595 intrări public/protected păstrate; două intrări noi pentru helperul PRE, zero eliminate |

## Probe și limite

| Gate | Rezultat și scope |
|---|---|
| native parentless | 91 PASS per build, zero failures/errors/skips |
| sursă | 57 invocări harness distincte verzi, plus cele cinci native observation în selecția inițială |
| JAR fizic | 58 PASS: aceleași 57 plus CodeSource pentru Reporting J15 și Core C3 |
| matrice nouă | opt combinații individual/combinat × component/UI-shaped × Extent/fallback; patru controale negative și patru browser |
| fidelitate | JSON exact, întreg peste precizia JS, redacție, TEXT CR/LF integral, verdict, timing și PNG per owner |
| mutant | body la sibling, ultimul rând eliminat, FAIL→PASS, timing fals, link extern și PNG reasociat sunt refuzate |
| autonomie | browserul deschide doar copia HTML; HTTP/HTTPS blocate, fără XML/JSON/properties auxiliare |
| randare | Enter/Space, End la ultima linie, vecin închis, redimensionare și comparație geometrică în aceeași citire DOM |

Reproducerea inițială are patru eșecuri din opt pentru primul LF consumat.

- Primul gate extins a semnalat citirea geometrică neatomică din testul nou și un timeout End de 5 s în regresia UI adiacentă; dovezile rămân în TMP.
- Testul nou citește ambele dreptunghiuri în aceeași stare DOM; mutanții editează segmentele brute, fără reserializare care ar altera alte newline-uri.
- Selecțiile afectate au fost rerulate verzi; pragul de 5 s nu este mărit, iar gate-ul inițial eșuat nu este relabelat ca reușit.

## Auditul J4

| Suprafață | Observație și boundary |
|---|---|
| componentă arhivată | publisher rerulat în TMP pe dovezi închise: 187 cazuri JUnit sunt observations în două scenarii globale |
| referințe | 189 linkuri file în ownerii originali din Tests; zero tabele standard assertions |
| E2E resume arhivat | două PNG inline se deschid corect; zero comparisons; 209 ms descriu compoziția, nu execuția de 37,476 s |
| verdict resume | PNG arată meniul și Resume Game disabled; JUnit PASS nu certifică reluarea |
| AI înghețat | metadata C3/J13 rămâne istorică; nu este rescrisă ca acceptanță J15 |

Auditul TAB MCP verifică dovezile publicate, fără acces ADB sau intervenție asupra lease-ului activ.

- Au fost inspectate componenta J4 recompusă, PNG-ul E2E în viewer, componenta neutră pe sursă și Extent/fallback pe JAR.
- FAIL-ul controlat 41→42 navighează din Fails la rândul original; Overview păstrează 1 PASS/1 FAIL.
- Textul fallback ajunge prin End la FINAL-TEXT-0, cu vecinul JSON închis.

## Acceptanță distinctă a consumerilor

| Consumer | Sanity pe candidat J15 | Limită |
|---|---|---|
| J1 | 31 PASS: Shop configuration 22 și AutoEx configuration 9 | fără rerulare journeys live în acest increment |
| J2 | 11 PASS: SQL și cele două suite Eventuate MOCK/embedded | brokerul real rămâne separat, conform feedback-ului local |
| J3 | 8 PASS: PCM 7 și canonical preparation 1 | fără acceptanță de captură audio live |
| J4 | 5 PASS: Core/Reporting 2, PNG adapter 2, inline renderer 1 | fără acceptanță pe device; oracolul resume rămâne deschis |

| Verificare finală | Dovadă |
|---|---|
| guvernanță | J0 156, J1 156, J2 170 și J3 43 invocări verzi; documentele și controalele POM au fost reconciliate |
| după distribuire | copii noi ale live-ului: J1 31, J2 11, J3 8, J4 5 PASS; zero failures/errors/skips |
| identitate | J15 fizic în classpath Surefire; același hash în live, snapshot și cache Maven; Core C3 identic |
| layout | exact patru fișiere în fiecare lib: o pereche Core și o pereche Reporting |
| randare sanity | Shop 22 grupuri assertions, SQL 5 și PCM 7; contractul PNG J4 are o imagine și nu pretinde comparații business |

Rezultatele runtime nu moștenesc automat acceptanța semantică a aplicațiilor.
Dovezile TMP sunt localizate prin evidenceRoot în library-provenance.json;
post-delivery-sanity.json păstrează numărătorile și identitatea per consumer.

[Contractul inline](ATF_Inline_Audit_Contract.md) separă compoziția și randarea.
