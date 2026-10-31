# ATF Validation — contract JSON și criterii rămase

## Pe scurt

VALIDATION-STRICT-93 este închis pentru subsetul JSON documentat și acceptat
separat în J0/J1/J2/J3. Contractul și probele permanente sunt în
[README-ul ownerului](../../atf-validation/README.md#contract-json-și-subset-de-schemă),
iar controlul CONTRACTS-J0-96 este în [registrul verificat](../ATF_Test_Changes_Baseline.md).

Clasificarea MIME de mai jos este un criteriu distinct, adăugat concurent.
Candidatul J2 rămâne separat de acest increment; nu îl declarăm adoptat la
owner sau în ceilalți consumeri. Core C1 și Reporting J7 rămân neschimbate.

## VALIDATION-MEDIA-92 — tipul JSON nu poate fi imitat de un parametru

**Prioritate: 86/100.** Ownerul este Validation comun în J0; Reporting nu
clasifică headerul și nu trebuie să schimbe verdictul primit. Revizia live J0
confirmă că toate cele cinci contracte canonice folosesc încă contains literal.

| Caz public | Comportament de refuzat | Contract necesar |
|---|---|---|
| userItem cu body valid și text/plain; profile="application/json" | PASS prin substring | FAIL numai pe cerința media type, cu payload-ul nominal păstrat |
| application/vnd.atf.user+json | refuz prin absența substringului application/json | clasificare JSON prin tip/subtip concret |
| header absent, wildcard, application/jsonp ori listă de tipuri | potrivire accidentală sau diagnostic lipsă | rând explicit nereușit în strict/warning |

- Reconcilează selectiv regula contentTypeJson, fațada și cele cinci contracte din [candidatul J2](../../../atf_j2/atf-validation/README.md#content-type--clasificare-semantică-json); păstrează refactorizările locale și contains literal compatibil.
- Deleagă clasificarea către MediaTypes.isJson din Core. Nu copia parserul în Validation și nu confunda clasificarea tipului cu validarea body-ului sau a tuturor parametrilor MIME.
- Păstrează identitate distinctă pentru rândul semantic, astfel încât regula și contains să fie compozabile fără duplicate assertion.
- Transferă regresia publică în testele proprii Validation și verifică toate cele cinci contracte, inclusiv string parameters, charset, sufix JSON și diferențe după preview 240; expected-urile sunt independente de funcția testată.
- Testele generice de proiecție rămân componente dedicate în Reporting Tests, cu assertions typed furnizate explicit. Nu muta ContractValidators ori algoritmul MIME în fixture-ul rendererului.

**Închidere:** nicio potrivire falsă pe parametru, nominalele JSON concrete și
contains compatibil sunt demonstrate la owner; acceptanțele deployment-urilor
se execută separat. Cele 31 de regresii și baseline-ul cu 12 eșecuri din J2
sunt dovezi ale candidatului, fără execuție sau livrare implicită J0/J1/J3.
