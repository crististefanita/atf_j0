# Redactarea unui nume de aserțiune de joc

## Martor și stare curentă

Biblioteca Reporting 1.0.0-j18 a publicat un tabel nativ dintr-o componentă JVM. Eticheta capturată era `Safe exit always scores five basic Romanian points`.

Prima celulă din HTML a devenit `Safe exit always scores five basic ***REDACTED*** points`. Cititorul pierde numele profilului de scor, iar comparația ordonată dintre HTML și receipt refuză publicarea cu `Comparison name/order changed`.

**P2 · owner Reporting.** În codul live, [regula textuală](../../atf-reporting/src/main/java/atf/reporting/security/SensitiveDataRedactor.java) asociază în continuare `basic` cu următorul cuvânt și în text liber. Extent și fallback aplică redactorul etichetei aserțiunii. Inspecția sursei indică aceeași cale de fals pozitiv ca în martorul J18. Calificarea pe JAR-ul curent rămâne deschisă.

## Criterii pentru bibliotecă

- Reproduceți în sursele deținute de bibliotecă, printr-o aserțiune cu acest nume și valori întregi.
- Păstrați proba pozitivă de mascarea unei credențiale Basic într-un context de autentificare. Folosiți exclusiv un token sintetic.
- Verificați separat o propoziție de business cu aceleași două cuvinte, un antet de autentificare și un secret configurat explicit.
- Verificați eticheta randată în DOM și lipsa tokenului sintetic din HTML, atât pentru rendererul principal, cât și pentru fallback.

## Adaptarea locală a martorului

Componenta a fost reexecutată într-o copie a sursei înghețate cu eticheta `An exact zero bid earns five points under Romanian scoring`. Cele șase capturi ale metodei au proveniență de execuție nouă. Încercarea originală și HTML-ul respins au fost păstrate.
