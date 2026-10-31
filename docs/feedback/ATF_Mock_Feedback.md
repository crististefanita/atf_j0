# Mock — reconcilierea guard-ului JSON și precizia matching-ului

## Pe scurt

Mock comun rămâne la ownerul J0; candidatul J2 este verificat în TMP.
Reconcilierea păstrează refactorizările live și acceptanțe separate, fără
copierea reactorului ori a harness-ului Reporting în consumeri.

## MOCK-STRICT-FIXTURE-85 — prioritate 85

Expected-ul și răspunsurile POST/PUT din MockRoute acceptă primul arbore JSON,
iar expected gol este convertit în absența constrângerii. În loader, un obiect
sau array YAML pentru request-body poate elimina aceeași constrângere.

- Reconcilează selectiv MockJsonDocument, MockRoute și guard-ul optionalText din [candidatul J2](../../../atf_j2/atf-mock/README.md); păstrează semantica rutelor, query, wildcard, MIME și fixture-urile raw de citire.
- Adaugă cele două componente native MockJsonFixtureComponentTest și MockJsonCatalogComponentTest la owner; fixture invalid refuzat înainte de dispatch, request invalid trimis la fallback, nominale structurale și moduri negative.
- Păstrează cauza parserului și textul response-ului; un expected absent sau YAML null nu este șirul gol ori documentul JSON null quoted.
- Regresia de publicare rămâne neutră și separată în Reporting Tests; acceptanța consumerului J2 nu trebuie să devină implementare a rendererului în Mock.

**Închidere:** documente complete și neambigue pe cele trei suprafețe, fără
coerciția YAML în expected absent; sursa comună și consumerii afectați au probe
proprii. Cele 62 de regresii noi J2 nu certifică implicit J0/J1/J3.

## MOCK-NUMERIC-MATCH-104 — prioritate 86

Pe candidatul J2, expected amount 9007199254740992.0 și request amount
9007199254740993.0 se potrivesc; Double pierde diferența înainte de equals.
Acesta este un contract separat de sintaxa JSON și păstrează propriul criteriu.

- Stabiliți egalitatea numerică fără narrowing și probați limite, fracții, exponent, zero și compatibilitatea tipurilor string/număr.
- Componenta publică trebuie să arate fallback pentru valoarea distinctă, nominale independente și wire body păstrat; nu schimbați expected-ul după observație.

**Închidere:** contraexemplul nu mai selectează ruta și nominalele sunt
demonstrate în deployment-urile reconciliate. Dovada curentă este în TMP,
incrementul atf-j2-mock-json-615a89c6ad, mock-numeric-observation.log.

[Core JSON](ATF_Core_Feedback.md) are owner și release distinct; un parser
strict local nu închide criteriul bibliotecii C1.
