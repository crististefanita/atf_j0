# Reporting J7 — dovada incrementului observations

## Pe scurt

Livrarea este `atf.api:atf-reporting:1.0.0-j7`, Java 21, thin JAR/POM.
SHA-256 JAR: `569D2BABE6B9EC091CB1DF6E1207110C611327EA15F1521955E847C837D9EB2A`.
SHA-256 POM: `A11E697CC2FA8A2C10790FF697B8D2AF16C797EFFA448CFCCE5914681B31D569`.

| Boundary | Dovadă |
|---|---|
| producător | 52 native, zero FAIL/ERROR/SKIP; fără module ATF în dependențe |
| reproducibilitate | 2 build-uri curate în TMP distincte; bytes identici |
| simboluri | 4193 → 4267; zero eliminări, 74 adăugări public/protected |
| compoziție/browser | 11 cazuri: INFO neutru, FAIL conservat, escaping/redacție/240, glyph local, deschidere manuală individuală fără Expand all, keyboard și scroll complet; Extent/fallback |
| JAR fizic | consumer Maven fără surse Reporting; 12 JAR-uri runtime, zero Core/Kit/JUnit/Playwright; statusuri PASS/FAIL/PASS în ambele renderere |
| refuz negativ | asset table-viewport absent → refuz Extent/fallback; artifactul anterior este păstrat |
| J1 | un caz Shop și un publisher AutoEx, apoi 12 probe de suprafață binară și controlul schimbării; 9 module, Core C1 |
| J2 | 3 componente Eventuate MOCK, 12 probe de suprafață binară și controlul schimbării; 7 module, Core C1 |
| J3 | 8 module; 25 nominale per renderer și controale separate FAIL/SKIP; transcriere operațională MP4 cu TXT/JSON/Extent INFO |

Inventarul simbolurilor nu certifică switch-uri exhaustive pe arborii sealed;
noile variante cer review la recompilare. Acceptanțele nu sunt suitele web
complete Shop/AutoEx și nu certifică brokerul real Eventuate.

Sursa și harness-ul permanent rămân la J0. J1/J2/J3 primesc aceeași pereche;
Core C1 și istoricul J6 nu sunt reconstruite sau rescrise ca parte a livrării.
Proveniența păstrează manifestul sursei, nativele și acceptanța distinctă.

[Contract](../atf-reporting/README.md#observații-publice-fără-transport) ·
[regresii](../atf-reporting-tests/README.md#observații-neutre-și-disclosure-imbricat) ·
[proces](ATF_Workspace_Synchronization.md).
