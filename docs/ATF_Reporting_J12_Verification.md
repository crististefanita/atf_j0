# Reporting J12 — inspector, timing și indicator assertions

## Pe scurt

J0 deține implementarea, cele 83 de native și regresiile cu mock/browser. J1–J4 consumă aceeași pereche Reporting JAR/POM direct în lib. Core C2 și domeniile consumerilor sunt păstrate.

| Livrabil | Valoare |
|---|---|
| coordonată | atf.api:atf-reporting:1.0.0-j12 |
| SHA-256 JAR | 1F1B7B5502FEFEA02F500C2FC80494859D4E57E38261306B57FA7AD2D5AB38B1 |
| SHA-256 POM | F3F300B4E5F0625BE637373FBEAFB6B475AEFC68121D767D0EAD377742680028 |
| reproducibilitate | două publicații noi, bytes identici; 83/83 native fiecare |
| producer inputs | 409 fișiere; catalog nativ finit cu 16 fișiere Java, inclusiv helpers |
| compatibilitate | zero intrări public/protected eliminate față de J11; constructorul și accessorii record-ului legacy sunt păstrați |
| default assertions | 240; verdictul precede redacția și preview-ul |
| assets | 264.978 bytes native din 265.000; 222.393 publicare din 223.000; pragurile existente sunt păstrate |

## Comportamentul livrat

| Suprafață | Contract și probă permanentă |
|---|---|
| inspector explicit | callerul declară renderer, scenarii, faze și evidence; matrix Extent/fallback, individual/agregat, cu HTTP obligatoriu sau assertion/observation-only |
| rezultat public | gol, incomplet, contradictoriu ori cu diagnostice duplicate este neconform; ProfileInspection păstrează inventarul intenției |
| execuție și publicare | barieră după capturare, fără sleep; faza 11 ms rămâne execuție, transportul 7 ms rămâne distinct de publicare |
| assertions | indicator închis/deschis/reînchis, mouse/Enter/Space; mutantul ascunde numai glyph-ul, iar oracolul îl refuză |
| lizibilitate | sursele Execution/Transport/Publication sunt separate vizual de valoare; testul refuză spațierea absentă |

[Contractul inspectorului](../../atf_j0/atf-reporting/README.md#inspecția-artifactului-cu-intenție-explicită) și [probele permanente](../../atf_j0/atf-reporting-tests/README.md#probe-inspector-timing-și-indicator-assertions) dețin regulile stabile.

## Rezultate și reconciliere

- J11 fizic reproduce 15 FAIL din 22 de cazuri, cu zero ERROR/SKIP; noul candidat le repară.
- Regresia extinsă pe sursă are 570 de cazuri verzi. După rafinarea spațierii, selecția afectată are 86/86 verzi, inclusiv API, profil, timing, glyph și assets.
- Candidatul anterior fără spațiere este refuzat în 4/4 cazuri de timing; ultima corecție trece acceptanța separată pe JAR.
- Prima rulare largă a surprins politica viewport CSS mutată din poziția finală și hook-uri statice JS vechi; ordinea CSS a fost reparată, iar probele browser păstrează oracole independente.
- Logurile eșuate rămân în TMP, inclusiv descriptorul Maven absent, controls duplicate, digestul metadatelor și linkurile documentare corectate.
- Două gate-uri J0 care indicau copia J11 sunt excluse din acceptanță; guvernanța finală fixează explicit staging-ul J12.

| Gate curent | Rezultat |
|---|---|
| JAR fizic final | 136/136; CodeSource fizic și aceleași probe permanente |
| J0 guvernanță | 156/156 pe staging-ul J12 |
| J1 | două componente Shop/AutoEx; guvernanță 156/156 |
| J2 | API/SQL prin suite: 6/6 Extent și 6/6 fallback; guvernanță ÎN VERIFICARE |
| J3 | PCM 7/7 în fiecare renderer; 58/58 workspace/Markdown |
| J4 | 3/3 contracte Core/Reporting și PNG sintetic |

## Audit TAB și limite

Pe raportul real J2, TEST BODY 0,56 s, transportul 41 ms și assertions 8 ms au surse distincte.
Fallback păstrează cele șapte comparații și indicatorul, inclusiv ultimul rând și acces Enter.
Negativele neutre păstrează două FAIL-uri, comparația finală PASS și vecinii colapsați; Fails deschide ownerul original.

- J2 păstrează încă apelul legacy al inspectorului în trei acceptoare; overload-ul cu profil trebuie adoptat explicit, cu inventar independent de HTML.
- Aceasta este restanță locală, separată de API-ul J12.
- Profilul static nu substituie auditul browser sau evaluarea business.
- SQL semantic/bugete, geometria RESULT, auditul generic exact-row și contractul de release rămân deschise.
- Acceptanțele nu sunt E2E nou de broker, ASR, Android ori web real.

Dovezile rămân în TMP: ATF_REPORTING_J12_ACTIVE_DO_NOT_CLEAN_ofy19s — gate-uri, XML, publication-inputs.json, binary-api.json și browser-audit.json.
Promovarea este selectivă, prin guard Core; se verifică din nou live-ul și se păstrează schimbările locale.

Ultima normalizare a importurilor este verificată prin 15/15 cazuri pe sursă și 15/15 pe JAR, fără schimbarea binarului.
