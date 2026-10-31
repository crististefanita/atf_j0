# Documentația ATF Java

Acest director este indexul contractelor stabile ale ATF Java și al
designurilor de proiect marcate explicit ca propuneri.

- arhitectură și boundaries;
- adăugare de teste API;
- contractul de raportare Extent;
- build, staging și sync sigur;
- feedback activ și criterii de închidere.

Documentele descriu framework-ul și ownerii lui, nu un run sau un artifact
particular.

## Pe scurt

[Reporting J8 — validare API colapsabilă](ATF_Reporting_J8_Verification.md) păstrează dovada livrării și limitele ei.

| Pentru | Citește întâi |
|---|---|
| orientare între proiecte | [workspace-uri](ATF_Workspace_Synchronization.md), apoi [atlasul de context](ATF_Context_Atlas.md) |
| contract și owner | [arhitectură](ATF_Architecture.md), POM și README-ul modulului |
| problemă încă deschisă | [registrul feedback-urilor](feedback/README.md); contractele livrate rămân la owner |
| Kafka/Podman în J2 | [atlasul WIP](../../atf_j2/docs/ATF_Context_Atlas.md); nu este o extensie web a J1 |



J0 deține implementarea și regresiile Reporting; J1 deține scenariile AutoEx/Shop.
J2 a început bootstrap-ul pentru cozi și mesaje;
[harta workspace-urilor](ATF_Workspace_Synchronization.md) separă infrastructura copiată de contractele și acceptanța mesageriei.

| Nivel | Documente | Întrebarea la care răspund |
|---|---|---|
| workspace și livrare | [ATF_Workspace_Synchronization.md](ATF_Workspace_Synchronization.md) | unde se repară Reporting și cum se validează separat fiecare consumer? |
| context progresiv | [ATF_Context_Atlas.md](ATF_Context_Atlas.md) | ce contract, intrare și probă citesc înainte de a extinde contextul? |
| bibliotecă Reporting | [context portabil](../atf-reporting/docs/README.md) și [atlasul codului](../atf-reporting/src/main/java/atf/reporting/README.md) | ce intră în JAR, ce rămâne la consumer și ce probe disting adopția de release? |
| hartă | [ATF_Architecture.md](ATF_Architecture.md), README-ul root | cine deține ce și în ce direcție curg dependențele? |
| contract | [ATF_Reporting_Contract.md](ATF_Reporting_Contract.md), [ATF_Reporting_Display_Contract.md](ATF_Reporting_Display_Contract.md), ghidurile de modul | ce comportament semantic și ce formă vizibilă sunt stabile? |
| evidence transversal | [ATF_Evidence_Capture_Contract.md](ATF_Evidence_Capture_Contract.md) | cum se separă flow-ul, policy-ul, sursa și publisherul? |
| practică | [README_Atf_BestPractices.md](README_Atf_BestPractices.md) | cum adăugăm cod fără drift? |
| testare | [ATF_Testing_Guide.md](ATF_Testing_Guide.md) | ce layer, modul și proof aleg pentru un comportament? |
| infrastructură de test | [ATF_Test_Infrastructure_Annotations.md](ATF_Test_Infrastructure_Annotations.md) | cum declar nivelul, canalul, output-ul, setup-ul și documentația? |
| suport E2E transversal | README-urile [Test Kit](../atf-test-kit/README.md), [Practice Shop](../atf-test-practice-shop/README.md) și [Automation Exercise](../atf-test-auto-ex/README.md), împreună cu [arhitectura ATF](ATF_Architecture.md) | ce primitive neutre se reutilizează în Test Kit, iar ce harness UI rămâne la consumer? |
| design propus | [ATF_AI_Evaluation_Design.md](ATF_AI_Evaluation_Design.md) | ce boundary și contract țintă discutăm, fără a le declara livrate? |
| execuție | [ATF_Build_And_Execution_Safety.md](ATF_Build_And_Execution_Safety.md) | cum lucrăm fără să oprim run-uri? |
| feedback / guvernanță | [feedback/README.md](feedback/README.md), documentele active pe owner și [ATF_Test_Changes_Baseline.md](ATF_Test_Changes_Baseline.md) | ce gap observabil, regulă sau control tehnic de tranziție rămâne? |

Indexul păstrează harta; detaliile sunt în documentul ownerului și cresc în
specificitate pe măsură ce coborâm în pagină.

## Diagramă de bază

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":40,"padding":10,"diagramPadding":16}}}%%
flowchart LR
    Docs["Index"] -->|orientează| Architecture["Arhitectură"]
    Architecture -->|leagă| Guides["Ghiduri"]
    Guides -->|include| Safety["Build safety"]
    Safety -->|constrânge| Practices["Bune practici"]
    Practices -->|alimentează| Feedback["Feedback"]
```

<details>
<summary><strong>Legendă — cum citești harta documentației</strong></summary>

- săgeata solidă arată traseul de orientare în documentație, nu o dependență
  între module;
- `Architecture` fixează modulele, boundaries și direcția dependențelor;
- `Guides` și contractele descriu folosirea stabilă a framework-ului;
- `Safety` descrie staging-ul, sync-ul și gate-urile de execuție;
- `Practices` descrie regulile transversale, iar `Feedback` păstrează numai gap-uri
  reproductibile și ownership-ul lor.
Etichetele scurte păstrează următorul sens:

| În schemă | Sens complet |
|---|---|
| Index | docs index |
| Arhitectură | architecture |
| Ghiduri | guides + contracts |
| Build safety | build safety |
| Bune practici | best practices |
| Feedback | feedback + owner docs |
| orientează | orientează spre |

</details>

## Ordinea recomandată de lectură

| Scop | Documente de pornire |
|---|---|
| Orientare și ownership | [Arhitectură](ATF_Architecture.md) · [README root](../README.md) |
| Scriere și stratificarea testelor | [Ghid de testare](ATF_Testing_Guide.md) · [Ghid API](ATF_Api_Test_Guide.md) · [Bune practici de cod](README_Atf_CodeBestPractices.md) |
| Adnotări reutilizabile și tag-uri | [Contractul implementat](ATF_Test_Infrastructure_Annotations.md) · [TestKit](../atf-test-kit/README.md) |
| Reporting și evidence | [Contract reporting](ATF_Reporting_Contract.md) · [Contract display/layout](ATF_Reporting_Display_Contract.md) · [Contract evidence](ATF_Evidence_Capture_Contract.md) · [README reporting](../atf-reporting/README.md) |
| Consumatori și guvernanță | [Practice Shop](../atf-test-practice-shop/README.md) · [AutoEx](../atf-test-auto-ex/README.md) · [ATF Tests](../atf-tests/README.md) · [registrul feedback](feedback/README.md) |
| Design și implementare AI | [Design de referință](ATF_AI_Evaluation_Design.md) · [comparator implementat](../atf-validation/README.md#comparatorul-ai--contract-implementat) · [gap-uri active](feedback/README_AI_Evaluation.md) |
| Contribuție și operare | [Siguranța build-ului](ATF_Build_And_Execution_Safety.md) · [Bune practici ATF](README_Atf_BestPractices.md) · [Bune practici documentație](README_Atf_DocumentationBestPractices.md) |

Ordinea completă de lectură este păstrată colapsată mai jos:

<details>
<summary>Ordinea completă de lectură și toate documentele</summary>

1. [ATF_Architecture.md](ATF_Architecture.md) — module, dependențe și responsabilități.
2. [../atf-core/README.md](../atf-core/README.md) — kernelul neutru, porturile
   și modelele comune.
3. [../atf-mock/README.md](../atf-mock/README.md) — fake-ul determinist,
   catalogul YAML și transportul acceptance.
4. [../atf-test-kit/README.md](../atf-test-kit/README.md) — contractele testelor,
   nivelurile unit/component/integration/e2e și excepția Console `optional compile`.
5. [../atf-reporting-tests/README.md](../atf-reporting-tests/README.md) — fixture-uri
   API, composition root și lifecycle de test.
6. [../atf-validation/README.md](../atf-validation/README.md) — validarea
   independentă și portul de publicare.
7. [../atf-reporting/README.md](../atf-reporting/README.md) — rendererul,
   contractele pure ale librăriei și suita de compoziție component/E2E pentru raportare.
8. [../atf-test-practice-shop/README.md](../atf-test-practice-shop/README.md) —
    consumer Practice Shop, config, flow-uri și purchase E2E.
9. [../atf-test-auto-ex/README.md](../atf-test-auto-ex/README.md) — consumerul
    Automation Exercise, profilele purchase UI și contractele API opt-in.
10. [../atf-tests/README.md](../atf-tests/README.md) — guvernanța întregului reactor.
11. [../atf-console/README.md](../atf-console/README.md) — proiecția diagnostică
   de consolă și contractul de output.
12. [../atf-ui/README.md](../atf-ui/README.md) — politicile generice și adaptorul
   Playwright.
13. [../atf-reporting-tests/src/test/java/atf/ui/tests/extent/README.md](../atf-reporting-tests/src/test/java/atf/ui/tests/extent/README.md)
   — contractul stabil pentru selecția API, mock-backed expected projection,
   secțiuni Extent și validarea UI reutilizabilă.
14. [../atf-debug-ui/README.md](../atf-debug-ui/README.md) — publicarea
   read-only a unui HTML pentru investigație manuală în Playwright/TAB MCP.
15. [ATF_Api_Test_Guide.md](ATF_Api_Test_Guide.md) — pașii minimali pentru un test API nou.
16. [ATF_Evidence_Capture_Contract.md](ATF_Evidence_Capture_Contract.md) — contractul
    transversal pentru flow mode, screenshot policy, source și testare pe niveluri.
17. [ATF_Reporting_Contract.md](ATF_Reporting_Contract.md) — arborele Extent,
   evidence-ul, guardul de referință și regulile de randare.
18. [ATF_Reporting_Display_Contract.md](ATF_Reporting_Display_Contract.md) —
    ierarhia vizibilă, layout, disclosure-uri, viewport și dovada browser.
19. [ATF_Build_And_Execution_Safety.md](ATF_Build_And_Execution_Safety.md) —
   build, sincronizare și validare fără oprirea execuțiilor existente.
20. [README_Atf_BestPractices.md](README_Atf_BestPractices.md) — indexul regulilor;
   [README_Atf_CodeBestPractices.md](README_Atf_CodeBestPractices.md) pentru cod,
   OOP și testare; [README_Atf_DocumentationBestPractices.md](README_Atf_DocumentationBestPractices.md)
   pentru structură, diagrame și lizibilitate.
21. [ATF_Testing_Guide.md](ATF_Testing_Guide.md) — alegerea layer-ului, matricea
    module × layer și dovada potrivită pentru UI/API și raportare.
22. [feedback/README.md](feedback/README.md) — ToDo active, criterii de închidere
    și registrul de ownership pentru API, UI și reporting.
23. [ATF_Test_Changes_Baseline.md](ATF_Test_Changes_Baseline.md) — controale
    tehnice `VERIFIED` de tranziție, separate de cerințele funcționale.
24. [ATF_AI_Evaluation_Design.md](ATF_AI_Evaluation_Design.md) — modelele țintă și
    deciziile de boundary pentru AI evaluation/workflow; design propus, nu inventar de funcționalități livrate.
25. [ATF_Test_Infrastructure_Annotations.md](ATF_Test_Infrastructure_Annotations.md) —
    niveluri și tag-uri, canale, output project-relative, setup API și documentație; contract implementat și limitele probelor.

</details>

Harta modulelor are un singur owner: [ATF_Architecture.md](ATF_Architecture.md).
Acest index oferă doar trasee de lectură și trimiteri către contractele detaliate.

## Regula de lucru pentru orice schimbare

```text
cod și feedback live → staging separat → teste relevante → audit artifact/UI → promovare selectivă
```

Păstrează rulările existente; nu copia artefactele generate în surse.
Detaliile operaționale sunt în
[ATF_Build_And_Execution_Safety.md](ATF_Build_And_Execution_Safety.md).

| Stare | Locul normativ |
|---|---|
| Contract stabil | README-ul ownerului și testele sale |
| Design propus | [Designul AI](ATF_AI_Evaluation_Design.md), separat de implementare și de scorul din feedback |
| Gap încă deschis | [registrul de feedback](feedback/README.md) și documentul ownerului |
| Analiză închisă sau datată | În afara documentației normative |

Testele aplicației rămân la consumer. Astfel, `atf-reporting` poate fi folosit
ca JAR fără dependență de `atf-reporting-tests`.

### Regula de promovare

- **când:** un ToDo devine parte din soluție;
- **în README-ul ownerului:** contractul stabil, ownership-ul și modul de folosire;
- **în feedback:** limita rămasă, criteriul de închidere și dovada necesară;
- **interdicție:** feedback-ul nu este sursa normativă pentru o capabilitate deja
  livrată.

## Reguli de documentație

- README-urile descriu contracte, ownership și fluxuri reproductibile.
- Numele documentelor descriptive folosesc `ATF_<PascalCase>`; acronimul `ATF`
  apare o singură dată, în primul segment. README-ul unui modul rămâne
  `README.md`, iar indexul feedback-ului rămâne `docs/feedback/README.md`.
- README-urile normative și contractele de modul nu conțin scoruri, timestamp-uri,
  nume de rapoarte sau stări intermediare; scorurile de prioritizare sunt
  rezervate feedback-ului și nu exprimă calitatea contractului stabil.
- Nu se păstrează linkuri către artefacte HTML generate; se documentează doar
  tipul de output, locația configurabilă și criteriul de verificare.
- O diagramă trebuie să reflecte responsabilitățile și fluxurile existente;
  dacă acestea se schimbă, diagrama se actualizează împreună cu testele.
- Codul și raportul sunt sursa comportamentului; documentația nu poate inventa
  o capabilitate care nu este protejată de un test.
- Orice schimbare vizuală sau de navigare se verifică în artifactul HTML și,
  când este relevant, prin interacțiune în TAB MCP.

### Verificare obligatorie pentru raportul UI randat

Orice schimbare de clasă mare, CSS, markup, disclosure sau evidence urmează
aceeași probă, indiferent dacă raportul este produs dinamic sau auditat static:

```text
GENERATED_API: setup → API/mock → API report → ATF Debug/browser MCP
STATIC_*:      HTML existent → ATF Debug/browser MCP
                                 ↓
                         UI audit → UI report → ATF Debug/browser MCP
```

- **Browser:** DOM, accessibility tree, computed style, console/network și tab-uri;
- **UI evidence:** expand/collapse, acțiuni/observații/assertions ca zone-soră și screenshot inline;
- **API report:** POST/PUT cu request/response JSON valide, nevid și reconciliat cu `ValidationResult`;
- **Verdict:** cel puțin un caz pozitiv și unul negativ.

Build-ul se face în staging extern, sub `${ATF_STAGING_ROOT}`, nu în staging-ul
proiectului; raportul fresh se copiază în
`${ATF_REPORT_OUTPUT_DIR}` sau subdirectorul UI configurat numai după verificarea prin
ATF Debug UI și browser MCP. Browserul Windows real și oprirea rulărilor curente
sunt interzise.

## Vocabular scurt

- `test` = intenția de produs verificată
- `request` = schimbul HTTP și evidence-ul lui tehnic
- `API contract validation` = copilul request-ului evaluat, cu tabelul agregat de assertions
- `UI/API validation` = evidence semantică atașată checkpoint-ului Gherkin comun
- `phase` = SETUP, TEST BODY sau TEARDOWN; fazele SETUP și TEARDOWN sunt opționale
- `scenario` = testul complet, cu fazele și verdictul final
- `artifact` = raportul HTML autonom rezultat după finalizarea sesiunii


## Cum verifici indexul

| Întrebare | Autoritate și probă |
|---|---|
| Documentul există și fragmentul este navigabil? | [guard-ul README](../atf-tests/src/test/java/atf/api/tests/governance/documentation/DocumentationReadmeStructureGovernanceTest.java) și [regresiile ancorelor](../atf-tests/src/test/java/atf/api/tests/governance/documentation/DocumentationAnchorGovernanceTest.java) |
| Cine deține contractul? | [Arhitectura](ATF_Architecture.md) și POM-ul ownerului; linkul din index nu transferă responsabilitatea |
| Este regula livrată? | Codul și proba ownerului; [feedback-ul](feedback/README.md) separă restanța de contract |

Validitatea linkurilor nu certifică semantica unui contract. Comanda gate-ului
și limitele lui sunt la [ATF Tests](../atf-tests/README.md#verificare-rapidă).

## Core ca bibliotecă — owner și deployment

J0 păstrează sursa și testele native Core; reactorul sursă are în continuare 12 module.
[Contractul Core C1](ATF_Core_Consumer.md) separă sursa, publicarea și acceptanțele J1/J2.
Testele native se păstrează în J0, iar acceptanța pe binar rulează în TMP.
