# Documentație ATF — fidelitatea deployment-urilor

## Pe scurt

Atlasul este orientare către cod și POM, iar guvernanța comună aparține ATF Tests.
Nota editorială și trecerea linkurilor nu certifică fidelitatea tuturor afirmațiilor.

## DOC-CONTEXT-DEPLOYMENT-111 — prioritate 81

**Prioritate: 81/100** = impact 31 + lacună 24 + lucrabil 18 + documentare 8.
**Owner:** guvernanța factuală J0; J1/J2/J3 păstrează contractele propriului deployment.

Atlasul J0/J1 declara J1 cu 10 module, J2 cu 8 și Reporting J6 consumat,
deși POM-urile au 9/7 module și consumerii au J8. Documentele sunt corectate;
guvernanța curentă nu detectează această contradicție semantică.

- Confruntați selectiv afirmațiile curente din atlas/index cu reactorul explicit, proprietatea Reporting și proveniența țintei; nu înghețați J8 ori un număr istoric în test.
- Proba negativă schimbă numai 9 în 10, 7 în 8 sau versiunea curentă J8 în J6; fiecare contradicție trebuie refuzată la afirmația exactă.
- Acceptați în continuare istoricul legitim J6/J7 și controalele la bazele originale; existența unei versiuni vechi în jurnal nu este singură un defect.
- Distingeți ownerul Core J0, Core JAR în J1/J2 și sursa Core locală J3; catalogul nu se micșorează implicit după foldere absente.

**Închidere:** documentele nominale și istorice trec, iar mutanții curenți
sunt refuzați fără relaxarea perimetrului. [Criteriul local J2](../../../atf_j2/docs/feedback/ATF_Documentation_Feedback.md)
are acceptanță proprie; nu închide implicit guvernanța comună.
