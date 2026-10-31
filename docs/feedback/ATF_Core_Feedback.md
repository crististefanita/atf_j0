# Core — validarea documentului JSON complet

## Pe scurt

Core deține validarea HTTP comună și nativele sale. Mock J2 are un guard
strict propriu, dar această protecție nu schimbă binarul C1 sau validarea
request/response din ceilalți consumeri.

## CORE-JSON-DOCUMENT-103 — prioritate 92

**Prioritate:** 92/100 = impact 38 + lacună 27 + lucrabil 19 + documentare 8.
[JsonBodyPolicy](../../atf-core/src/main/java/atf/core/http/response/JsonBodyPolicy.java)
parsează primul arbore fără verificarea finalului documentului; sursa live și
JAR-ul C1 au aceeași limită.

| Probă publică pe C1 | Actual | Cerință |
|---|---|---|
| validDocument pentru obiect urmat de trailing content | true | false |
| validDocument pentru două obiecte consecutive | true | false |
| validateWriteExchange cu primul request de mai sus și response valid | listă goală | încălcare explicită pentru request |
| chei duplicate în obiect | acceptate | politică de ambiguitate explicită, aliniată cu Mock și Validation |

- Reutilizați o primitivă pentru document complet la ownerul Core, cu reader imuabil și fără cache global de arbori; consumerii nu primesc surse Core shadow.
- Adăugați native pentru document gol, trailing content, documente multiple, duplicate nested și scalari, inclusiv null; separați sintaxa de politica write care interzice null.
- Verificați ambele laturi ale schimbului și refuzul request-ului invalid înainte de dispatch; păstrați cauza, attempts și zero replay pentru mutații.
- Reconcilează Mock și Validation selectiv după stabilirea contractului comun; nu introduceți dependență Mock către Validation ori Reporting pentru accesul la parser.
- Publicați un candidat Core nou cu JAR/POM și proveniență; rulați nativele pe sursă și binar și acceptanțe separate pentru deployment-urile afectate, fără înlocuirea bytes sub versiunea C1 existentă.

**Închidere:** cele trei contraexemple nu mai sunt acceptate, politica pentru
duplicate și null este documentată și probele native refuză implementarea veche.
Un Mock verde nu certifică automat fluxul HTTP real al consumerului.

<details>
<summary><strong>Dovadă J2 pe binarul fizic, fără modificarea Core</strong></summary>

- Increment TMP atf-j2-mock-json-615a89c6ad, mock-matrix/core-policy-observation.json.
- validDocument pentru trailing content și al doilea document este true; writeExchangeViolations este listă goală.
- [Guard-ul candidat Mock J2](../../../atf_j2/atf-mock/README.md) aplică strictețea local; nu este livrare Core ori acceptanță J0/J1/J3.

</details>
