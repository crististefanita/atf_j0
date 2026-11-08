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


## Priorități de mentenanță activă

Scorurile sunt priorități, nu note de calitate sau procente de acoperire. Formula este impact /40 + lacună /30 + lucrabil /20 + datorie documentară /10; definiția și dovada rămân la owner.

| Ordine | Owner și criteriu | Prioritate /100 | Restanță concretă |
|---:|---|---:|---|
| 1 | [Valori UI integrale](ATF_Android_UI_Reporting_Feedback.md#report-ui-assertion-full-evidence-133--valori-lungi-capturate-înainte-de-preview) | 88 | captură înaintea preview-ului callerului și disclosure sigur; ACTION/OBSERVE este deja livrat |
| 2 | [API public susținut](ATF_Reporting_Feedback.md#report-public-api-43--suprafață-susținută-și-ownership) | 81 | delimitarea suprafeței, capabilităților adaptoarelor și compatibilității |
| 3 | [Oracolul testelor Reporting](ATF_Reporting_Tests_Feedback.md#report-test-audit-60--o-dovadă-identificabilă-pentru-fiecare-scenariu) | 80 | identificarea exactă a dovezii și matricea negativă rămasă |
| 4 | [Proiecția recovery](ATF_Reporting_Feedback.md#ui-recovery-audit-01--încercări-inspectabile-în-extent) | 78 | proiecția typed a încercării, cauzei, timing-ului și bugetului |
| 5 | [Contractul release-ului](ATF_Reporting_Feedback.md#report-release-contract-45--ce-conține-și-ce-promite-pachetul) | 73 | gate automat cu toolchain declarată și metadate externe de distribuție |

Extinderea SQL solicitată, [REPORT-SQL-STEPS-116](ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile), rămâne separată de nota de mentenanță a comportamentului livrat.

Contractele de redactare literală, MIME, corelare și lifecycle își păstrează probele. Core verifică separat anularea HttpClient și URLConnection; acceptanța fiecărui consumer rămâne distinctă.

Nu introduceți funcționalitate doar pentru a atinge o notă; prioritizați corectitudinea demonstrată, auditabilitatea și ownership-ul.

## Contracte și controale verificate

| Boundary | Delivered contract and retained proof |
|---|---|
| Ghiduri de consum | [Gate JAR/POM](../ATF_Platform_Libraries.md) |
| Core | [Contractul consumerului](../ATF_Core_Consumer.md): ownership sursă/native, anularea body/deadline și acceptanța independentă pe JAR |
| Disclosure și guvernanță | [Contractul de afișare](../ATF_Reporting_Display_Contract.md): criteriile 114/100 sunt livrate |
| System, inspector și timing | [Contractul de afișare](../ATF_Reporting_Display_Contract.md) și [contractul Reporting](../../atf-reporting/README.md): criteriile 117/66/62/83/124 sunt livrate |
| Memoria publicării și ACTION/OBSERVE integral | [Contractul Reporting](../../atf-reporting/README.md): criteriile 131/132 au ieșit din prioritățile active; proba sursă/JAR rămâne |
| Documentație și controale | [Atlas](../ATF_Context_Atlas.md), [arhitectură](../ATF_Architecture.md), [testare](../ATF_Testing_Guide.md) și [baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate) păstrează contractele și bazele Git reale |

## Cerințe transferate în J1

[Registrul J1](../../../atf_j1/docs/feedback/README.md) deține guvernanța comună, designul AI și coordonarea consumerilor. Test Kit are owner în J0 și este consumat ca JAR în J1–J6, fără surse paralele. ID-urile și probele sunt păstrate; transferul nu închide implementarea.

## Păstrare, promovare și verificare

| Situație | Acțiune și autoritate |
|---|---|
| contract deja implementat | păstrează regula la README-ul ownerului; indexul trimite la ea prin [atlas](../ATF_Context_Atlas.md) |
| lacună reală | trigger, observație, owner, prioritate și criteriu de închidere în feedback-ul domeniului |
| rezultat parțial | precizează limita și restanța; un guard verde nu închide alt scenariu |
| control Git verificat | [baseline](../ATF_Test_Changes_Baseline.md); nu se rebazează ori prelungește implicit |

[Protocolul documentar](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului) și [build-ul în TMP](../ATF_Build_And_Execution_Safety.md) guvernează fiecare increment. Criteriul se elimină numai după probă și promovarea contractului stabil.
