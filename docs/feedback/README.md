<a id="atf-j0---register-of-active-loopholes"></a>
<a id="atf-j0--registrul-lacunelor-active"></a>
<a id="atf-j0--active-gap-registry"></a>
# ATF J0 — registrul lacunelor active

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## Pe scurt

Registrul J0 urmărește lacunele Core, Reporting, Console, Mock, Test Kit și Debug UI, inclusiv regresiile generice Reporting. [Atlasul](../ATF_Context_Atlas.md) indică ownerii contractelor livrate, iar [registrul J1](https://github.com/crististefanita/atf_j1/blob/main_RO/docs/feedback/README.md) urmărește cerințele comune din afara bibliotecilor și scenariile AutoEx/Shop.

[Workspace-urile](../ATF_Workspace_Synchronization.md) separă J0 sursă, J1 consumer web și J2 WIP pentru Kafka/mesagerie cu Setup în Podman. Un rezultat sau o prioritate nu se transferă automat între ele.

<a id="common-libraries"></a>
<a id="biblioteci-comune"></a>
## Biblioteci comune

J0 deține codul, contractele publice, testele native, împachetarea și feedback-ul Core, Reporting, Console, Mock, Test Kit și Debug UI, inclusiv constatările venite din consumerii ATF. [Ghidul de mentenanță](../../lib/README.md) definește o singură cerință per problemă și acceptanțe distincte la consumeri.

| Bibliotecă / probă | Backlog canonic |
|---|---|
| Core | [Core](ATF_Core_Feedback.md) |
| Reporting | [Reporting](ATF_Reporting_Feedback.md) |
| Regresii generice Reporting | [Reporting Tests](ATF_Reporting_Tests_Feedback.md) |
| Console și Mock | [Console / Mock](ATF_Console_Mock_Feedback.md) |
| Test Kit | [Test Kit](ATF_Common_Test_Support_Feedback.md) |
| Debug UI | [Contractul Debug UI și ownership-ul feedback-ului](../ATF_Platform_Libraries.md#debug-ui-ownership-and-acceptance) |

<a id="maintenance-governance"></a>
<a id="guvernanța-mentenanței"></a>
## Guvernanța mentenanței

[Feedback-ul workspace-urilor](ATF_Maintenance_Feedback.md) păstrează ownership-ul porților whole-reactor și J0/J1. Entry point-ul combinat, verificările read-only și criteriul de închidere sunt executabile și legate de documentația procesului.

Criteriile editoriale ale bibliotecilor sursă și adopția regulii pentru limba sursei dincolo de J1 sunt urmărite în [feedback-ul documentației](ATF_Documentation_Feedback.md); rollout-ul rămâne deschis până când fiecare owner aplicabil adaugă și verifică un gate conștient de limbaj. Criteriile comune ale consumerilor rămân în J1.

<a id="active-maintenance-priorities"></a>
<a id="priorități-de-mentenanță-activă"></a>
## Priorități de mentenanță activă

Scorurile sunt priorități, nu note de calitate sau procente de acoperire. Formula este impact /40 + lacună /30 + lucrabil /20 + datorie documentară /10; definiția și dovada rămân la owner.

| Ordine | Owner și criteriu | Prioritate /100 | Restanță concretă |
|---:|---|---:|---|
| 1 | [Valori UI integrale](ATF_Android_UI_Reporting_Feedback.md#report-ui-assertion-full-evidence-133--valori-lungi-capturate-înainte-de-preview) | 88 | captură înaintea preview-ului callerului și disclosure sigur; ACTION/OBSERVE este deja livrat |
| 2 | [API public susținut](ATF_Reporting_Feedback.md#report-public-api-43--suprafață-susținută-și-ownership) | 81 | delimitarea suprafeței, capabilităților adaptoarelor și compatibilității |
| 3 | [Oracolul testelor Reporting](ATF_Reporting_Tests_Feedback.md#report-test-audit-60--o-dovadă-identificabilă-pentru-fiecare-scenariu) | 80 | identificarea exactă a dovezii, selecția ownerului în proba externă și matricea negativă rămasă |
| 4 | [Contractul release-ului](ATF_Reporting_Feedback.md#report-release-contract-45--ce-conține-și-ce-promite-pachetul) | 73 | gate automat cu toolchain declarată și metadate externe de distribuție |
| 5 | [Profilul pentru observation UI](ATF_Reporting_Feedback.md#report-observation-contract--domeniul-validatorului-de-artefact) | 72 | intenția PNG și asocierea ei la acțiunea/ownerul declarat |

Extinderea SQL solicitată, [REPORT-SQL-STEPS-116](ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile), rămâne separată de nota de mentenanță a comportamentului livrat.

[Redactarea etichetelor de aserțiuni](Whist_Assertion_Redaction_Feedback.md) urmărește cazul punctual în care textul de business cu `basic` pierde un cuvânt. Proba de închidere păstrează și mascarea credențialelor Basic reale.

Contractele de redactare literală, MIME, corelare și lifecycle își păstrează probele. Core verifică separat anularea HttpClient și URLConnection; acceptanța fiecărui consumer rămâne distinctă.

Nu introduceți funcționalitate doar pentru a atinge o notă; prioritizați corectitudinea demonstrată, auditabilitatea și ownership-ul.

<a id="checked-contracts-and-controls"></a>
<a id="contracte-și-controale-verificate"></a>
## Ownerii contractelor

[Atlasul](../ATF_Context_Atlas.md) trimite contractele reutilizabile la ownerii normativi; [controalele schimbărilor de teste](../ATF_Test_Changes_Baseline.md) păstrează bazele Git imuabile. Dovezile livrării către consumeri și criteriile încheiate nu sunt duplicate în acest backlog.

<a id="requirements-transferred-to-j1"></a>
<a id="cerințe-transferate-în-j1"></a>
## Cerințe transferate în J1

[Registrul J1](https://github.com/crististefanita/atf_j1/blob/main_RO/docs/feedback/README.md) deține guvernanța comună, designul AI și coordonarea consumerilor. Test Kit și Debug UI au owner în J0; J1 este primul consumer Debug UI, fără surse paralele. ID-urile și probele sunt păstrate; transferul nu închide implementarea.

<a id="preservation-promotion-and-verification"></a>
<a id="păstrare-promovare-și-verificare"></a>
## Păstrare, promovare și verificare

| Situație | Acțiune și autoritate |
|---|---|
| contract deja implementat | păstrează regula la README-ul ownerului; indexul trimite la ea prin [atlas](../ATF_Context_Atlas.md) |
| lacună reală | trigger, observație, owner, prioritate și criteriu de închidere în feedback-ul domeniului |
| rezultat parțial | precizează limita și restanța; un guard verde nu închide alt scenariu |
| control Git verificat | [baseline](../ATF_Test_Changes_Baseline.md); nu se rebazează ori prelungește implicit |

[Protocolul documentar](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului) și [build-ul în TMP](../ATF_Build_And_Execution_Safety.md) guvernează fiecare increment. Criteriul se elimină numai după probă și promovarea contractului stabil.
