<a id="drafting-a-game-assertion-name"></a>
<a id="redactarea-unui-nume-de-aserțiune-de-joc"></a>
<a id="redaction-a-game-assertion-name"></a>
# Redaction a game assertion name

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

Reporting has the verification of the publication labels. The criterion covers the preservation of the game text and the masking of real credentials.

<a id="witness-and-status"></a>
<a id="martor-și-stare-curentă"></a>
## Witness and status

The historical witness is from a JVM component published by Reporting J18.

| Surface | Observed value |
|---|---|
| Trapped label | `Safe exit always scores five basic Romanian points` |
| Label in HTML | `Safe exit always scores five basic ***REDACTED*** points` |
| Comparison HTML-Receipt | `Comparison name/order changed` |

- **P2 · owner Reporting.** Extent and fallback shall apply the editor of the assertion label.
- [Textual rule](../../atf-reporting/src/main/java/atf/reporting/security/SensitiveDataRedactor.java) associates `basic` with the following word and in free text. This is the false positive path observed in the J18 Witness.
- The current JAR qualification remains open.

<a id="criteria-for-the-library"></a>
<a id="criterii-pentru-bibliotecă"></a>
## Criteria for the library

- Reproduce in the sources owned by the library, by means of an assertion with this name and whole values.
- Keep the positive sample by masking a basic credit in a context of authentication. Use only a synthetic token.
- Check separately a business sentence with the same two words, a login header and an explicitly configured secret.
- Check the label in DOM and the lack of synthetic token in HTML for both the main renderer and the fallback.

<a id="local-adaptation-of-the-witness"></a>
<a id="adaptarea-locală-a-martorului"></a>
## Local adaptation of the witness

The component was reexecuted in a copy of the frozen source with the `An exact zero bid earns five points under Romanian scoring` label. The six catches of the method have new execution provenance. The original test and HTML rejected were retained.
