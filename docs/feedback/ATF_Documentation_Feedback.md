# Documentație ATF — fidelitatea deployment-urilor

## Pe scurt

Atlasul este orientare către cod și POM, iar guvernanța comună aparține ATF Tests.
Nota editorială și trecerea linkurilor nu certifică fidelitatea tuturor afirmațiilor.

## DOC-CONTEXT-DEPLOYMENT-111 — prioritate 81

**Prioritate: 81/100** = impact 31 + lacună 24 + lucrabil 18 + documentare 8.
**Owner:** guvernanța factuală J0; J1/J2/J3 păstrează contractele propriului deployment.

Reproducerea inițială avea J1 cu 10 module, J2 cu 8 și Reporting J6 în atlas,
deși reactorul avea 9/7 module și binarul era J8. Documentele curente indică J10;
guvernanța curentă nu detectează această contradicție semantică.

- Confruntați selectiv afirmațiile curente din atlas/index cu reactorul explicit, proprietatea Reporting și proveniența țintei; versiunea așteptată se citește din deployment, fără constantă istorică în test.
- Proba negativă schimbă numai 9 în 10, 7 în 8 sau versiunea curentă cu una istorică; fiecare contradicție trebuie refuzată la afirmația exactă.
- Acceptați în continuare istoricul legitim J6/J7/J8/J9 și controalele la bazele originale; existența unei versiuni vechi în jurnal nu este singură un defect.
- Distingeți ownerul sursei Core J0 de consumul Core C2 prin JAR în J1/J2/J3; catalogul nu se micșorează implicit după foldere absente.

**Închidere:** documentele nominale și istorice trec, iar mutanții curenți
sunt refuzați fără relaxarea perimetrului. [Criteriul local J2](../../../atf_j2/docs/feedback/ATF_Documentation_Feedback.md)
are acceptanță proprie; nu închide implicit guvernanța comună.

### Current concrete drift from the J2/J3 audit

ATF_Workspace_Synchronization still describes J2 as seven modules and its
framework observer as WIP. The current J2 reactor has eight modules including
atf-sql; observer contracts and prior real infrastructure evidence exist,
while full application HTTP/CDC/saga remains NOT RUN. Reconcile this row with
the J2 owner and preserve the older verification documents as historical.

- J2 also links a J10 delivery through a J8 verification page; J3's governance README retains a current J8 hash label.
- These are actionable current-context contradictions, not a reason to replace every historical version string.
- Bind only current declarations to the deployment facts and prove a wrong- version mutant.
- Keep the approved README style/navigation during cleanup.

## Producer README review — fidelity before expansion

- Core documents structural JSON field selection precisely. The console leak
  is a missing projection/policy boundary, not proof that every described
  redactor invariant is false. Update that owner's contract when the fix lands.
- Reporting README has 2,291 lines, but only 355 nonblank lines outside details;
  Reporting Tests has 1,231 / 314. Progressive disclosure already works: do not
  split approved documents merely to satisfy an arbitrary line count.
- Core README has 390 nonblank visible lines. Improve its short task routes
  around console/cancellation rather than duplicating the security atlas.
- The 75-line production atlas and the module-local portable entry remain good
  context. Public API support, portable normative contracts and release package
  completeness are still separate criteria, already owned by 43/45/46.
- Preserve HTML/fallback publication and threading differences: they are
  explicitly documented, not an undocumented promise of universal atomicity.
- Clarify stale current J2/module-version claims and J3's failed-reactor versus
  isolated-pass wording. Keep dated receipts and test outcomes intact.
