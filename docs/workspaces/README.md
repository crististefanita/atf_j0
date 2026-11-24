<a id="atf-java--executive-overview"></a>
<a id="atf-java--prezentare-executivă"></a>
# ATF Java — workspace index

## At a glance

J0 owns the source of Core, Reporting, Console, Mock, Test Kit and Debug UI. This index connects each producer or consumer to its project documentation; implementation details and acceptance remain with their owners.

| Workspace | Scope | Entry point |
|---|---|---|
| J0 | 12 source modules; Core, Reporting, Console, Mock, Test Kit and Debug UI ownership | [Source atlas](../ATF_Context_Atlas.md) |
| J1 | 5 local modules; Shop/AutoEx scenarios, six library pairs and first Debug UI consumption | [Project documentation](https://github.com/crististefanita/atf_j1/blob/main/docs/README.md) |
| J2 · WIP | 5 modules; Kafka/Eventuate queues and messaging, without UI/Debug UI | [Project documentation](https://github.com/crististefanita/atf_j2/blob/HEAD/docs/ATF_Context_Atlas.md) |
| J3 | 4 modules; English speech transcription | [Project documentation](https://github.com/crististefanita/atf_j3/blob/main/docs/ATF_Speech_Architecture.md) |
| J4 | 3 modules; Whist Lab and Android testing | [Project documentation](https://github.com/crististefanita/whist/blob/main/docs/README.md) |
| J5 | 2 modules; Rentz Lab and offline tests | [Project documentation](https://github.com/crististefanita/rentz/blob/main/README.md) |
| J6 | 5 modules; Hold’em Lab | [Project documentation](https://github.com/crististefanita/atf_j6/blob/main/README.md) |
| J7 | 1 Maven module; Chess Lab and ATF library consumption | [Project documentation](https://github.com/crististefanita/chess/blob/main/README.md) |

## Ownership and limits

- Library implementations and native tests remain in J0; each consumer owns its adapters, configuration and application expectations.
- Module counts describe reactor structure, not test coverage or acceptance. A build or mock result does not certify an external service, broker, device or speech model.
- Use the project’s current documentation for its scope; no delivery history or run receipts are collected here.

## Related contracts

- [Library maintenance and feedback](../../lib/README.md)
- [Workspace synchronization procedure](../ATF_Workspace_Synchronization.md)
- [J0 project overview](../../README.md)
