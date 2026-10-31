# ATF Core — determinismul probelor de deadline

## Pe scurt

Acest criteriu privește o probă nativă Core observată în J3, fără schimbare
de implementare Core. Contractele Validation/Speech au acceptanțe separate.

## CORE-DEADLINE-PROBE-01 — distinge expirarea înainte și după dispatch

**Prioritate:** 78/100 = impact 29 + lacună 24 + lucrabil 18 + documentare 7.
**Owner:** testele native Core în J0; J3 verifică sursa sa reconciliată.

În prima rulare J3, `ApiDiagnosticIsolationTest` a eșuat la
`pending.isCancelled()` cu un deadline de 10 ms; întregul Core a trecut la
reverificare, fără modificări. Nu este demonstrată cauza unică. Testul cere
implicit dispatch înainte de expirare, deși deadline-ul poate expira anterior.

- [ ] Reproduceți sub încărcare și separați două stări controlate: fără
  dispatch la deadline expirat și anularea viitorului efectiv trimis.
- [ ] Folosiți sincronizare/clock injectabil la boundary dacă se justifică;
  nu creșteți timeout-ul ca singură corecție și nu eliminați anularea.
- [ ] Păstrați cauza TimeoutException, istoricul attempts, diagnosticul
  suppressed și probe negative care arată defectul vizat.

**Închidere:** cazurile sunt deterministe, eșuează la încălcarea contractului
lor și trec repetat sub încărcare; o rerulare verde singură nu închide criteriul.

[Sursa probei](../../atf-core/src/test/java/atf/core/http/diagnostics/ApiDiagnosticIsolationTest.java) ·
[Feedback activ](README.md).
