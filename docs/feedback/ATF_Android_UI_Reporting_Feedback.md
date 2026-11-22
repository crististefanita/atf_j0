# Android UI evidence — current neutral Reporting boundaries

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## Pe scurt

Registrul urmărește probele neutre ale bibliotecii Reporting și acceptanța lor pe JAR. Adaptorul Android deține captura și oracolul aplicației.

<a id="decision-and-boundary"></a>
<a id="decizie-și-boundary"></a>
## Decizie și boundary

J0 deține publicarea neutră și testele permanente sursă/JAR. J1 coordonează consumerii; J4 deține ADB, captura integrală a display-ului și oracolele aplicației. [proveniența curentă a bibliotecii](../../../atf_j1/library-provenance.json) este separat de martorii J10/J13 înghețați și de o nouă execuție pe dispozitiv.

| Current residual | Owner și limita închiderii |
|---|---|
| Comparații UI lungi, criteriul 133 | captură înainte de preview-ul callerului, disclosure sigur și negative pe sursă/JAR |
| Rute de observație | media explicit versus THEN fără comparații, cu o caracterizare independentă a modului flow |
| Martor suplimentar pentru volum | [REPORT-COMPONENT-FRAGMENTS](ATF_Reporting_Feedback.md#report-component-fragments--costul-multor-corpuri-mici) urmărește măsurătorile neutre ale publicării |

<a id="provenence-of-ui-shares"></a>
<a id="proveniența-acțiunilor-ui"></a>
## Proveniența acțiunilor UI

[Contractul UI neutru](../../atf-reporting/README.md#neutral-ui-attribution-and-failure-ownership) deține titlurile fidele și navigarea la rândul sursă în Extent/fallback; criteriul rămas pentru comparații lungi este separat.

<a id="report-ui-assertion-full-evidence-133---long-values-captured-prior-to-forecast"></a>
<a id="report-ui-assertion-full-evidence-133--valori-lungi-capturate-înainte-de-preview"></a>
## REPORT-UI-ASSERTION-FULL-EVIDENCE-133 — valori lungi capturate înainte de preview

**Prioritate: 88/100.** Criteriu distinct rezultat din completarea handoff-ului J4; celulele ACTION/OBSERVE rezolvate nu îl închid.

- [ ] Delimitați valoarea integrală capturată de preview-ul callerului: Kit poate transmite deja numai text redus, pe care Reporting nu îl poate reconstitui.
- [ ] Definiți proiecția sigură pentru expected/actual UI lungi, cu preview 240 și disclosure corelat la scenariu/fază/checkpoint/comparație; fără reevaluare sau secrete ascunse.
- [ ] Păstrați regresie neutră în Reporting Tests: numai ultimul scalar eliminat, sibling omonim, verdict FAIL după 240, Unicode și redacție, Extent/fallback sursă/JAR.
- [ ] Verificați Escape/colapsare, revenire la aceeași comparație și ultimul rând; un tabel scurt cu ellipsis singur nu reconstruiește dovada.

J4 păstrează temporar corpurile TEXT native cu body limit explicit pentru valorile complete. Adopția și proba aplicației rămân în [handoff-ul consumerului](../../../atf_j1/docs/feedback/ATF_Consumer_Reporting_Handoff_Feedback.md), fără assertions PASS artificiale.

J4 deține calificarea aplicației și a accesului ADB pe dispozitiv; probele neutre Reporting nu folosesc dispozitivul. Consultați [contractul curent de captură și ownership](../../../atf_j4/docs/reporting/ATF_Android.md#capture-and-ownership).

<a id="observation-routes-and-large-publication-follow-up"></a>
<a id="rute-de-observație-și-urmărirea-publicațiilor-mari"></a>
## Rute de observație și urmărirea publicațiilor mari

- [ ] Caracterizați THEN fără comparații față de media explicită, inclusiv INDIVIDUAL/UNIFIED_FLOW și intrări numai OBSERVE-. Documentați comportamentul intenționat bazat doar pe rezultat sau refuzul capturilor nesusținute; nu recomandați aserțiuni inventate.

[J4 audit contract](../../../atf_j4/docs/reporting/ATF_Audit.md) deține calificarea de volum specifică producătorului și acceptanța HTML detașată. Rezultatul vechi de memorie J13 se aplică numai fixture-ului limitat; nu promite capacitate nelimitată.

<details>
<summary><strong>Contracte încheiate și rute istorice de predare</strong></summary>

<a id="mandatory-requirements"></a>
<a id="cerințe-obligatorii"></a>
## Cerințe obligatorii

[Composite screenshot evidence](../ATF_Inline_Audit_Contract.md#composite-screenshot-evidence) owns the stable composition, failure, identity and source/JAR boundaries.

<a id="j0-minimum-sample-matrix"></a>
<a id="matricea-minimă-de-probă-j0"></a>
## Matricea minimă de probă J0

[Composite evidence acceptance](../ATF_Inline_Audit_Contract.md#composite-evidence-acceptance) păstrează fixture-ul neutru și matricea de mutații. Acceptanța dispozitivului rămâne separată.

<a id="generic-and-handoff-retention"></a>
<a id="restanță-generică-și-handoff"></a>
## Restanță generică și handoff

Restanțele J0 active sunt în tabelul curent de mai sus. [coordonarea J1/J4](../../../atf_j1/docs/feedback/ATF_Consumer_Reporting_Handoff_Feedback.md) deține intrările dispozitivului și aserțiunile aplicației.

<a id="report-publication-memory-131---full-copies-of-the-publication"></a>
<a id="report-publication-memory-131--copii-integrale-la-publicare"></a>
## REPORT-PUBLICATION-MEMORY-131 — copii integrale la publicare

Închis pentru fixture-ul sursă/JAR verificat cu 321 checkpoint-uri și 1 GiB, din acceptanța J13 originală. [Reporting owner contract](../../atf-reporting/README.md) păstrează contractul de publicare limitat. Proba nu promite capacitate nelimitată.

<a id="report-action-full-evidence-132---expected--observed-beyond-the-forecast"></a>
<a id="report-action-full-evidence-132--expectedobserved-dincolo-de-preview"></a>
## REPORT-ACTION-FULL-EVIDENCE-132 — expected/observed dincolo de preview

[ACTION/OBSERVE contract](../../atf-reporting/README.md#actionobserve--preview-și-valori-complete) deține valorile redactate complete, preview-ul 240 și negativele permanente pentru sufix/owner.

## Observation-only screenshot handoff

Martorul J13 înghețat pentru cadrul lipsă este istoric. [inline audit contract](../ATF_Inline_Audit_Contract.md) păstrează publicarea media explicită fără aserțiuni inventate; matricea separată pentru rute fără comparații rămâne deschisă.

</details>

<a id="full-assertion-capture--caller-and-renderer-responsibilities"></a>
<a id="capturarea-integrală-a-aserțiunilor--responsabilitățile-callerului-și-rendererului"></a>
## Capturarea integrală a aserțiunilor — responsabilitățile callerului și rendererului

`RecordedAssertions` din Kit acceptă `AssertionValueDisplay` cu observer opțional pentru valorile `Projection`. Observerul primește textul redactat complet, iar rândurile păstrează preview-uri limitate. Rămâne conectarea capturii la Reporting și definirea retenției când `ValidationTable` sau adaptoarele furnizează valori deja limitate.

- [ ] Coordonați captura cu [J0 Test Kit owner](ATF_Common_Test_Support_Feedback.md#retenția-comparațiilor--coordonare-cu-reporting) și [J1 integration](../../../atf_j1/docs/feedback/ATF_Common_Test_Support_Feedback.md). Păstrați preview-ul 240, evaluați comparația originală o singură dată și redactați înaintea afișării.
- [ ] Adăugați un martor neutru de compoziție cu primul mismatch după preview și un negativ independent care elimină numai sufixul păstrat; disclosure-ul ACTION/OBSERVE integral nu închide singur captura aserțiunilor.

<a id="report-setup-media-completeness---sample-set-only-with-average"></a>
<a id="report-setup-media-completeness--probă-setup-numai-cu-media"></a>
## REPORT-SETUP-MEDIA-COMPLETENESS — probă setup numai cu media

**Lacună de regresie · responsabil Reporting.** Codul live transmite captura setup nenulă către `request.screenshot(...)`, iar `UiSetupFallbackParityComponentTest` verifică setup cu acțiuni și comparații, în Extent și fallback. Martorul J22 descrie o versiune veche și nu dovedește o pierdere în binarul curent. Lipsește o probă izolată pentru setup cu media, dar fără acțiuni și comparații, cu asocierea exactă a PNG-ului la owner.

- [ ] Adăugați cazul setup media-only fără pași inventați; verificați payload-ul
      prin legătura ownerului, dimensiuni/pixeli și hash, astfel încât logo-ul sau captura unui sibling să nu satisfacă oracolul.
- [ ] Calificați sursa și JAR-ul fizic în Extent/fallback pe ruta acceptată și
      confirmați că viewerul revine la același owner. Nu deduceți suport OBSERVE numai din acceptarea unui screenshot de setup.

Un test cu raster sintetizat este probă neutră de raportare, nu execuție Android; J4 califică separat capturarea device-ului și payload-ul furnizat de adaptor.
