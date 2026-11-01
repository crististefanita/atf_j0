# Reporting J10 — disclosure local și compatibilitate

## Pe scurt

J0 deține sursa, nativele și regresiile Reporting. J1/J2/J3 consumă aceeași
pereche J10, alături de Core C2; nu primesc surse Reporting ori harness-ul.

| Livrabil | Contract |
|---|---|
| coordonată | atf.api:atf-reporting:1.0.0-j10 |
| JAR SHA-256 | CE527BA085FC216656B7E4DFBF11C71193FBACE2CEE17C2B40E2AEDECB0F0AC9 |
| publicare | două build-uri curate cu bytes identici; 67 native per build |
| Core | C2 și proveniența sa rămân neschimbate |
| lib | exact două perechi JAR/POM direct în folder, o versiune per bibliotecă |

## Independența expandărilor

| Control | Comportament |
|---|---|
| scenariu Extent | click, Enter și Space resetează numai descendenții scenariului acționat |
| vecini deschiși | păstrează fazele, request-urile, validările, JSON-ul și assertions vizibile |
| bulk | Expand all și Collapse all sunt explicite și limitate ownerului activ |
| fallback | disclosure-urile native rămân locale și operabile prin mouse/keyboard |
| preview | implicit 240; expected/actual și verdicturile originale rămân independente de scurtare |

Regresia neutră publică individual și agregat în ambele renderere. Cele 14
cazuri includ și controale care ascund corpul, păstrând atributele expanded
și valorile în DOM; oracolul verifică vizibilitatea efectivă și ultimul rând.

J9 fizic reproduce trei FAIL din 12 nominale, zero ERROR/SKIP, exact pentru
vecinii Extent și cele trei interacțiuni. Navigarea așteaptă DOMContentLoaded,
apoi readiness-ul și dovezile reale; un asset extern nu decide verdictul probei.

## Coeziune și API

| Owner | Structură verificată |
|---|---|
| taguri | ReportTags în pachet semantic; implementare unică pentru renderere și snapshot-uri |
| API legacy | ReportTagNormalizer rămâne public static, deprecated fără angajament de eliminare |
| model | opt contracte active plus puntea binară: nouă fișiere fizice, inventar exact revizuit |
| negative de coeziune | refuză orice a zecea clasă și substituirea unui contract la același număr fizic; pragul general rămâne opt |
| observation | cele două componente sunt în pachetul observation, cu imports normale; assertions sunt păstrate |
| binar | client compilat împotriva vechii semnături, apoi executat împotriva bibliotecii actuale; stub-ul rămâne exclus din classloader |

## Revalidare și limite

- Sursa și JAR-ul fizic au gate distinct pentru disclosure, JSON, observation și compatibilitate.
- Guvernanța completă se verifică separat în J0/J1/J2; J3 păstrează selecția sa speech și propriul guard documentar.
- Componentele MOCK/SQL ori speech fără model nu certifică un broker remote sau o nouă execuție ASR.
- Profilul inspectorului, timing-ul capturat/publicare, contrastul și lățimea fallback își păstrează criteriile separate.

Dovezi externe: incrementul TMP ATF_REPORTING_J10_ACTIVE_DO_NOT_CLEAN_267996e33f,
logurile gate-urilor, binary-api.json, manifestele publicației și verificarea
finală a livrării. [Sincronizarea](ATF_Workspace_Synchronization.md) guvernează
promovarea selectivă și păstrarea istoricului; output-urile nu intră în live.

## Rezultate executate pe candidat

| Gate | Invocări verzi |
|---|---:|
| Reporting sursă: unit și componente selectate | 533 |
| sursă finală: disclosure 14 și client ABI 1; disclosure se suprapune selecției anterioare | 15 |
| JAR: rerularea clasei afectate și a identității/ABI | 16 |
| guvernanță completă J0 / J1 / J2 | 156 / 156 / 168 |
| profil J3: speech component și guvernanță selectată | 80 |
| componente J1: un caz Shop și un caz AutoEx | 2 |
| J2: API individual / suită, fiecare în Extent și fallback | 1 / 3; 1 / 3 |
| J2: suită SQL H2, witness existent | 3 |

JAR-ul fizic acoperă 49 de cazuri distincte: tags native, client ABI vechi,
identitate binară, JSON și disclosure API/vecini. Prima execuție a avut un
timeout de navigare; clasa afectată trece integral la rerulare, cu buget
explicit 30 s pentru navigare și același buget 5 s pentru assertions.

Auditul TAB confirmă Extent și fallback agregat: închiderea vecinului
păstrează JSON-ul și tabelul original al subiectului, inclusiv rândul final.
Două build-uri parentless au 67 native fiecare și JAR-uri identice; comparația
J9/J10 păstrează toate cele 6.402 simboluri și adaugă trei intrări pentru ReportTags.
