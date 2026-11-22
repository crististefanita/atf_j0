# Feedback documentație — contracte deținute în sursă

## Pe scurt

J0 deține documentația bibliotecilor sursă și a regresiilor Reporting. [Criteriile comune ale consumerilor](../../../atf_j1/docs/feedback/ATF_Documentation_Feedback.md) rămân în J1; transferul unei constatări nu demonstrează implementarea.

<a id="joint-documentation---owner-j1"></a>
<a id="documentație-comună--owner-j1"></a>
<a id="doc-context-deployment-111--prioritate-81"></a>
## Ownership și contracte păstrate

| Limită | Owner și autoritate |
|---|---|
| proză și exemple ale bibliotecilor sursă | README-ul modulului, codul și testele native din J0 |
| stil comun și paritatea edițiilor | [standardul documentar](../README_Atf_DocumentationBestPractices.md#technical-prose-review), guvernanța documentației și comparatorul edițiilor fixate pe commit |
| scenarii ale consumerilor și criterii comune din afara bibliotecilor | J1; acceptanța independentă rămâne la fiecare consumer |

## DOC-REPORTING-PROSE — revizia rămasă a secțiunilor detaliate

**Status: Deschis.** Intrările principale sunt revizuite; secțiunile detaliate despre workerul browser și dovezi mai cer o revizie terminologică țintită.

- Revizuiește lifecycle-ul workerului din [Reporting Tests](../../atf-reporting-tests/README.md) și proza disclosure/evidence din [Reporting](../../atf-reporting/README.md); traducerile rămase includ „workshop” pentru worker și „discoloration” pentru disclosure.
- Compară fiecare pasaj cu ediția română, codul ownerului și oracolul independent; păstrează bugetele de timeout, mutanții, identificatorii și proveniența înregistrată.
- Închide numai după ce secțiunile au proză naturală, păstrează același contract în ambele ediții și trec guvernanța documentară și o revizie nouă a edițiilor fixată pe commit.
