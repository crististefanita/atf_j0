# ATF Core — fidelitatea datelor la boundary

## Pe scurt

Sunt trei criterii reproduse prin API-uri publice, cu valori sintetice și fără
serviciu extern. Privesc redactarea configurabilă, headerele explicite și
clasificarea MIME; transportul, mock-ul și rendererul au responsabilități separate.

## CORE-REDACTION-LITERAL-66 — masca rămâne text literal

**Prioritate: 76/100** = impact 30 + lacună 23 + lucrabil 17 + documentare 6.
**Owner:** Core security. **Nivel:** unit, apoi adaptorul care consumă politica.

| Trigger și observație | Expected independent |
|---|---|
| `RedactionSettings.replacement="$0"`; textul `token=ATF_SYNTHETIC_SECRET` păstrează valoarea prin backreference | masca este literală; rezultatul este `token=$0` |
| PII activ și aceeași mască; emailul sintetic rămâne nemascat | întregul match devine `$0` |
| masca `$MASK` aruncă `IllegalArgumentException` în strategia regex | orice mască acceptată de configurație se aplică fără interpretare regex |

Sursa este la [strategia text](../../atf-core/src/main/java/atf/core/security/SecretPatternRedactor.java),
[PII](../../atf-core/src/main/java/atf/core/security/PiiRedactor.java) și
[căi locale](../../atf-core/src/main/java/atf/core/security/LocalPathRedactor.java).
Masca implicită trece controlul nominal; reproducerea nu declară un leak în rapoartele implicite Shop/AutoEx.

- [ ] Tratați literal doar masca configurată; păstrați grupurile structurale ale regulilor existente.
- [ ] Probați `$0`, `$1`, `$MASK`, backslash și combinații, în text, PII, căi și fallback JSON; valorile originale rămân intacte.
- [ ] Păstrați semantica `enabled`, PII opt-in, câmpurile sensibile și limita de preview a callerului; fără a reduce redactarea pentru a evita excepția.

**Închidere:** [probele de redactare](../../atf-core/src/test/java/atf/core/security/RedactionStrategiesTest.java)
refuză păstrarea canary-ului și verifică exact masca, inclusiv diferența dintre JSON valid și fallback.

## CORE-MEDIA-TYPE-69 — JSON se decide din media type

**Prioritate: 68/100** = impact 27 + lacună 18 + lucrabil 17 + documentare 6.
**Owner:** Core pentru clasificare; Mock pentru efectul asupra matching-ului.
**Nivel:** unit și componentă Mock.

`MediaTypes.isJson("text/plain; profile=json")` întoarce `true`.
Cu body `{}`, o rută Mock POST obișnuită acceptă request-ul, deși tipul principal este text/plain.

- [ ] Clasificați tipul/subtipul, separat de parametri; păstrați JSON standard, sufixele `+json`, case-insensitivity și charset-ul valid.
- [ ] Refuzați `json` doar în parametri ori într-un subtip fără semantica JSON; verificați și modul Mock `WRONG_CONTENT_TYPE`.
- [ ] Păstrați body-ul JSON valid și invalid ca axe distincte; un MIME corect nu repară un payload invalid.

**Închidere:** [MediaTypes](../../atf-core/src/main/java/atf/core/http/request/MediaTypes.java)
și [MockRoute](../../atf-mock/src/main/java/atf/mock/MockRoute.java) au controale pozitive/negative independente;
ruta aleasă și verdictul nu depind de un substring din parametri.

## CORE-EXPLICIT-HEADERS-68 — precedență egală în transporturile live

**Prioritate: 65/100** = impact 25 + lacună 18 + lucrabil 17 + documentare 5.
**Owner:** Core HTTP. **Nivel:** unit pentru request-ul construit, apoi loopback pentru ambele transporturi.

Un POST cu `content-type: application/x-www-form-urlencoded` ajunge în
`HttpClient.sendAsync` cu două valori Content-Type: JSON și form-urlencoded.
[HttpClientTransport](../../atf-core/src/main/java/atf/core/http/transport/HttpClientTransport.java)
adaugă headerele, iar [UrlConnectionTransport](../../atf-core/src/main/java/atf/core/http/transport/UrlConnectionTransport.java)
le înlocuiește. Proba înregistrează request-ul real construit și exact un send, fără rețea externă.

- [ ] Definiți precedența case-insensitive a headerului explicit față de default; păstrați Accept când nu este suprascris.
- [ ] Verificați valorile complete văzute la boundary-ul transportului, inclusiv nume cu casing diferit și JSON fără override.
- [ ] Păstrați autentificarea și o singură expediere; nu introduceți replay pentru a compensa MIME-ul greșit.

**Închidere:** ambele transporturi livrează același header efectiv pentru același request;
proba nu verifică numai map-ul inițial `ApiRequest.headers()`.

## Proces

Atlasul [pachetului security](../../atf-core/src/main/java/atf/core/security/README.md)
delimitează redactarea de preview. Schimbările și probele se execută prin
[procesul izolat](../ATF_Build_And_Execution_Safety.md); controalele testelor rămân în
[baseline](../ATF_Test_Changes_Baseline.md).
