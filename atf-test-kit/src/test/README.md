# Testele infrastructurii comune

## Pe scurt

J0 califică primitivele Kit separat de regulile aplicațiilor și de renderer. [Contractul Kit](../../README.md) deține API-ul; [strategia comună](../../../docs/ATF_Testing_Guide.md) delimitează nivelurile și responsabilitățile.

## Familii și oracole

| Familie | Proprietate verificată |
|---|---|
| Configurare și selecție | Descoperire, conversie și refuzul configurațiilor ambigue ori incomplete. |
| Scope și resurse | Proprietate pe clasă, suită sau invocare; inițializare și închidere controlate. |
| Aserțiuni și proiecție | Comparație evaluată o dată, redactare înainte de preview, valori integrale opționale și cauza originală păstrată. |
| Ciclul Jupiter | Izolarea invocărilor, faze și eșecuri observabile, inclusiv erori prinse de test. |
| Căi și timp | Limite de output și intervale măsurate prin ceasuri controlate. |
| Publicație | POM independent, namespace și CodeSource pe JAR, fără implementări paralele. |

## Execuție și limite

Într-o [copie TMP proprie](../../../docs/ATF_Build_And_Execution_Safety.md), JDK 21 și Maven rulează POM-ul fără parent. Clasele-fixture cu eșec intenționat sunt lansate de testele lor; nu se selectează direct.

- `KitUnitSuite` și `KitComponentSuite` selectează separat tag-urile unit și component prin engine-ul Jupiter, cu scope explicit de suită. Folosiți `mvn -Dtest=KitUnitSuite,KitComponentSuite test` în staging-ul J0; nativele nu se distribuie consumerilor.
- Unitățile califică politica locală; componentele ciclului folosesc Jupiter real. [Feedback-ul Kit](../../../docs/feedback/ATF_Common_Test_Support_Feedback.md) păstrează limitele concurenței și ale retenției; acceptanța binară este distinctă per consumer.
