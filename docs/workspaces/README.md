<a id="atf-java--executive-overview"></a>
<a id="atf-java--prezentare-executivă"></a>
# ATF Java — indexul workspace-urilor

## Pe scurt

J0 deține sursa Core, Reporting, Console, Mock și Test Kit. Acest index conectează fiecare producător sau consumer la documentația proiectului său; detaliile implementării și acceptanța rămân la owneri.

| Workspace | Domeniu | Punct de intrare |
|---|---|---|
| J0 | 12 module sursă; ownership Core, Reporting, Console, Mock și Test Kit | [Atlasul sursei](../ATF_Context_Atlas.md) |
| J1 | 6 module; scenarii Shop/AutoEx și consumul bibliotecilor comune | [Documentația proiectului](https://github.com/crististefanita/atf_j1/blob/HEAD/docs/README.md) |
| J2 · WIP | 5 module; cozi și mesaje Kafka/Eventuate, fără UI/Debug UI | [Documentația proiectului](https://github.com/crististefanita/atf_j2/blob/HEAD/docs/ATF_Context_Atlas.md) |
| J3 | 4 module; transcriere audio în engleză | [Documentația proiectului](https://github.com/crististefanita/atf_j3/blob/HEAD/docs/ATF_Speech_Architecture.md) |
| J4 | 3 module; Whist Lab și testare Android | [Documentația proiectului](https://github.com/crististefanita/whist/blob/HEAD/docs/README.md) |
| J5 | 2 module; Rentz Lab și teste offline | [Documentația proiectului](https://github.com/crististefanita/rentz/blob/HEAD/README.md) |
| J6 | 5 module; Hold’em Lab | [Documentația proiectului](https://github.com/crististefanita/atf_j6/blob/HEAD/README.md) |
| J7 | 1 modul Maven; Chess Lab și consumul bibliotecilor ATF | [Documentația proiectului](https://github.com/crististefanita/chess/blob/HEAD/README.md) |

## Ownership și limite

- Implementările bibliotecilor și testele native rămân în J0; fiecare consumer își deține adaptoarele, configurația și așteptările aplicației.
- Numărul modulelor descrie structura reactorului, nu acoperirea testelor sau acceptanța. Un rezultat de build sau mock nu certifică un serviciu extern, broker, dispozitiv sau model speech.
- Consultă documentația curentă a proiectului pentru domeniul său; aici nu se colectează istorice de livrare sau dovezi ale rulărilor.

## Contracte conexe

- [Mentenanța bibliotecilor și feedback](../../lib/README.md)
- [Procedura de sincronizare a workspace-urilor](../ATF_Workspace_Synchronization.md)
- [Prezentarea proiectului J0](../../README.md)
