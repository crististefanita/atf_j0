# Core C3 și profilurile consumer J2 — verificare 2026-11-02

## Pe scurt

J0 păstrează sursa Core și Reporting și testele lor permanente.
J1–J4 consumă aceeași pereche Core C3; Reporting rămâne J12, fără release artificial pentru adopția unui API existent.

| Artifact | Identitate |
|---|---|
| Core | atf.api:atf-core:1.0.0-c3 |
| SHA-256 JAR Core | 605407B9D40E67C9C352EE6E94DA758E50BD3CFB14CC0D7A0DE3359C71088447 |
| SHA-256 POM Core | 7853373300E588B2EA25F6A07C927ECAC1FB06044073AD75FDA8D6B7CFF275E0 |
| Reporting neschimbat | atf.api:atf-reporting:1.0.0-j12 |
| SHA-256 JAR Reporting | 1F1B7B5502FEFEA02F500C2FC80494859D4E57E38261306B57FA7AD2D5AB38B1 |

## Contracte probate

- Core redactează numele START/COMPLETE și string-urile JSON publice decodate folosind politica configurată; request-ul și response-ul originale rămân intacte.
- HttpClientTransport transmite best effort anularea către upstream, o dată pentru caller, cu interrupt flag păstrat; completarea concurentă nu înlocuiește verdictul terminal.
- Eșecul cleanup este suppressed pe anularea primară. GET-ul local blocat păstrează un singur dispatch, CANCELLED și absența unui răspuns inventat.
- Profilurile J2 sunt declarate de autor, înaintea citirii HTML; oracolele de identitate, efecte business, SQL typed și valori complete rămân independente.

## Matricea executată

| Probă | Rezultat / limită |
|---|---|
| Core sursă | 286 native, zero fail/error/skip |
| Core parentless | două build-uri noi independente, 286 native fiecare, JAR byte-identic |
| Core JAR fizic | aceleași 286 native plus un control CodeSource; fără surse Core |
| API C2 → C3 | zero simboluri public/protected eliminate |
| Reporting pe Core C3 | 16 cazuri sursă, 43 cazuri JAR și un control de identitate; zero fail/error/skip în gate-urile acceptate |
| J2 Eventuate | 14 rapoarte, 18 invocări, șase FAIL-uri JUnit controlate |
| J2 Mock | 14 rapoarte, comparații și verdicturi din probele independente; fără HTTP fictiv |
| J2 SQL | 12 rapoarte, 36 invocări, patru excepții JDBC controlate |
| Profiluri J2 | 40/40 conforme; fiecare mutant ACTUAL refuzat; cinci contracte unitare suplimentare |
| Browser | Extent suită/FAIL, Mock combinat și mesaje/SQL fallback; mouse, Enter/Space, ultimul rând și backlink exact |

## Limite și documentație

SQL nu are dependență runtime Core sau Reporting. Eventuate încarcă Core C3, iar ambele module consumă Reporting J12 fizic.
Anularea unui future nu dovedește rollback server-side; aceste probe nu certifică aplicația Kafka, ASR sau Android.

- Feedback-ul activ J0 păstrează numai Core/Reporting. Test Kit, guvernanța comună, AI și coordonarea consumerilor sunt transferate în registrul J1.
- Istoricul auditului rămâne document datat; contractele probate se citesc la owner. Transferul nu închide criteriile fără implementare și probă.

Dovezi TMP: ATF_ADOPTION_CORE_C3_ACTIVE_DO_NOT_CLEAN_HjeEfO — gate-uri, XML, core-binary-api.json, matricile C3, inventar și promovare guarded.
Regresiile Reporting pe sursă/JAR și guvernanța finală sunt consemnate în receiptele incrementului.


<details>
<summary><strong>Rulări excluse și limitele acceptanței</strong></summary>

- Un replay sursă a atins limita DOM de 5 s la JsonEvidenceFidelityComponentTest în context concurent. Repetarea izolată are 16/16 verzi, cu același prag; logul inițial este păstrat.
- Două gate-uri documentare au țintit alt staging și sunt excluse în invalid-documentation-gates.json. Gate-urile corecte verifică explicit identitatea proiectului; runnerul refuză nepotrivirea nume/workdir.
- Cache-ul TMP J4 copiat conținea POM/JAR invalide; verificarea acceptată folosește cache nou și două contracte Core/Reporting verzi.
- Publisherul Core intermediar a fost înlocuit înaintea livrării. Numai publisher-3/4 produc candidatul final, byte-identic, cu toate cele 149 intrări fixate.
- Cerințele Reporting noi de memorie la volum mare și ACTION/OBSERVE integral rămân deschise în J0; mitigările J4 și Core C3 nu le închid.

</details>
