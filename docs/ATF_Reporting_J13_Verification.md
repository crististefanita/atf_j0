# Reporting J13 — memorie la publicare și ACTION/OBSERVE integrale

## Pe scurt

J0 deține implementarea și regresiile; J1–J4 primesc aceeași pereche thin JAR/POM
direct în lib. Core C3 rămâne identic; sursa și harness-ul Reporting rămân numai în J0.

| Identitate | Valoare |
|---|---|
| Reporting | atf.api:atf-reporting:1.0.0-j13 |
| SHA-256 JAR | 9B501223A60EA8C3F4B44B74DC0336F101770A7C63A35DE8DB889C49B302A367 |
| SHA-256 POM | 736A6860926978310BC3E2DEF3E8C2ADC6A0C0B69153D140BF9F048CE295BA74 |
| Core păstrat | C3: 605407B9D40E67C9C352EE6E94DA758E50BD3CFB14CC0D7A0DE3359C71088447 |
| Build | Java 21; două staging-uri parentless noi, bytes identici |
| API J12 → J13 | zero simboluri public/protected eliminate; UiPhase adaugă observation și supportsObservations |

## Cauză și corecție

- J12 eșuează la heap 1 GiB pe modelul real sintetic: 321 checkpoint-uri,
6.634 comparații, 577 ACTION/OBSERVE și 77.808.474 bytes PNG sursă. 
- Înlocuirile
întregului HTML propagă repetat payload-urile Base64 prin copii UTF-16.

- J13 păstrează PNG-urile mari pe disc în staging-ul privat, transformă structura
compactă și restaurează payload-urile integral prin buffere fixe, înaintea
înlocuirii atomice a artifactului. 
- Deduplicarea rămâne pe identitatea capturii.

- Expected/observed ACTION și OBSERVE aveau numai display(..., settings), deci
coada se pierdea chiar și cu body limit zero. 
- Acum celula păstrează preview-ul
240 și valoarea completă redactată, colapsabilă. 
- SUMMARY_ONLY rămâne explicit.

- UiPhase deleagă observations fazei neutre, fără request-uri ori assertions inventate.

## Probe și limite

- Native standalone: 87/87 în fiecare build; patru probe noi de round-trip, buffer boundary, writer failure și spool trunchiat.
- Harness pe sursă: 327/327 în 19 clase afectate, inclusiv timing, failure paths și screenshot policy.
- Proba de volum verifică hash-ul fiecărei capturi, ordinea și ownerul original, toate comparațiile și ACTION/OBSERVE; eliminarea numai a ultimului rând este refuzată.
- Celulele complete sunt verificate în Extent/fallback pentru 239/240/241, două scenarii omonime la preview, markup, newline, Unicode, secret redactat și suffix eliminat.
- Browserul verifică Enter/Space, End, vecinul închis, panou îngust și absența suprapunerii summary/conținut.
- Cele două profile anterioare la 384 MiB păstrează plafonul observat 345 MiB; bugetele assets nu sunt mărite.

Măsurătorile sunt observații pe fixture, nu un plafon universal pentru orice
raport. Modelul Extent/Spark și textul continuă să consume heap proporțional
cu inputul; durata close include flush și publicarea, nu execuția aplicației.
PNG-urile sintetice nu certifică dispozitivul, brokerul, ASR ori business-ul Shop/AutoEx.

Dovezi TMP: ATF_REPORTING_J13_ACTIVE_DO_NOT_CLEAN_WvBK4t — logs, XML,
reporting-binary-api.json, snapshot-uri, rapoarte sursă/JAR și promovare guarded.

## Criteriu distinct rămas

Valorile lungi ale assertions UI au boundary separat: callerul poate fi redus
textul înainte de Reporting. J13 nu reconstruiește informația absentă și nu
închide acel criteriu prin celulele ACTION/OBSERVE. Ownerul și acceptanța sunt
în [feedback-ul generic](feedback/ATF_Android_UI_Reporting_Feedback.md).

## Măsurători pe modelul de volum

| Execuție | Durată close | Vârf heap eșantionat | Alocări în close | HTML |
|---|---:|---:|---:|---:|
| sursă | 31.726 ms | 1.062.403.464 bytes | 1.245.840.296 bytes | 108.133.052 bytes |
| JAR fizic J13 | 15.773 ms | 1.069.639.760 bytes | 1.244.388.776 bytes | 108.133.052 bytes |

- Ambele procese folosesc -Xmx1g; diferențele de timp/GC includ încărcarea concurentă a hostului.
- Vârful eșantionat la 2 ms nu este un plafon universal; include obiecte încă necolectate, iar raportul rămâne mare.
- Harness JAR: 332/332, inclusiv cele patru native ale spool-ului și controlul CodeSource Core C3/Reporting J13.
- Snapshot-ul producătorului are 412 intrări și 17 fișiere native; documentele ownerului sunt validate separat după completarea livrării.

## Audit TAB

- Extent și fallback: celulă integrală prin Enter/Space, End până la suffix, redacție vizibilă și vecin închis.
- Raportul mare JAR: Fails → Open in Tests ajunge la Comparison 320/19 FAIL, cu vecini PASS; ultimul ACTION și PNG-ul său sunt vizibile.
- Captura finală se deschide în viewer, iar Escape revine la același checkpoint.
- Snapshot-ul MCP integral depășește limita de frame; auditul mare folosește capturi focalizate ale UI. Această limită tehnică nu este confundată cu un PASS DOM integral.

## Intrare istorică din J4

| Dovadă J12 a consumerului | Inventar păstrat separat |
|---|---|
| raport real cu heap 3 GB | 325 checkpoint-uri PNG, 77.578.039 bytes PNG, 6.655 assertions, 581 ACTION/OBSERVE |
| HTML | 107.332.432 bytes, 307 URI-uri PNG și 305 hash-uri distincte |
| SHA-256 HTML | 2458bd209cb7b1e46aebfd18a58351efffdb9628f7ad7e746c4892ad388b16fe |
| receipt extern | J4_ACCEPTANCE_ROOT/accepted-report-verification.properties și publisher-verification.xml |
| versiune capturată | Core C2/Reporting J12; adopția C3 și J13 au probe separate |

Cele 86,683 s ale probei J4 includ publicarea și verificarea, fără o măsurare separată de peak heap.
Acest istoric nu este relabelat ca execuție J13 și nu substituie acceptanța device/business.

## Consolidarea probelor

- Proba browser mutată sub ownerul UI al harness-ului trece izolat pe sursă și JAR, fără creșterea timeout-ului de 5 s.
- O rulare sursă concurentă a depășit readiness-ul de 5 s; log-ul este păstrat și nu intră în acceptanță.
- Catalogul finit de native include noul fișier în J0/J1/J2; fingerprint-ul importului J2 are reconciliere explicită, fără prag relaxat.
- SHA-256 per captură se calculează pe Base64 ASCII complet înainte și după publicare; egalitatea conservă și bytes PNG decodați.

## Harness la heap implicit și regresii adiacente

- Oracolul citește incremental URI-urile PNG și păstrează SHA-256 la ownerul DOM, fără implementarea spool-ului de producție.
- CapturedVolumePublicationComponentTest trece cu parent 384 MiB și child 1 GiB pe sursă/JAR; mutantul ultimului PNG și ultimului rând este refuzat.
- OOM-ul anterior din oracol la citirea integrală a HTML-ului rămâne în log-ul default-heap-volume-source; raportul child fusese publicat complet.
- BoundedFailureEvidenceComponentTest păstrează canarul de secret global și plafonul assertions; numai disclosure-ul ACTION original poate deține scalarul complet.
- Cele 71 de cazuri adiacente trec separat pe sursă și JAR: preview YAML 18/240/480, markup, screenshot, failure context și CSS.

## Acceptanța consumerilor

| Workspace | Probă executată |
|---|---|
| J0 | 156/156 guvernanță completă; native, harness și browser verificate separat |
| J1 | 156/156 guvernanță; Shop 22 și AutoEx 9 cazuri componentă pe JAR |
| J2 | 170/170 guvernanță; 7 SQL și 9 Eventuate MOCK, cu profile explicite |
| J3 | 43/43, inclusiv guvernanță și PCM fixture; fără device/ASR live |
| J4 | 2/2 contracte Core/Reporting, recompilate după refresh live; fără certificare device/business |

Controalele demonstrate sunt în baseline; cerința generică pentru valorile lungi
ale assertions rămâne distinctă în backlog-ul sursei J0.

## Completare live J4 înaintea adopției J13

| Execuție istorică Core C3 / Reporting J12 | Dovadă |
|---|---|
| artifact | 41.278.016 bytes; SHA-256 342e0da369e5551c25ded7beb6abc961793bfcaec5fff77eccf58eef47bbd70b |
| dovezi capturate | 287 checkpoint-uri PNG, 180 URI-uri PNG, 178 hash-uri distincte, 11.047 assertions și 405 ACTION/OBSERVE |
| populație separată | 233 cazuri JUnit/Gradle; mutantul rămâne observație neutră cu erori așteptate |
| receipt extern | J4_ROMANIAN_ACCEPTANCE_ROOT/accepted-report-verification.properties; publisher-verification.xml și executed-library-classpath.json fixează binarele |
| limită | publisher 53,438 s cu heap 3 GB, incluzând verificarea; peak heap și latența exclusivă nu sunt măsurate |

Acest succes J12 nu a închis OOM-ul de 107 MB sau celulele generice incomplete.
J13 are probele neutre sursă/JAR și auditul TAB proprii; nu relabelează execuția
Android și nu substituie verificarea vizuală a raportului istoric J4.
