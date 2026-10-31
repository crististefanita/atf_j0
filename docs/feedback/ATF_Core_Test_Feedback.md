# ATF Core — probe deadline revizuite

## Pe scurt

Probele disting ownership-ul înainte și după dispatch, cu snapshot și cauză verificate.

## CORE-DEADLINE-PROBE-01 — distinge expirarea înainte și după dispatch

**Stare: închis prin probe de ownership.** Clock-ul controlat forțează separat
zero dispatch și un dispatch la bugetul de 10 ms. Proba cu diagnostice păstrează
același buget, verifică starea observată în 20 de repetări și conservă cauza.

- Cancel omis: assertion-ul probei post-dispatch refuză mutația.
- Guard înainte de dispatch omis: proba cu clock forțat refuză mutația.
- Diagnostic suppressed omis: proba păstrării cauzei refuză mutația.

[Contract și probe permanente](../../atf-core/README.md#probele-deadline-ului-și-ownership-ul-future-ului)
deține distincția dintre deadline și failsafe-ul JUnit. Nu s-a modificat
implementarea Core, iar consumerii binari păstrează perechea existentă.
