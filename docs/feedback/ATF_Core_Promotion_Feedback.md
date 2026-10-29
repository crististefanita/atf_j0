# ATF Core — eligibilitatea părinților destinației

## Pe scurt

Prevalidarea tuturor surselor staging este implementată și probată. Restanța
privește un părinte al destinației care devine fișier după capturarea manifestului,
înaintea apelului de promovare; eroarea structurală este descoperită după prima scriere.

Contractul livrat și atomicitatea per fișier sunt la
[Core](../../atf-core/README.md#manifest-și-promovare-guarded).
Procesul rămâne [izolat în TMP](../ATF_Build_And_Execution_Safety.md#regula-de-lucru-pentru-orice-schimbare).

## CORE-PROMOTION-DESTINATION-33 — părinte neeligibil înaintea execuției

**Prioritate: 66/100** = impact 25 + lacună 18 + lucrabil 18 + documentare 5.
**Owner:** [AtfPromotionGuard](../../atf-core/src/main/java/atf/core/sync/AtfPromotionGuard.java)
și [manifestul](../../atf-core/src/main/java/atf/core/sync/AtfPromotionManifest.java).

Proba folosește exclusiv două directoare temporare și API-ul public:

| Pas | Input independent | Rezultat observat |
|---|---|---|
| Capturează manifestul | `a.txt=old`; `blocked/b.txt` absent și aprobat | baseline valid |
| Pregătește sursele | ambele fișiere staging sunt regulate și citibile | preflight-ul surselor trece |
| Schimbă părintele live | creează fișierul `blocked` înainte de `promote(...)` | `mismatches(live)` rămâne gol; manifestul urmărește fișierele aprobate |
| Promovează | aceeași ordine a manifestului | `FileAlreadyExistsException`; `a.txt=new` este deja promovat |

Părintele invalid este cunoscut la intrarea în guard. Această probă nu susține
o tranzacție pentru întregul lot și nu extinde garanția la permisiuni ori la
schimbări I/O apărute după prevalidare.

- [ ] Includeți eligibilitatea structurală a părinților existenți în planul
      verificat înaintea primei scrieri; un părinte regulat nu poate servi drept director.
- [ ] Păstrați aprobarea explicită pentru destinația absentă, verificările
      symlink/path, baseline-ul și revalidarea per fișier. Nu creați părinții în preflight.
- [ ] Probați părinte existent director, absent, fișier și schimbat după captură;
      inputul structural invalid lasă toate fișierele live și sursele staging intacte.
- [ ] Păstrați explicit limita erorilor ulterioare execuției: atomicitate per
      fișier, fără rollback implicit sau reluarea oarbă a unui lot parțial.

**Închidere:** proba negativă refuză părintele invalid înaintea schimbării lui
`a.txt`; cazurile valide și guard-urile existente rămân verzi.
