# ATF J0 — registrul lacunelor active

## Pe scurt

J0 deține implementările Core/Reporting, testele native Core și regresiile generice Reporting; J1 deține scenariile AutoEx/Shop. Contractele livrate sunt la [ownerii documentați](../ATF_Context_Atlas.md), iar aici rămân numai lacunele și traseele de închidere.

[Workspace-urile](../ATF_Workspace_Synchronization.md) separă J0 sursă, J1 consumer web și J2 WIP pentru Kafka/mesagerie cu Setup în Podman. Un rezultat sau o prioritate nu se transferă automat între ele.

## Top feedback-uri active

Scorurile sunt priorități, nu note de calitate sau procente de acoperire. Formula este impact /40 + lacună /30 + lucrabil /20 + datorie documentară /10; definiția și dovada rămân la owner.

| Ordine | Owner și criteriu | Prioritate /100 | Restanță concretă |
|---:|---|---:|---|
| 1 | [Reporting Tests](ATF_Reporting_Tests_Feedback.md) | 89 | exact failure evidence, source/JAR negatives and distinct remaining geometry/SQL criteria |
| 2 | [SQL reporting](ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile) | 88 | neutral typed steps, independent SQL budgets and separate J2 adoption |
| 3 | [Long UI assertion evidence](ATF_Android_UI_Reporting_Feedback.md#report-ui-assertion-full-evidence-133--valori-lungi-capturate-înainte-de-preview) | 88 | capture before caller preview and safe full-value disclosure; ACTION/OBSERVE is already delivered |
| 4 | [Supported public API](ATF_Reporting_Feedback.md#report-public-api-43--suprafață-susținută-și-ownership) | 81 | supported surface, adapter capabilities and compatibility policy |
| 5 | [Recovery projection](ATF_Reporting_Feedback.md#ui-recovery-audit-01--încercări-inspectabile-în-extent) | 78 | typed attempt, cause, timing and budget projection |
| 6 | [Release contract](ATF_Reporting_Feedback.md#report-release-contract-45--ce-conține-și-ce-promite-pachetul) | 73 | automated declared-toolchain gate and external distribution metadata |

Masca literală, MIME, probele deadline-ului, corelarea comenzii, factura și lifecycle-urile deja livrate nu sunt relistate ca defecte. Rămân deschise numai negativele distincte descrise în feedback.

## Contracte și controale verificate

| Boundary | Delivered contract and retained proof |
|---|---|
| Core C3 | [Core consumer contract](../ATF_Core_Consumer.md): source/native tests in J0, physical JAR consumption and independent acceptances |
| Disclosure and governance | [J10](../ATF_Reporting_J10_Verification.md): criteria 114/100 are delivered |
| System, inspector and timing | [J11](../ATF_Reporting_J11_Verification.md) and [J12](../ATF_Reporting_J12_Verification.md): criteria 117/66/62/83/124 are delivered |
| Publication memory and full ACTION/OBSERVE | [J13](../ATF_Reporting_J13_Verification.md): criteria 131/132 are removed from the active ranking; bounded original source/JAR proof retained |
| Documentation and controls | [Atlas](../ATF_Context_Atlas.md), [architecture](../ATF_Architecture.md), [testing](../ATF_Testing_Guide.md) and [baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate) retain stable contracts and real Git bases |
| Latest consumer evidence | [J1 live purchases, 2026-11-03](../../../atf_j1/docs/ATF_J1_Live_Screenshot_Verification.md): four passing individual/filtered-suite invocations; residual generic J0 criteria remain separate |

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

[Core C3](../ATF_Core_C3_Verification.md) consolidează console/cancellation cu native permanente, probă JAR și acceptanțe separate J1–J4. The increment is historical; [J18](../ATF_Reporting_J18_Verification.md) owns the current Reporting delivery.

## Producer audit — 2026-11-01

[Auditul istoric de proiect](../ATF_Producer_Audit_2026-11-01.md) păstrează scorurile și probele datate. Criteriile curente se citesc la ownerii Core/Reporting de mai sus și în registrul J1 pentru celelalte module.
