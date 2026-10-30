# UI network — captură, identitate și eliberarea listenerelor

## Pe scurt

Pachetul observă traficul browserului și păstrează un jurnal bounded. Nu trimite
request-uri, nu atribuie verdict business și nu publică noduri Extent.
Sesiunea de browser deține pagina; inspectorul deține propriile listener-e.

## Intrări și responsabilități

| Când investighezi | Citește întâi | Limita |
|---|---|---|
| atașare, captură și close | [UiNetworkInspector](UiNetworkInspector.java) | coordonare, rezervare, retenție și livrare |
| request pornit înainte de marker | [UiRequestTimeline](identity/UiRequestTimeline.java) | owner direct al hook-ului de start; identitate scoped |
| valoare de identitate | [UiRequestIdentity](identity/UiRequestIdentity.java) | ordine în același scope; distinctă de receipt sequence |
| date păstrate în jurnal | [UiNetworkObservation](UiNetworkObservation.java) | snapshot și sanitizare conform configurației |
| trafic acceptat și capacitate | [UiNetworkCapturePolicy](UiNetworkCapturePolicy.java) | politică de captură, nu criteriu business |
| compactare și repetări | [UiNetworkInspector.presentationObservations](UiNetworkInspector.java) | vedere de prezentare; jurnalul raw păstrează și răspunsul ulterior eșuat |

## Invariante de păstrat

- Captura este refuzată în timpul atașării incomplete și după close, înainte de
  dereferențierea unui Response expirat. Închiderea este logică și ireversibilă.
- Timeline-ul compensează propria înscriere eșuată și când este folosit direct.
  Fiecare owner încearcă toate eliminările sale, păstrând cauza primară și
  cleanup-urile distincte; nu adaugă aceeași excepție ca suppressed sieși.
- Callback-urile native rămase după un refuz de dezabonare sunt inerte.
  Close nu anulează snapshot-ul subscriberilor unei livrări deja începute.
- Ordinea pornirii request-ului nu este ordinea sosirii răspunsului. Consumerul
  decide apartenența business; jurnalul păstrat rămâne inspectabil după close.
- Valorile query sunt eliminate din URL. Înaintea redactării, captura calculează
  o amprentă SHA-256 opacă pentru identitate; resanitizarea o păstrează fără
  recuperarea valorilor. Inputul legacy deja redactat, fără amprentă, rămâne necunoscut.
  Adapterul de raportare nu publică digestul și nu tratează placeholder-ul drept query.

## Probe înainte de schimbare

| Contract | Probă de pornire |
|---|---|
| achiziție și cleanup, inclusiv Error/reentranță | [UiNetworkLifecycleComponentTest](../../../../../test/java/atf/ui/network/lifecycle/UiNetworkLifecycleComponentTest.java) |
| identitatea scoped | [UiRequestIdentityComponentTest](../../../../../test/java/atf/ui/network/identity/UiRequestIdentityComponentTest.java) |
| livrarea subscriberilor | [UiNetworkDeliveryComponentTest](../../../../../test/java/atf/ui/network/delivery/UiNetworkDeliveryComponentTest.java) |
| identitate fără valori query expuse | [UiObservedQueryFingerprintComponentTest](../../../../../test/java/atf/ui/network/UiObservedQueryFingerprintComponentTest.java) |
| raw lossless și compactare configurabilă | [UiNetworkPresentationStreamComponentTest](../../../../../test/java/atf/ui/network/UiNetworkPresentationStreamComponentTest.java) |

Condițiile de retenție și exemplele consumer sunt în [contractul modulului](../../../../../../README.md).

Continuă orientarea în [atlasul de context ATF](../../../../../../../docs/ATF_Context_Atlas.md).
