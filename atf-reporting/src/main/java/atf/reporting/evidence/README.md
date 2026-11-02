# ATF Reporting Evidence — date observate, format și disclosure

## Pe scurt

Pachetul transformă dovezile primite în tabele și markup. Deține politica de
display și prezentarea capturilor existente; nu capturează ecranul și nu
execută operația pentru a completa un rând lipsă.

[Atlas](../README.md) · [Profil UI](profile/ReportEvidenceProfile.java) ·
[Contractul de display](../../../../../../README.md#lizibilitatea-dovezilor-în-panouri-înguste)

| Schimbare | Sursa | Responsabilitate |
|---|---|---|
| dispatch între UI și transport | [ExtentEvidenceRenderer](ExtentEvidenceRenderer.java) | coordonează renderere specializate, fără lifecycle de nod |
| request/response/attempts | [ExtentTransportEvidenceRenderer](ExtentTransportEvidenceRenderer.java) | tabele tehnice și dovezi JSON |
| acțiuni, observations și assertions UI | [ExtentUiEvidenceRenderer](ExtentUiEvidenceRenderer.java), [ui](ui) | owner și prezentare UI |
| ordinea redactare/format/plafon | [ReportEvidenceJsonFormatter](markup/ReportEvidenceJsonFormatter.java) | politică comună Extent/fallback |
| câmpuri și tabele | [EvidenceTableFormatter](EvidenceTableFormatter.java), [ReportEvidenceTable](ReportEvidenceTable.java) | structură și date de display |
| sumar de excepție | [ReportFailureDescription](diagnostic/ReportFailureDescription.java) | graf bounded, redactare înainte de 240; fără decizie business |
| imaginea atașată acțiunilor | [UiActionScreenshotPolicy](policy/UiActionScreenshotPolicy.java) | cardinalitate, fără captură browser |

## Pipeline-ul de citit împreună

- Modelul păstrează faptele complete pentru verdict și asociere; copia de
  display aplică redactare, formatare și bugetul configurat.
- Formatterul JSON este folosit de ambele renderere. O corecție numai în HTML-ul
  Extent poate lăsa fallback-ul sau o altă proiecție nealiniată.
- Disclosure-ul deține interacțiunea; patru coloane de assertions și șase de
  acțiuni au roluri diferite. Timpul unei comparații nu este inventat din timpul acțiunii.

<details>
<summary><strong>Invariante pentru capturi, status și siguranță</strong></summary>

- O secvență cu acțiuni are cel mult un payload screenshot distinct;
  observațiile fără acțiuni pot avea galerie, conform politicii existente.
- Screenshot policy, flow mode și action details sunt axe separate ale profilului.
- `ExtentReportUiEvidence` poate insera capturi PNG deja produse într-un artifact;
  numele său nu îl transformă într-un provider Playwright.
- Verdictul se păstrează din intrarea typed. Trunchierea textului nu produce o
  comparație nouă și nu transformă expected/actual egale vizual în PASS.
- [Redactorul JSON](../security/JsonEvidenceRedactor.java) interpretează cheia
  semantică înaintea valorii; maschează și scalare/array/obiect, păstrează
  câmpurile duplicate și nu modifică valorile folosite de caller la verdict.
- [Sumarul assertion-urilor](diagnostic/ReportAssertionSummary.java) este numai
  diagnostic sigur, cu redacție per câmp și plafon 240; nu reevaluează comparațiile.

</details>

## Probe

[ReportEvidenceJsonFormatterTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/evidence/markup/ReportEvidenceJsonFormatterTest.java), [ReportEvidenceTableTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/evidence/ReportEvidenceTableTest.java) și
[ReportEvidenceStyleContractTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/evidence/ReportEvidenceStyleContractTest.java) verifică proiecția; controalele de renderer și
browser sunt în [Reporting Tests](../../../../../../../atf-reporting-tests/README.md#tabele-publicate-și-inventar-independent).

Pentru o modificare de secret/buget se citesc și
[SensitiveDataRedactor](../security/SensitiveDataRedactor.java) și
[ReportingSettings](../configuration/ReportingSettings.java); pentru geometrie,
contextul continuă la [runtime și assets](../runtime/README.md).


## Valorile complete ACTION/OBSERVE

| Owner | Contract |
|---|---|
| [UiEvidenceValueMarkup](ui/UiEvidenceValueMarkup.java) | redactează o singură valoare; preview 240 și disclosure complet în aceeași celulă |
| [ACTION](ui/ExtentUiActionRenderer.java) și [OBSERVE](ui/ExtentUiObservationRenderer.java) | folosesc aceeași proiecție în Extent și fallback, cu ordinal și status capturate |

SUMMARY_ONLY rămâne explicit; body display limit și valorile assertions au contracte separate.

## Un singur owner pentru rândul UI

[UiEvidenceRowMarkup](ui/UiEvidenceRowMarkup.java) proiectează operation, ordinal,
target și valorile capturate pentru ACTION și OBSERVE, cu aceeași politică de redactare.
[UiEvidenceValueMarkup](ui/UiEvidenceValueMarkup.java) păstrează preview-ul 240 și
textul integral, inclusiv începutul și finalul whitespace-ului în DOM.

- ACTION păstrează coloana TIME; OBSERVE păstrează structura fără timp.
- Fallback deleagă recordurile originale; titlul PNG folosește politica rendererului.
- Verdictul și comparațiile rămân input capturat, independent de textul redactat.
