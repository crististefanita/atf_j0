<a id="large-report-navigation--retained-evidence-incremental-work"></a>
<a id="navigarea-rapoartelor-mari--dovezi-păstrate-lucru-incremental"></a>
# Navigarea rapoartelor mari — dovezi păstrate, lucru incremental

## Contract

Rapoartele HTML mari păstrează toate comparațiile furnizate și fiecare PNG original inclus în raport. Navigarea se schimbă atunci când sunt pregătite metadatele de layout și timp, nu lista dovezilor publicate și nici evaluarea verdictului testului.

| Responsabilitate | Invariantă |
|---|---|
| celule de aserțiuni | preview-ul nativ implicit rămâne 240; verdictele capturate separat și corpurile complete furnizate rămân neschimbate |
| tabele ascunse | nu se creează wrapper-e, măsurători intrinseci sau abonări la redimensionare înainte de afișare; vizualizările cunoscute ca ascunse sar și peste citirile geometriei |
| schimbări de layout | rădăcinile afectate se grupează o singură dată per cadru de animație; descendenții unui strămoș deja programat nu declanșează o nouă scanare a subarborilor |
| dialoguri cu capturi | interfața adăugată fără legătură nu rescanează fiecare tabel din document; octeții PNG originali, descrierile locale și contextul de revenire rămân intacte |
| timp | markerul original de execuție al unui card se rezolvă o dată per trecere de agregare; cache-ul se resetează la trecerea următoare, inclusiv când markerul lipsește |
| vizualizări proiectate | indexul sursă-owner/scenariu se capturează o dată per trecere de proiecție și se partajează între Category, Fails și Overview |
| view selection | Tests, Category, Fails și Overview sincronizează panoul vizibil, singurul link curent și fragmentul URL, inclusiv după rescrierea linkurilor de către Spark nativ |
| conținut dinamic | scrierile identice pentru class/open sunt ignorate; mutațiile reale se grupează înaintea invalidării conținutului; schimbările reale de copii/text măresc revizia conținutului; tabelele noi/redeschise/redimensionate primesc alocarea nativă a coloanelor; tabelele eliminate își retrag observarea redimensionării |
| scope-ul rendererului | alocatorul de tabele este partajat de Extent și fallback; agregarea duratelor aparține Extent |

Raportul rămâne autonom. Această schimbare nu mută dovezile într-un alt fișier, nu elimină ownerii prin virtualizare, nu șterge capturi și nu înlocuiește rândurile de aserțiuni cu observații.

<a id="permanent-verification"></a>
<a id="verificare-permanentă"></a>
## Verificare permanentă

| Responsabil | Dovadă |
|---|---|
| LargeNavigationFixture | patru owneri originali, 600 de grupuri de comparație, 12.004 rânduri de comparație capturate și patru checkpoint-uri PNG sintetice complete |
| LargeReportNavigationBrowserComponentTest | comutări repetate între owneri, căi vizate pentru aserțiuni/PNG și revenire; extindere în masă pe fixture-ul neutru sau pentru cel mult doi owneri externi selectați explicit; inventar SHA-256 per owner pentru celulele/corpurile originale și payload-urile/referințele PNG canonice, înainte/după; se refuză o mutație de un singur caracter a payload-ului |
| EvidenceTableVisibilityComponentTest | scrierile identice de prezentare nu declanșează remăsurare; schimbările reale ale stilului celulei o fac; vizualizările cunoscute ca ascunse sar peste geometrie; dovezile închise rămân structural intacte; deschiderea/redeschiderea și mutația directă a nodului text primesc coloane măsurate complet |
| NavigationSelectionBrowserComponentTest | Chromium verifică cele patru tranziții și singurul link curent accesibil. Fixture-ul distribuției verifică cele patru ținte semantice și listenerul fragmentelor pe HTML din JAR fizic. |
| RecordedPublicationTimingComponentTest | execuția și transportul rămân independente de publicare; markerii eliminați/înlocuiți se actualizează la următoarea agregare |
| familii de browser învecinate | tabele standard UI/API, layout fallback, stare a disclosure-ului, etichete sursă și contractele viewport/revenire pentru captura integrală |

- Proprietatea opțională `atf.navigation.input` folosește implicit căi vizate.
- `atf.navigation.expandOwners` acceptă explicit cel mult doi indici owner
  distincți, numerotați de la zero; indecșii duplicați, negativi/în afara intervalului și cererile nelimitate sunt refuzate.
- Fiecare owner selectat se extinde o singură dată, cu același inventar canonic
  de dovezi înainte și după.
- Aceasta verifică publicarea/randarea, nu o execuție business sau pe dispozitiv
  proaspătă.

<a id="measurement-and-boundaries"></a>
<a id="măsurare-și-limite"></a>
## Măsurare și limite

- Ceasurile pentru readiness-ul browserului, interacțiunea cu ownerul și
  publicare au scope-uri diferite.
- Trecerea contractului capturilor nu certifică o execuție Android nouă și nici
  oracolul ei business.
- Dovezile obligatorii foarte mari consumă în continuare memorie pentru parser,
  layout și imagini; bugetul de conținut nu garantează o limită pentru întregul HTML sau heap-ul browserului.
- Observațiile de performanță nu transformă niciodată un FAIL capturat în PASS.

Producătorul deține regresiile permanente sursă/JAR. Proveniența consumerului înregistrează binarul curent și acceptanța independentă; randarea Android arhivată nu certifică o rulare nouă pe dispozitiv.

<a id="cold-startup-and-readiness"></a>
<a id="pornire-la-rece-și-readiness"></a>
## Pornire la rece și readiness

| Lifecycle | Comportament cerut |
|---|---|
| DOMContentLoaded/load | inițializează o singură dată și activează numai ținta hash solicitată; un hash gol deschide Overview |
| încărcarea nativă Spark | reconciliază cele patru href-uri de navigare după rescrierea lor de către vendor; nu reactivează și nu repornește ceasul readiness |
| readiness | rulează normalizarea structurală inițială într-un task agregat chiar dacă frame-urile vizuale sunt amânate; publică numai după succes |
| încărcarea paginii | înregistrează separat timpul de navigare încheiat; o încărcare nefinalizată rămâne necunoscută, nu zero |
| conținut dinamic | păstrează observerii, invalidarea reviziei și normalizarea programată pentru mutațiile ulterioare |
| deadline-uri | readiness/acțiune 10.000 ms; navigare 45.000 ms; matricile API existente păstrează 5.000 ms |

[ColdStartReadinessComponentTest](../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/navigation/ColdStartReadinessComponentTest.java) verifică cinci destinații inițiale, deep-link-uri de scenariu la startup, repararea linkurilor native și load repetat. Confirmă că readiness așteaptă normalizarea structurală reală și că Tests funcționează cu frame-urile de animație suspendate. Un atf.cold.input extern rulează trei procese Chromium noi pe același artefact complet.

<a id="native-timeline-labels"></a>
<a id="etichete-native-pentru-timeline"></a>
## Etichete native pentru timeline

| Limită | Invariantă |
|---|---|
| șablon Spark | codifică cheia owner-ului timeline completă ca șir JSON; păstrează ghilimelele, întreruperile de linie, backslash-urile și Unicode |
| date capturate | păstrează exact eticheta callerului și durata originală; fără tăiere sau verdict dedus |
| upgrade de vendor | eșuează explicit când se schimbă expresia cheii native fixate, în loc să publice în tăcere un script invalid |
| dovadă permanentă | TimelineTitleEncodingComponentTest execută scriptul încorporat real și refuză erorile browserului; regresiile timestamp-ului rămân separate |

Producătorul deține implementarea și testele permanente sursă/JAR. Consumerii își păstrează oracolele independente; martorii externi datați nu devin acceptanță nouă.
