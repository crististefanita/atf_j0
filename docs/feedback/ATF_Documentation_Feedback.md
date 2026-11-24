# Feedback documentație — contracte deținute în sursă

## Pe scurt

J0 deține documentația bibliotecilor sursă și a regresiilor Reporting. [Criteriile comune ale consumerilor](https://github.com/crististefanita/atf_j1/blob/HEAD/docs/feedback/ATF_Documentation_Feedback.md) rămân în J1; transferul unei constatări nu demonstrează implementarea.

<a id="doc-reporting-prose--remaining-detailed-section-review"></a>
<a id="doc-reporting-prose--revizia-rămasă-a-secțiunilor-detaliate"></a>
<a id="joint-documentation---owner-j1"></a>
<a id="documentație-comună--owner-j1"></a>
<a id="doc-context-deployment-111--prioritate-81"></a>
## Ownership și contracte păstrate

| Limită | Owner și autoritate |
|---|---|
| proză și exemple ale bibliotecilor sursă | README-ul modulului, codul și testele native din J0 |
| stil comun și paritatea edițiilor | [standardul documentar](../README_Atf_DocumentationBestPractices.md#technical-prose-review), guvernanța documentației și comparatorul edițiilor fixate pe commit |
| scenarii ale consumerilor și criterii comune din afara bibliotecilor | J1; acceptanța independentă rămâne la fiecare consumer |

## DOC-GUIDE-PROSE — terminologia rămasă a mentenanței

**Status: Deschis.** Revizuiește terminologia launcherelor/scope-urilor din [feedback-ul activ Test Kit](ATF_Common_Test_Support_Feedback.md) și formulările despre consumeri/sincronizare din [protocolul workspace-urilor](../ATF_Workspace_Synchronization.md), după ediția română și ownerii live.

- Păstrează criteriile nerezolvate, comenzile executabile și bazele originale ale controalelor de teste; elimină narațiunea rezultatelor închise în loc să o traduci.
- Închide după ce pasajele selectate au proză naturală, păstrează același contract în ambele ediții și trec guvernanța documentației.

<a id="source-language-english-2026-11-24"></a>
## SOURCE-LANGUAGE-EN-2026-11-24 — sursă și comentarii în engleză

**Stare: Verificat și închis pentru J0 și J1.** J0 deține regula și scannerul comun din Test Kit; J1 consumă binarul selectat și îl aplică inventarului său Java/MJS. Sursa și testele rămân identice între `main` și `main_RO`; se traduce doar proza documentației guvernate.

- Identificatorii, comentariile/docstrings, numele testelor și diagnosticele scrise de dezvoltatori sunt în engleză. Textele UI localizate, fixture-urile/payload-urile intenționat localizate, numele API/protocol, notația de șah și sursa generată rămân date sau contracte externe.
- J0 deține implementarea canonică; consumerii dețin descoperirea surselor, excepțiile de localizare și includerea în suite. Gate-ul J1 verifică Java și MJS fără o copie proprie de scanner.
- Fixture-urile pozitive/negative, diagnosticele fișier/linie/construcție, guvernanța limbii documentației și integrarea în suita completă au trecut în build-uri staging.

Adopția rămasă este urmărită separat și nu anulează incrementul J0/J1 verificat.

<a id="source-language-adoption-2026-11-24"></a>
## SOURCE-LANGUAGE-ADOPTION-2026-11-24 — ceilalți consumatori ATF

**Stare: Deschis.** J0/J1 stabilesc regula comună Java/MJS. Ceilalți owneri ATF au nevoie de un gate local verificat înainte ca regula să fie declarată universală.

- Inventariază limbajele și rădăcinile sursă întreținute în fiecare modul; exclude explicit sursele generate.
- Adoptă scannerul Test Kit acolo unde sintaxa este suportată. Pentru alte limbaje, extinde-l cu fixture-uri sau definește un gate echivalent, conștient de limbaj; nu aplica verificarea lexicală Java/MJS la sintaxă nesuportată.
- Păstrează aceeași semnificație în documentațiile EN/RO și sursa identică între ramurile lingvistice.
- Include gate-ul în suita obișnuită de guvernanță a fiecărui modul, cu exemple negative de cod și exemple pozitive de date localizate.

Închide per consumer numai după trecerea inventarului surselor, diagnosticelor, includerii în suită și guvernanței documentației.
