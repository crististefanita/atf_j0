# Test Kit — biblioteca comună

## Pe scurt

J0 deține codul, API-ul, nativele, împachetarea și feedback-ul Test Kit. J1–J6 păstrează adaptoarele și oracolele domeniului și consumă o singură pereche JAR/POM direct în `lib`, cu ghid local și proveniență la rădăcină; guvernanța implementării rămâne în J0.

## Contractul păstrat

| Suprafață | Utilizare și limită |
|---|---|
| `@AtfUnitTest`, `@AtfComponentTest`, `@AtfIntegrationTest`, `@AtfE2eTest` | Clasifică testul; adnotarea de nivel nu publică singură un raport Extent. |
| `RecordedAssertions` | Evaluează prin JUnit; adaptorul injectează sanitizer-ul înainte de preview. Limita implicită este 240; valorile stocate nu reprezintă o arhivă integrală. |
| `ComponentAuditExtension`, `ComponentAuditAdapter` | Protocol neutru pentru faze, comparații și cleanup; consumerul furnizează fabrica și publicarea în Reporting. |
| Lifecycle și scope | Resurse per invocare, rollback și păstrarea cauzei; fără browser, broker, ADB ori ASR în Kit. |
| Configurație și resolvere | Politici typed pentru canal, evidence, consolă și output; configurația efectivă și selecția aparțin consumerului. |
| Resurse | `readResourceUtf8` citește și intrări JAR; `requiredPath` cere un fișier real. Nu confundați un resource URL cu o cale pe disc. |
| Ghiduri executabile | Utilitare opționale pentru compilarea exemplelor pe JAR/POM; cer JDK/Maven la verificare, nu pornesc procese la simpla utilizare a adnotărilor. |

Sesiunea comună și proiecția invocării sunt deja API Reporting: [AssertionAuditRun / AssertionAuditReport](../lib/README.Reporting.md#scope-și-integrare). Adaptorul mapează rezultatul Kit; implementarea Reporting rămâne în biblioteca sa.

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":40,"padding":10}}}%%
flowchart LR
    Owner["J0<br/>API · sursă · native"] -->|verifică| Pair["Candidat unic<br/>JAR + POM"]
    Pair -->|distribuie| Consumers["J1–J6<br/>adaptoare locale"]
    Consumers -.->|reproducere și feedback| Owner
```

<details>
<summary><strong>Legendă — aceeași bibliotecă, acceptanțe distincte</strong></summary>

- Muchiile descriu livrarea și mentenanța; nu introduc module noi sau dependențe către business.
- După adopție, sursa și nativele Kit rămân numai în J0. Consumerii verifică binarul fizic și propriile oracole.
- Reporting primește datele adaptorului; Kit nu depinde de renderer și nu capturează ecranul.

</details>

## Mentenanță și distribuție

1. Reconciliați diferențele live și păstrați API-ul comun. POM-ul publicat devine independent de parent, cu versiuni vendor explicite, JDK 21 și build reproductibil.
2. Verificați nativele și aceleași contracte pe JAR, fără surse Kit pe classpath; păstrați metadata runtime a adnotărilor, cauzele originale, scope-urile și limitele existente.
3. Declarați versiunea Kit separat de versiunea reactorului. Folosiți `test` pentru produs; adaptoarele din main ale modulelor de test pot declara `compile` justificat, fără propagare în aplicație. Console păstrează `optional compile`.
4. Adaptați bootstrap-ul Maven/IntelliJ, scripturile, politicile și guvernanța înainte de eliminarea modulului. Verificarea sursei aparține J0; consumerul verifică API, CodeSource/hash, inventar și lipsa copiilor paralele.
5. Distribuiți aceeași pereche verificată și păstrați istoricul provenienței. `lib` conține numai versiunea curentă a fiecărei biblioteci; ghidul Kit intră în inventarul exact împreună cu perechea sa.

Console păstrează dependența Kit opțională din publicația sa; consumerul declară explicit versiunea Kit. Compatibilitatea bridge-ului se verifică pe JAR. Schimbarea POM-ului Console cere o coordonată nouă, fără rescrierea publicației existente.

Runnerul păstrează motorul JUnit Jupiter în dependențele sale de test; Kit expune API-ul, fără să furnizeze automat engine-ul sau un lansator pentru aplicație.

## Acceptanța fiecărui consumer

| Consumer | Probă necesară și limită |
|---|---|
| J1 — web | Componentă Shop/AutoEx, individual și suită, lifecycle și aserțiuni colapsabile; capturarea rămâne la UI. |
| J2 — mesagerie | Componente SQL/Eventuate și profilul explicit Extent/fallback; fără module browser ori certificare implicită a brokerului. |
| J3 — speech | Componente de lifecycle/configurație și adaptorul de raportare cu fixture; fără modele, înregistrare sau acceptanță ASR presupusă. |
| J4 — Android | Comparații pe faze, cleanup și publisherul cu PNG furnizat; acceptanța fizică ADB rămâne distinctă. |
| J5 — Rentz | Componentele motorului și adaptorului, scriptul TMP și guvernanța proprie; fără introducerea modulelor Whist retrase. |
| J6 — Hold’em | Bootstrap Maven din lib și integrare offline în holdem-app; bibliotecile sunt test-scoped, iar cele cinci module de domeniu rămân independente. |

Un FAIL controlat rămâne FAIL cu aceeași cauză și aceleași comparații. Un test nedescoperit, un container eșuat sau o sursă paralelă nu califică migrarea. Nicio acceptanță nu se transferă între proiecte.

## Ghidul local al JAR-ului

Fiecare `lib/README.TestKit.md` este contractul local, suficient fără acces la J0: dependență și scope corecte, adnotări, un exemplu compilat, lifecycle, configurare și limitele preview-ului. Explicați explicit că publicarea Extent cere adaptorul consumerului. Exemplele se verifică pe perechea distribuită, fără surse paralele.

- `GuideBundle.publish` verifică explicit cele cinci biblioteci. KitLibraryGuideIT probează exemplul și controalele de compilare/bootstrap pe perechea selectată.
- Unitățile pure păstrează politica lor; migrarea nu le impune raportarea de componentă/E2E.

Footerul trimite mentenanța și constatările către [feedback-ul Kit din J0](feedback/ATF_Common_Test_Support_Feedback.md). Cerințele active, inclusiv Unicode și retenția înainte de preview, rămân acolo; acest contract nu le declară rezolvate.
