# ATF UI — sincronizare și recuperare ghidată de backend

## Pe scurt

**Stare: sincronizare nativă, politică BE read-only și cleanup complet livrate;
proiecția typed a încercărilor rămâne deschisă.**
Ownerii sunt UI pentru sincronizare, Kit pentru declarația neutră,
consumerii pentru semantica operațiilor și Reporting pentru dovezile randate.

- Continuitatea selectului este livrată la [UI](../../atf-ui/README.md#așteptare-nativă-și-unicitate):
  observație read-only înaintea unicei selecții native, în deadline-ul existent.
  Precondițiile business ale checkout-ului rămân la consumer.
- Reluarea întregului test, a creării contului, a coșului ori a plății rămâne
  exclusă din acest increment; un rezultat necunoscut al mutației oprește replay-ul.

Contractele existente sunt în [UI](../../atf-ui/README.md),
[AutoEx](../../atf-test-auto-ex/README.md#coeziunea-suportului-autoex)
și [Kit](../../atf-test-kit/README.md). Căutarea catalogului folosește
[clickul unic cu postcondiție](../../atf-ui/README.md#click-unic-cu-postcondiție-explicită),
cu [semantica AutoEx](../../atf-test-auto-ex/README.md#confirmarea-căutării-în-catalog).
Acest feedback deține diferențele rămase,
nu schimbă acele contracte prin simpla propunere a unei configurații.

**Prioritate: 78/100** = impact 33 + lacună 22 + lucrabil 16 + documentare 7.
Criteriul aparține Reporting; nu reprezintă o restanță a politicii UI livrate.
Incrementul fără Reporting păstrează acest contract și nu închide proiecția
prin adăugarea unor mesaje INFO la consumer.

## UI-RECOVERY-AUDIT-01 — încercări inspectabile în Extent

**Owner:** model/proiecție la Reporting; probe renderer la Reporting Tests;
adaptoare și aserțiuni business în main la cei doi consumeri.

Observațiile INFO livrate sunt normate la
[Reporting](../../atf-reporting/README.md#decizii-be-fără-verdict-de-succes-implicit).
`UiRecoveryTrace` deține deja operația, încercarea, decizia, motivul, semnalul BE
și duratele; adaptoarele AutoEx/Shop le publică în rânduri INFO. Restanța este
proiecția structurată comună și matricea de acceptanță, fără parsarea mesajelor
sau repetarea deciziei la renderer.

- [ ] Compuneți faptele typed din trace, policy și signal în proiecția comună:
      operație, încercare, decizie/motiv, sursa politicii, bugete și timpi măsurați.
      Signal-ul păstrează deja scope-ul invocare/operație/frame/document și secvența;
      rendererul primește corelarea neutră de la adaptor, fără parsarea mesajului.
      Starea DOM apare numai dacă a fost observată; lipsa ei rămâne necapturată,
      fără acțiuni sau capturi suplimentare pentru completarea tabelului.
      Distingerea observației de replay trebuie să fie vizibilă în raport.
      Separați durata încercării, backoff-ul și timpul total măsurat; o durată
      indisponibilă rămâne explicit necapturată, fără valoare HTTP inventată.
- [ ] Păstrați toate cauzele intermediare și capturile permise. Recuperarea
      înaintea eșecului terminal poate avea rânduri INFO explicite, conform
      recorderului existent; nu rescrieți un FAIL deja publicat în PASS și nu
      înlocuiți raportul original cu artifactul unei rulări ulterioare.
- [ ] Definiți verdictul logic și indicatorul de recuperare: un singur test
      în totaluri, succes inițial distinct de recuperare, epuizare FAIL și abort
      păstrat SKIP. Retry-ul infrastructurii de publicare nu rerulează business-ul.
      Inputul contradictoriu este reconciliat conform
      [contractului comun Reporting](../../atf-reporting/README.md#consistența-statusului-transmis-de-caller).
- [ ] Probați expand/collapse până la fiecare încercare, acțiune, request și
      aserțiune; Fails → test/fază/pas exact și Overview trebuie să păstreze
      cauzele și totalurile coerente. Captura ulterioară nu înlocuiește starea
      consemnată la eroare.
- [ ] Verificați câte un caz relevant individual și prin suită în AutoEx și
      Shop, cu profil UI și UI+API, succes, recuperare/refuz și epuizare. Probele
      Extent neutre rămân în `atf-reporting-tests`; policy/resolver și adaptoarele
      au teste la owner, fără cuplarea testelor de business la HTML.

## Contracte păstrate și criteriu de închidere

- Auditul componentelor are evaluare JUnit unică și pas colapsabil standard,
  conform [ghidului de testare](../ATF_Testing_Guide.md#auditul-componentelor-consumerilor).
- Așteptarea nativă și activarea unică sunt la [UI](../../atf-ui/README.md#așteptare-nativă-și-unicitate);
  precondiția de identitate/coș este la [consumer](../../atf-test-auto-ex/README.md#gate-ul-coșului-și-ownerul-contului).

Bugetul redactat configurat, paritatea rendererelor și lizibilitatea dovezilor sunt
contracte [Reporting](../../atf-reporting/README.md#proiecția-sigură-a-aserțiunilor),
inclusiv în [panouri înguste](../../atf-reporting/README.md#lizibilitatea-dovezilor-în-panouri-înguste).
Aceste invariante rămân obligatorii pentru viitoarele încercări de recuperare.

Politica validată și exemplele livrate sunt la ownerii UI, Kit, Reporting și
consumeri, cu precedența documentată în
[ghidul adnotărilor](../ATF_Test_Infrastructure_Annotations.md).
[Geometria iframe-urilor](../../atf-ui/README.md#geometria-nativă-a-contextului)
are contract și probe separate la ownerul UI.
Verificarea și publicarea urmează [procesul izolat](../ATF_Build_And_Execution_Safety.md);
o repetare manuală parțială nu închide criteriile de recuperare rămase.

Controlul politicii BE livrate este în
[baseline-ul verificat](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate).

## Acceptanță pentru reevaluarea UI

- Regresiile start identity și ale livrării izolate protejează
  [contractele deja implementate](../../atf-ui/README.md#inspectorul-de-rețea--limită-generică-de-responsabilitate).
  Cleanup-ul păstrează [contractul scope-ului închis](../../atf-ui/README.md#inspectorul-de-rețea--limită-generică-de-responsabilitate), inclusiv refuzul nativ al unei dezabonări.
- Politica existentă de recuperare nu devine retry al întregului test. Selecția
  și geometria păstrează activarea unică, iar rezultatul necunoscut al unei
  mutații rămâne refuz, nu permisiune de replay.
- Rulați probele locale înaintea câte unui caz consumer relevant; browserul și
  Reporting nu intră în Kit, iar adaptorul business nu repară rendererul prin CSS local.

Nota se revede prin [rubrica de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor),
cu criteriile independente închise la owner; o singură componentă verde nu
certifică toate adaptările consumerilor.
