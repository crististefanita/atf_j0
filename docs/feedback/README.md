# ATF J0 — registrul lacunelor active

## Pe scurt

J0 deține implementările Core/Reporting, testele native Core și regresiile generice Reporting; J1 deține scenariile AutoEx/Shop. Contractele livrate sunt la [ownerii documentați](../ATF_Context_Atlas.md), iar aici rămân numai lacunele și traseele de închidere.

[Workspace-urile](../ATF_Workspace_Synchronization.md) separă J0 sursă, J1 consumer web și J2 WIP pentru Kafka/mesagerie cu Setup în Podman. Un rezultat sau o prioritate nu se transferă automat între ele.

## Top feedback-uri active

Scorurile sunt priorități, nu note de calitate sau procente de acoperire. Formula este impact /40 + lacună /30 + lucrabil /20 + datorie documentară /10; definiția și dovada rămân la owner.

| Ordine | Owner și criteriu | Prioritate /100 | Restanță concretă |
|---:|---|---:|---|
| 1 | [Reporting](ATF_Reporting_Feedback.md) | 89 | profilul inspectorului; timing capturat/publicare 88 rămâne distinct |
| 2 | [Reporting Tests](ATF_Reporting_Tests_Feedback.md) | 89 | matricea assertion-only/HTTP și guvernanța preexistentă 87 |
| 3 | [Expandarea vecinilor](ATF_Reporting_Feedback.md#report-sibling-disclosure-114--păstrează-expandarea-vecinilor) | 82 | închiderea unui scenariu resetează fazele altor scenarii în Extent |
| 4 | [Documentație](ATF_Documentation_Feedback.md) | 81 | mutanți semantici pentru atlas, cu istoricul legitim păstrat |

Masca literală, MIME, probele deadline-ului, corelarea comenzii, factura și lifecycle-urile deja livrate nu sunt relistate ca defecte. Rămân deschise numai negativele distincte descrise în feedback.

## Contracte și controale verificate

[Contractul Core C2](../ATF_Core_Consumer.md) fixează sursa și nativele în J0,
consumul prin JAR în J1/J2/J3 și acceptanțele distincte. Criteriile rezolvate ale
distribuției și probele deterministe de timeout sunt în documentația ownerului
și în baseline; nu rămân defecte active în feedback.

[Atlasul](../ATF_Context_Atlas.md), [arhitectura](../ATF_Architecture.md) și [ghidul de testare](../ATF_Testing_Guide.md) dețin contractele stabile și probele de pornire. Controalele tehnice sunt în [baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate), cu bazele reale ale proiectului.

## Design AI — evaluare separată

[Evaluarea AI](README_AI_Evaluation.md) separă comparatorul existent de designul agentic propus. Backlog-ul de design nu extinde incrementul de mentenanță, iar [nota editorială](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale) nu certifică readiness-ul implementării.

## Păstrare, promovare și verificare

| Situație | Acțiune și autoritate |
|---|---|
| contract deja implementat | păstrează regula la README-ul ownerului; indexul trimite la ea prin [atlas](../ATF_Context_Atlas.md) |
| lacună reală | trigger, observație, owner, prioritate și criteriu de închidere în feedback-ul domeniului |
| rezultat parțial | precizează limita și restanța; un guard verde nu închide alt scenariu |
| control Git verificat | [baseline](../ATF_Test_Changes_Baseline.md); nu se rebazează ori prelungește implicit |

[Protocolul documentar](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului) și [build-ul în TMP](../ATF_Build_And_Execution_Safety.md) guvernează fiecare increment. Criteriul se elimină numai după probă și promovarea contractului stabil.
