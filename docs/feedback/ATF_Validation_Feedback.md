# ATF Validation — contracte reconciliate

## Pe scurt

Contractele JSON și MIME sunt la [ownerul Validation](../../atf-validation/README.md).
Regresia semantică verifică separat headerul, body-ul și politica strict/warning;
rendererul primește verdicturile deja evaluate.

## VALIDATION-MEDIA-92 — tipul JSON nu poate fi imitat de un parametru

**Stare: închis la owner și reconciliat în J1/J2/J3.** Cele cinci contracte
canonice folosesc contentTypeJson, delegat către Core MediaTypes.isJson;
contains literal rămâne compatibil. Regresia permanentă cu 31 cazuri și
acceptanțele Extent/fallback păstrează rândurile, verdicturile și preview 240.

[Contractul și limitele](../../atf-validation/README.md#content-type--clasificare-semantică-json)
separă clasificarea tipului/subtipului de validarea parametrilor ori a body-ului.
Problemele generice de randare rămân în feedback-ul Reporting J0; închiderea
acestui criteriu nu declară remediat rendererul.
