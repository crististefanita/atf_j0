# Redactarea unui nume de aserțiune de joc

## Reproducere

Biblioteca Reporting 1.0.0-j18 a publicat un tabel nativ dintr-o componentă JVM. Eticheta capturată era `Safe exit always scores five basic Romanian points`.

Prima celulă din HTML a devenit `Safe exit always scores five basic ***REDACTED*** points`. Cititorul pierde numele profilului de scor, iar comparația ordonată dintre HTML și receipt refuză publicarea cu `Comparison name/order changed`.

## Criterii pentru bibliotecă

- Reproduceți în sursele deținute de bibliotecă, printr-o aserțiune cu acest nume și valori întregi.
- Păstrați proba pozitivă de mascarea unei credențiale Basic într-un context de autentificare. Folosiți exclusiv un token sintetic.
- Verificați separat o propoziție de business cu aceleași două cuvinte, un antet de autentificare și un secret configurat explicit.
- Verificați eticheta randată în DOM și lipsa tokenului sintetic din HTML, atât pentru rendererul principal, cât și pentru fallback.

## Rezolvare la producătorul dovezii

Componenta a fost reexecutată într-o copie a sursei înghețate cu eticheta `An exact zero bid earns five points under Romanian scoring`. Cele șase capturi ale metodei au proveniență de execuție nouă. Încercarea originală și HTML-ul respins au fost păstrate.
