# Ghiduri de consum — acceptanță executabilă

## Pe scurt

J0 deține ghidurile Core, Reporting, Console și Mock și regresiile lor generice. Cerințele comune sunt păstrate aici; consumerii verifică integrarea proprie.

## LIB-GUIDE-EXEC — P2

- [ ] Compilați permanent exemplele din ghiduri pe perechile JAR/POM distribuite, fără surse de producție paralele; o metodă retrasă sau o dependență lipsă trebuie să producă FAIL.
- [ ] Verificați comportamentele promise: JSON strict, catalogul și fallback-ul Mock, configurația Console și publicarea Reporting cu aserțiuni PASS/FAIL/null.
- [ ] Păstrați selecția repetabilă la ownerii probelor native; Reporting își păstrează regresiile în atf-reporting-tests, fără cuplarea celorlalte biblioteci la renderer.

Criteriul cere integrare permanentă în verificările proiectului. Probele ad-hoc din TMP sunt dovezi de investigație; nu închid singure cerința.

[Ghiduri canonice](../../lib/README.md) · [Registrul bibliotecilor](README.md#biblioteci-comune)
