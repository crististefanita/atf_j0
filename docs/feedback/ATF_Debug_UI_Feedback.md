# ATF Debug UI — fidelitatea căii artifactului

## Pe scurt

Serverul rămâne read-only și loopback. Criteriul privește identitatea fișierului
selectat de un path URI valid, fără a modifica raportul ori publicarea lui.

## DEBUG-URI-PATH-71 — plusul literal nu este spațiu

**Prioritate: 42/100** = impact 14 + lacună 9 + lucrabil 14 + documentare 5.
**Owner:** Debug UI server. **Nivel:** componentă socket loopback.

Cu fișiere distincte `proof+1.html` și `proof 1.html`, GET-ul
`/proof+1.html` servește conținutul fișierului cu spațiu. `URLDecoder` aplică
semantica form-urlencoded unui path. Ruta `%2B` generată de `uriFor` trece controlul nominal.

- [ ] Decodați path-ul cu semantica URI; păstrați separat `+`, `%2B` și `%20`, fără dublă decodare.
- [ ] Verificați corpul exact pentru GET și metadata corespunzătoare pentru HEAD, fără body; un 200 singur nu certifică identitatea artifactului.
- [ ] Păstrați containment-ul realpath, refuzul traversal-ului, accesul read-only și închiderea bounded cu request incomplet.

**Închidere:** [DebugUiServer](../../atf-debug-ui/src/main/java/atf/debugui/DebugUiServer.java)
servește fișierul cerut prin ambele forme valide ale plusului; encoded traversal
și path-uri malformate nu ies din root. Proba nu folosește un serviciu extern.
