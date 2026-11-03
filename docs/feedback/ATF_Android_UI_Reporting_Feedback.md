# Android UI evidence — current neutral Reporting boundaries

## Decizie și boundary

J0 owns neutral publication and permanent source/JAR tests. J1 coordinates
consumer handoffs; J4 owns ADB, full-display capture and application oracles.
The current [J18 delivery](../ATF_Reporting_J18_Verification.md) is separate from
frozen J10/J13 witnesses and from a new device execution.

| Current residual | Owner and closure boundary |
|---|---|
| Long UI comparisons, criterion 133 | capture before caller preview, safe disclosure and source/JAR negatives |
| Observation routes | explicit media versus empty-comparison THEN, with independent flow-mode characterization |
| Additional volume witness | neutral captured inputs and measured publication limits before assigning a renderer defect |

## REPORT-UI-ASSERTION-FULL-EVIDENCE-133 — valori lungi capturate înainte de preview

**Prioritate: 88/100.** Criteriu distinct rezultat din completarea handoff-ului J4;
celulele ACTION/OBSERVE rezolvate nu îl închid.

- [ ] Delimitați valoarea integrală capturată de preview-ul callerului: Kit poate transmite deja numai text redus, pe care Reporting nu îl poate reconstitui.
- [ ] Definiți proiecția sigură pentru expected/actual UI lungi, cu preview 240 și disclosure corelat la scenariu/fază/checkpoint/comparație; fără reevaluare sau secrete ascunse.
- [ ] Păstrați regresie neutră în Reporting Tests: numai ultimul scalar eliminat, sibling omonim, verdict FAIL după 240, Unicode și redacție, Extent/fallback sursă/JAR.
- [ ] Verificați Escape/colapsare, revenire la aceeași comparație și ultimul rând; un tabel scurt cu ellipsis singur nu reconstruiește dovada.

J4 păstrează temporar corpurile TEXT native cu body limit explicit pentru valorile complete.
Adopția și proba aplicației rămân în [handoff-ul consumerului](../../../atf_j1/docs/feedback/ATF_Consumer_Reporting_Handoff_Feedback.md), fără assertions PASS artificiale.

Coordonarea accesului ADB este la [ownerul consumerului J1/J4](../../../atf_j1/docs/feedback/ATF_Consumer_Reporting_Handoff_Feedback.md#coordonarea-probei-j4-pe-dispozitiv-partajat--2026-11-02-1533); probele generice Reporting nu folosesc dispozitivul.

## Observation routes and large-publication follow-up

- [ ] Characterize empty-comparison THEN versus explicit media, including INDIVIDUAL/UNIFIED_FLOW and OBSERVE-only inputs. Document intentional outcome-only behavior or diagnose unsupported capture requests; never recommend invented assertions.
- [ ] Reproduce the new high-volume consumer witness with neutral captured values before assigning a generic renderer defect. Measure native publication peak heap, HTML bytes and final-row readability separately from input composition.

The [consumer witness](../../../atf_j4/docs/ATF_J4_Report_Audit.md#concurrent-consumer-evidence--j15-learning-publication) retains the dated volume, failure attempts and exact acceptance boundary. J13 memory criterion 131 remains closed for its verified fixture; it does not promise unbounded publication capacity.

<details>
<summary><strong>Delivered contracts and historical handoff routes</strong></summary>

## Cerințe obligatorii

[Composite screenshot evidence](../ATF_Inline_Audit_Contract.md#composite-screenshot-evidence) owns the stable composition, failure, identity and source/JAR boundaries.

## Matricea minimă de probă J0

[Composite evidence acceptance](../ATF_Inline_Audit_Contract.md#composite-evidence-acceptance) preserves the neutral fixture and mutation matrix. Consumer device acceptance is separate.

## Restanță generică și handoff

The active J0 residuals above are long assertion capture and separate media/volume characterization. [J1/J4 coordination](../../../atf_j1/docs/feedback/ATF_Consumer_Reporting_Handoff_Feedback.md) owns device inputs and application assertions.

## REPORT-PUBLICATION-MEMORY-131 — copii integrale la publicare

Closed for the verified 321-checkpoint, 1 GiB source/JAR fixture in [J13](../ATF_Reporting_J13_Verification.md). This bounded proof does not promise unbounded publication capacity.

## REPORT-ACTION-FULL-EVIDENCE-132 — expected/observed dincolo de preview

The [ACTION/OBSERVE contract](../../atf-reporting/README.md#actionobserve--preview-și-valori-complete) and [J13](../ATF_Reporting_J13_Verification.md) own complete redacted values, preview 240 and permanent suffix/owner negatives.

## Observation-only screenshot handoff

The frozen J13 missing-frame witness is historical. [J16](../ATF_Reporting_J16_Verification.md) retains explicit media publication with zero invented assertions; the separate empty-comparison route matrix above remains open.

</details>
