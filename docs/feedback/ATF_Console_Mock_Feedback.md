<a id="console-and-mock--library-ownership"></a>
<a id="console-și-mock--ownership-ul-bibliotecilor"></a>
# Console și Mock — ownership-ul bibliotecilor

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## Pe scurt

J0 deține implementarea comună Console/Mock și testele native. J1–J7 reutilizează JAR-urile și trimit aici constatările generice; adaptoarele, cataloagele și oracolele domeniului rămân locale.

<a id="delivered-ownership-contract"></a>
<a id="contractul-de-ownership-livrat"></a>
## Contractul de ownership livrat

[Consumul bibliotecilor platformei](../ATF_Platform_Libraries.md) definește publicarea standalone, integritatea JAR fizic, păstrarea testelor native și acceptanța independentă. Extracțiile închise ies din backlog; pachetul curent păstrează dovezile detaliate în TMP propriu. Constatările Core/Reporting și J1 UI rămân separate.

<a id="mock-fault-script-111--compare-the-two-existing-failure-fixtures-before-extracting"></a>
<a id="mock-fault-script-111--comparați-cele-două-fixture-uri-de-eșec-existente-înaintea-extracției"></a>
## MOCK-FAULT-SCRIPT-111 — comparați cele două fixture-uri de eșec existente înaintea extracției

Prioritate: 78/100. TerminalFailureFixtures și ConvergenceFailureFixtures din J2 repetă secvențierea finită a transportului, numărarea expedierilor și finalizarea excepțională. J0 deține biblioteca Mock; J2 păstrează oracolele de domeniu și acceptanța independentă.

- [ ] Comparați cerințele concrete ale fixture-urilor înaintea unui API nou; extrageți numai o primitivă comună demonstrată, fără modul Maven nou.
- [ ] Dacă se justifică, păstrați planul imuabil și starea locală invocării; creați o excepție nouă la fiecare execuție și distingeți încercările expediate, rutele potrivite și răspunsurile finalizate.
- [ ] Păstrați metoda, MIME, autentificarea, query-ul, matching-ul JSON/numeric, ordinea strictă și eșecul la epuizare. Politica martorilor OrderContract, saga și SQL rămâne în J2.
- [ ] Adăugați negative native pentru contractul public în J0 și rulați ambele matrice J2 originale pe JAR fizic, inclusiv cauze originale/suppressed și sink-uri eșuate.

Închiderea cere duplicare redusă și aceleași oracole de domeniu independente, fără importarea consumerului în Mock. Extragerea publicației nu implementează această primitivă opțională și nu certifică mesageria live.
