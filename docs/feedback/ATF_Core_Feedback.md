# Core — criterii active și contracte consolidate

## Pe scurt

Nu rămân cerințe Core active din auditul console/cancellation.
[Core C3](../ATF_Core_C3_Verification.md) păstrează dovada sursă/JAR și distribuția distinctă către fiecare consumer.

<a id="core-console-redaction-124--sanitize-every-emitted-console-channel"></a>

## Proiecția console — contract consolidat

[Politica Core](../../atf-core/README.md#proiecția-console-și-anularea-transportului)
redactează numele START/COMPLETE și string-urile JSON publice cu sintaxă cunoscută.
Inputurile originale, numerele exacte, duplicatele și expected-ul independent rămân intacte.

<a id="core-transport-cancel-125--retain-ownership-of-the-upstream-http-future"></a>

## Anulare upstream — contract consolidat

Future-ul expus transmite best effort anularea către operația preluată, cu interrupt flag păstrat.
Cursele, finalizările târzii și cleanup-ul eșuat au probe native permanente; eroarea cleanup rămâne suppressed.

Anularea nu dovedește rollback server-side și nu autorizează replay al unei mutații incerte.
GET-ul local este probă HTTP distinctă de acceptanța aplicației Eventuate.

## Owneri și restanțe distincte

- [Reporting](ATF_Reporting_Feedback.md) păstrează criteriile generice de bibliotecă și release.
- [Regresiile Reporting](ATF_Reporting_Tests_Feedback.md) păstrează oracolele și probele neutre.
- [Registrul J1](../../../atf_j1/docs/feedback/README.md) deține criteriile comune non-Core/Reporting; consumerii păstrează acceptanțele domeniilor lor.
- [Baseline-ul tehnic](../ATF_Test_Changes_Baseline.md) reține controalele VERIFIED, la bazele lor reale, separat de backlog.
