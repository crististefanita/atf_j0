# ATF J0 — registrul lacunelor active

## Pe scurt

J0 deține implementările Core/Reporting/Console/Mock/Test Kit, toate testele native și regresiile generice ale bibliotecilor; J1 deține scenariile AutoEx/Shop. Contractele livrate sunt la [ownerii documentați](../ATF_Context_Atlas.md), iar aici rămân numai lacunele și traseele de închidere.

[Workspace-urile](../ATF_Workspace_Synchronization.md) separă J0 sursă, J1 consumer web și J2 WIP pentru Kafka/mesagerie cu Setup în Podman. Un rezultat sau o prioritate nu se transferă automat între ele.

## Biblioteci comune

J0 deține codul, contractele publice, testele native, împachetarea și feedback-ul Core, Reporting, Console, Mock și Test Kit, inclusiv constatările venite din consumerii ATF. [Ghidul de mentenanță](../../lib/README.md) definește o singură cerință per problemă și acceptanțe distincte la consumeri.

| Bibliotecă / probă | Backlog canonic |
|---|---|
| Core | [Core](ATF_Core_Feedback.md) |
| Reporting | [Reporting](ATF_Reporting_Feedback.md) |
| Regresii generice Reporting | [Reporting Tests](ATF_Reporting_Tests_Feedback.md) |
| Console și Mock | [Console / Mock](ATF_Console_Mock_Feedback.md) |
| Test Kit | [Test Kit](ATF_Common_Test_Support_Feedback.md) |


## Top feedback-uri active

Scorurile sunt priorități, nu note de calitate sau procente de acoperire. Formula este impact /40 + lacună /30 + lucrabil /20 + datorie documentară /10; definiția și dovada rămân la owner.

| Ordine | Owner și criteriu | Prioritate /100 | Restanță concretă |
|---:|---|---:|---|
| 1 | [Reporting unit baseline](ATF_Reporting_Tests_Feedback.md#report-unit-baseline-149--restore-the-complete-existing-unit-gate) | 93 | eight existing failures among 487 canonical unit cases; classify code/oracle causes and retain independent source/JAR negatives |
| 2 | [SQL reporting](ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile) | 88 | neutral typed steps, independent SQL budgets and separate J2 adoption |
| 3 | [Long UI assertion evidence](ATF_Android_UI_Reporting_Feedback.md#report-ui-assertion-full-evidence-133--valori-lungi-capturate-înainte-de-preview) | 88 | capture before caller preview and safe full-value disclosure; ACTION/OBSERVE is already delivered |
| 4 | [Supported public API](ATF_Reporting_Feedback.md#report-public-api-43--suprafață-susținută-și-ownership) | 81 | supported surface, adapter capabilities and compatibility policy |
| 5 | [Recovery projection](ATF_Reporting_Feedback.md#ui-recovery-audit-01--încercări-inspectabile-în-extent) | 78 | typed attempt, cause, timing and budget projection |
| 6 | [Release contract](ATF_Reporting_Feedback.md#report-release-contract-45--ce-conține-și-ce-promite-pachetul) | 73 | automated declared-toolchain gate and external distribution metadata |

Contractele de redactare literală, MIME, corelare și lifecycle își păstrează probele. Core verifică separat anularea HttpClient și URLConnection; acceptanța fiecărui consumer rămâne distinctă.

SQL typed steps are an explicitly requested expansion, separate from maintenance quality of existing features. Do not invent extensions or raise scores merely to reach a target; prioritize demonstrated correctness, auditability and ownership gaps first.

## Contracte și controale verificate

| Boundary | Delivered contract and retained proof |
|---|---|
| Ghiduri de consum | [Gate JAR/POM](../ATF_Platform_Libraries.md) |
| Core | [Core consumer contract](../ATF_Core_Consumer.md): source/native ownership, body/deadline cancellation and independent physical-JAR acceptance |
| Disclosure and governance | [display contract](../ATF_Reporting_Display_Contract.md): criteria 114/100 are delivered |
| System, inspector and timing | [display contract](../ATF_Reporting_Display_Contract.md) and [Reporting owner contract](../../atf-reporting/README.md): criteria 117/66/62/83/124 are delivered |
| Publication memory and full ACTION/OBSERVE | [Reporting owner contract](../../atf-reporting/README.md): criteria 131/132 are removed from the active ranking; bounded original source/JAR proof retained |
| Documentation and controls | [Atlas](../ATF_Context_Atlas.md), [architecture](../ATF_Architecture.md), [testing](../ATF_Testing_Guide.md) and [baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate) retain stable contracts and real Git bases |
| Latest consumer evidence | [J1 live purchases, 2026-11-03](../../../atf_j1/docs/ATF_J1_Live_Screenshot_Verification.md): four passing individual/filtered-suite invocations; residual generic J0 criteria remain separate |

## Cerințe transferate în J1

[Registrul J1](../../../atf_j1/docs/feedback/README.md) deține guvernanța comună, designul AI și coordonarea consumerilor. Test Kit are owner în J0 și este consumat ca JAR în J1–J5, fără surse paralele. ID-urile și probele sunt păstrate; transferul nu închide implementarea.

## Păstrare, promovare și verificare

| Situație | Acțiune și autoritate |
|---|---|
| contract deja implementat | păstrează regula la README-ul ownerului; indexul trimite la ea prin [atlas](../ATF_Context_Atlas.md) |
| lacună reală | trigger, observație, owner, prioritate și criteriu de închidere în feedback-ul domeniului |
| rezultat parțial | precizează limita și restanța; un guard verde nu închide alt scenariu |
| control Git verificat | [baseline](../ATF_Test_Changes_Baseline.md); nu se rebazează ori prelungește implicit |

[Protocolul documentar](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului) și [build-ul în TMP](../ATF_Build_And_Execution_Safety.md) guvernează fiecare increment. Criteriul se elimină numai după probă și promovarea contractului stabil.
