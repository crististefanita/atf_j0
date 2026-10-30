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
- Redactarea cheilor JSON Unicode și a valorilor sensibile non-string are
  [criteriu activ](../../../../../../../docs/feedback/ATF_Reporting_Feedback.md#report-json-redaction-40--secretul-semantic-înaintea-formatării);
  descrierea pipeline-ului nu dovedește acoperirea tuturor formelor de input.

</details>

## Probe

`ReportEvidenceJsonFormatterTest`, `ReportEvidenceTableTest` și
`ReportEvidenceStyleContractTest` verifică proiecția; controalele de renderer și
browser sunt în [Reporting Tests](../../../../../../../atf-reporting-tests/README.md#tabele-publicate-și-inventar-independent).

Pentru o modificare de secret/buget se citesc și
[SensitiveDataRedactor](../security/SensitiveDataRedactor.java) și
[ReportingSettings](../configuration/ReportingSettings.java); pentru geometrie,
contextul continuă la [runtime și assets](../runtime/README.md).

