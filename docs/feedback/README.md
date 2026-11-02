# ATF J0 — registrul lacunelor active

## Pe scurt

J0 deține implementările Core/Reporting, testele native Core și regresiile generice Reporting; J1 deține scenariile AutoEx/Shop. Contractele livrate sunt la [ownerii documentați](../ATF_Context_Atlas.md), iar aici rămân numai lacunele și traseele de închidere.

[Workspace-urile](../ATF_Workspace_Synchronization.md) separă J0 sursă, J1 consumer web și J2 WIP pentru Kafka/mesagerie cu Setup în Podman. Un rezultat sau o prioritate nu se transferă automat între ele.

## Top feedback-uri active

Scorurile sunt priorități, nu note de calitate sau procente de acoperire. Formula este impact /40 + lacună /30 + lucrabil /20 + datorie documentară /10; definiția și dovada rămân la owner.

| Ordine | Owner și criteriu | Prioritate /100 | Restanță concretă |
|---:|---|---:|---|
| 1 | [Publicare la volum mare](ATF_Android_UI_Reporting_Feedback.md#report-publication-memory-131--copii-integrale-la-publicare) | 94 | OOM J12 la 1 GB, stack în înlocuirea assets pe HTML integral; probă neutră sursă/JAR necesară |
| 2 | [ACTION/OBSERVE complet](ATF_Android_UI_Reporting_Feedback.md#report-action-full-evidence-132--expectedobserved-dincolo-de-preview) | 90 | preview 240 păstrat, dovadă integrală corelată și mutant cu ultimul scalar lipsă |
| 3 | [Reporting Tests](ATF_Reporting_Tests_Feedback.md) | 89 | permanent source/JAR oracles and negative fixtures |
| 4 | [SQL reporting](ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile) | 88 | neutral facts, configurable SQL budgets, J2 adoption after release |
| 5 | [Android handoff](ATF_Android_UI_Reporting_Feedback.md) | 88 | preserve the separate J4 adoption scope |
| 6 | [Public API contract](ATF_Reporting_Feedback.md#report-public-api-43--suprafață-susținută-și-ownership) | 81 | supported surface, explicit capabilities and legacy compatibility |

Masca literală, MIME, probele deadline-ului, corelarea comenzii, factura și lifecycle-urile deja livrate nu sunt relistate ca defecte. Rămân deschise numai negativele distincte descrise în feedback.

## Contracte și controale verificate

[Contractul Core C3](../ATF_Core_Consumer.md) fixează sursa și nativele în J0,
consumul prin JAR în J1/J2/J3 și acceptanțele distincte. Criteriile rezolvate ale
distribuției și probele deterministe de timeout sunt în documentația ownerului
și în baseline; nu rămân defecte active în feedback.

[Atlasul](../ATF_Context_Atlas.md), [arhitectura](../ATF_Architecture.md) și [ghidul de testare](../ATF_Testing_Guide.md) dețin contractele stabile și probele de pornire. Controalele tehnice sunt în [baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate), cu bazele reale ale proiectului.

[Disclosure-ul local și guvernanța Reporting J10](../ATF_Reporting_J10_Verification.md) sunt contracte verificate; criteriile 114 și 100 sunt eliminate din backlog.

[System și index fallback J11](../ATF_Reporting_J11_Verification.md) dețin contractele criteriilor 117 și 66; acestea nu mai sunt ToDo-uri. [Inspectorul, timing-ul și indicatorul J12](../ATF_Reporting_J12_Verification.md) au contracte și regresii permanente; handoff-ul Android rămâne distinct.

## Cerințe transferate în J1

[Registrul J1](../../../atf_j1/docs/feedback/README.md) deține Test Kit, guvernanța comună, designul AI și coordonarea consumerilor. ID-urile și probele sunt păstrate; transferul nu închide implementarea.

[Auditul datat 2026-11-01](../ATF_Producer_Audit_2026-11-01.md) este istoric de proiect, cu scorurile și limitele originale. Nu reprezintă starea curentă a criteriilor închise ulterior.

## Păstrare, promovare și verificare

| Situație | Acțiune și autoritate |
|---|---|
| contract deja implementat | păstrează regula la README-ul ownerului; indexul trimite la ea prin [atlas](../ATF_Context_Atlas.md) |
| lacună reală | trigger, observație, owner, prioritate și criteriu de închidere în feedback-ul domeniului |
| rezultat parțial | precizează limita și restanța; un guard verde nu închide alt scenariu |
| control Git verificat | [baseline](../ATF_Test_Changes_Baseline.md); nu se rebazează ori prelungește implicit |

[Protocolul documentar](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului) și [build-ul în TMP](../ATF_Build_And_Execution_Safety.md) guvernează fiecare increment. Criteriul se elimină numai după probă și promovarea contractului stabil.


## Increment consolidat — 2026-11-02

[Core C3](../ATF_Core_C3_Verification.md) consolidează console/cancellation cu native permanente, probă JAR și acceptanțe separate J1–J4. Reporting rămâne J12; adopția profilurilor J2 nu cere alt release al bibliotecii.

## Producer audit — 2026-11-01

[Auditul istoric de proiect](../ATF_Producer_Audit_2026-11-01.md) păstrează scorurile și probele datate. Criteriile curente se citesc la ownerii Core/Reporting de mai sus și în registrul J1 pentru celelalte module.
