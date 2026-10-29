# ATF Core — prevalidarea promovării surselor

## Pe scurt

`AtfPromotionGuard` protejează baseline-ul live și înlocuiește atomic fiecare
fișier. Validarea surselor staging este însă făcută în bucla de copiere;
o sursă lipsă ulterior poate lăsa primul fișier deja promovat.

Contractul și limita curentă sunt la [Core](../../atf-core/README.md#manifest-și-promovare-guarded)
și în [procesul de build/sync](../ATF_Build_And_Execution_Safety.md#guard-software-pentru-sync-livestaging).
Nu se propune un modul nou, un sistem de tranzacții sau rollback automat.

## CORE-PROMOTION-PREFLIGHT-26 — surse eligibile înaintea primei scrieri

**Prioritate: 80/100** = impact 32 + lacună 23 + lucrabil 18 + documentare 7.
**Owner:** Core, în [AtfPromotionGuard](../../atf-core/src/main/java/atf/core/sync/AtfPromotionGuard.java).

Proba folosește exclusiv două directoare temporare și portul public:

| Pas | Input independent | Observație |
|---|---|---|
| Capturează manifestul | live are `a.txt=old-a`, `b.txt=old-b`; ambele aprobate | baseline valid, fără drift |
| Pregătește staging | numai `a.txt=new-a`; `b.txt` lipsește | eroare cunoscută înainte de execuție |
| Apelează `promote(...)` | aceeași ordine de manifest | `IOException` la a doua sursă; live păstrează `a.txt=new-a`, `b.txt=old-b` |

Aceasta este o promovare parțială la input invalid, nu un conflict live
reprodus și nici o pierdere de fișiere. Proba existentă
[AtfPromotionGuardTest](../../atf-core/src/test/java/atf/core/sync/AtfPromotionGuardTest.java)
acoperă drift-ul inițial și crearea autorizată, fără cazul celei de-a doua surse lipsă.

- [ ] Delimitați prevalidarea de execuție: verificați toate sursele aprobate și
      destinațiile înaintea primei scrieri; planul păstrează căile relative,
      prezența și baseline-ul fără stare business sau output-uri în checkout.
- [ ] O sursă absentă ori neeligibilă în orice poziție refuză planul fără
      modificarea live-ului; conservați verificările de path și symlink existente.
- [ ] Păstrați revalidarea baseline-ului la execuție și blocarea promotorilor
      cooperanți. Prevalidarea nu autorizează overwrite după drift și nu
      promite izolarea editorilor care nu folosesc lock-ul.
- [ ] Probați două fișiere valide, sursa ulterioară lipsă/neeligibilă, conflict
      live inițial, creare concurentă și fișier neaprobat; expected-ul nu este
      extras din lista produsă de implementare.
- [ ] Distingeți refuzul inițial de un eșec I/O apărut după pornirea execuției;
      atomicitatea rămâne per fișier, fără declarație de tranzacție pentru întregul lot.

**Închidere:** inputul staging invalid este refuzat înaintea primei modificări,
cu cauza și calea păstrate; regresia negativă detectează comportamentul anterior.
Verificarea și promovarea urmează [procesul izolat](../ATF_Build_And_Execution_Safety.md),
iar probele permanente rămân la ownerul Core.
