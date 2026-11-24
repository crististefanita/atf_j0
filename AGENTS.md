<a id="atf-workspace-rules"></a>
<a id="regulile-workspace-ului-atf"></a>
# Regulile workspace-ului ATF

- Folosește workspace-ul frate `atf_j0` (`../atf_j0`) ca referință curentă
  pentru sursele și testele Core/Reporting/Console/Mock/Debug UI.
  `demo2` este istoric; nu îl edita sau sincroniza pentru lucrări noi.
- Citește `docs/ATF_Workspace_Synchronization.md` și
  `docs/ATF_Build_And_Execution_Safety.md` înainte de schimbarea sau livrarea codului.
- Păstrează implementarea/testele native Reporting în `atf_j0/atf-reporting` și
  regresiile permanente cu mock, contract și browser în `atf_j0/atf-reporting-tests`.
- După o schimbare de bibliotecă în J0, evaluează fiecare consumer inițializat
  afectat din `atf_j1`–`atf_j7`; livrează și validează separat. Inspectează
  domeniul actual al fiecărui proiect înaintea promovării; rezultatul unui
  consumer nu implică acceptanța altuia.
- J2 își păstrează domeniul Git și de mesagerie. Profilul Eventuate explicit include Setup și exclude Shop, AutoEx, UI și Debug UI; nu restaura aceste module sau Playwright în J2.
- Păstrează implementarea și testele native Core și Debug UI numai în J0. Consumerii binari
  păstrează perechile JAR/POM selectate în cataloagele locale direct în `lib`,
  cu o singură proveniență la rădăcină. Nu copia sursa Reporting sau harness-ul ei
  în consumerii binari live.
- Reconciliază selectiv codul comun și adaptoarele consumerilor în ambele direcții.
  Păstrează lucrul local, POM-urile reactorului, configurațiile și bazele Git independente.
  Promovarea unui fișier nu este o tranzacție atomică între workspace-uri.
- Rulează build-ul/testele numai în staging TMP propriu; păstrează acolo
  rapoartele/cache-ul/output-ul generate. Păstrează preview-ul implicit de 240 de
  caractere și aserțiunile componentelor/E2E auditabile și colapsabile.
- Păstrează documentația și feedback-ul ownerului aliniate cu codul și testele.
  Păstrează proveniența istorică; redenumirea unui director nu cere o versiune
  Reporting nouă și nu implică o execuție nouă a testelor.

- Limitează feedback-ul J0 activ la Core/Reporting/Console/Mock, Debug UI și Test Kit, inclusiv testele generice Reporting. J0 deține implementarea și guvernanța nativă Test Kit și Debug UI; consumerii binari folosesc perechile selectate în cataloagele lor, fără surse paralele. J1 este primul consumer Debug UI; nu deduce adopția de către J2–J7. J1 coordonează celelalte criterii comune și predările între consumeri; păstrează ownership-ul și acceptanța independentă. Rutele de compatibilitate pot rămâne în J0 fără TODO-uri duplicate.

- Păstrează README-urile și documentele centrate pe contracte curente. Reține numai nota curentă de verificare Reporting lângă pachetul producătorului în TMP propriu; înregistrează locația în proveniența curentă a consumerului. Nu acumula verificări Markdown per release în docs sau lib. Păstrează proveniența istorică a build-urilor/testelor.

- Documentația oficială descrie contracte curente, fără istoricul îmbunătățirilor Core/Reporting. Păstrează cronologia release-urilor, numerele execuțiilor datate și scorecard-urile în afara README-urilor/atlaselor/contractelor oficiale; dovezile curente rămân lângă bibliotecă. Raportează notele în conversație și păstrează lacunele concrete la owner.
- Identificatorii, comentariile, Javadocs/docstrings, numele testelor și mesajele tehnice scrise de dezvoltatori sunt în engleză. J0 deține scannerul comun din Test Kit și gate-ul reactorului; consumerii folosesc JAR-ul versionat, fără copii ale scannerului. Textele localizate ale produsului și valorile intenționat localizate în fixture-uri rămân date.
