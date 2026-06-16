# ADR-004: Sikkerhetsmodell for OBO og CC

**Dato:** 2026-06-16  
**Status:** Godkjent  
**Beslutningstakere:** Team Sikkerhetstjenesten

## Kontekst

`entra-proxy` eksponerer et internt API som brukes i to ulike sikkerhetskontekster:

1. **On-behalf-of (OBO)** når en bruker er involvert, og kall skal utføres for innlogget ansatt
2. **Client credentials (CC)** når et system kaller API-et uten en sluttbruker i konteksten

Disse to kallmønstrene har ulike behov:

- OBO-kall skal bare kunne hente data for brukeren som faktisk er representert i tokenet
- CC-kall skal kunne slå opp data for en spesifisert ansatt når et autorisert system har behov for det
- Feil bruk av tokenkontekst kan føre til at data hentes for feil person eller at systemtilgang blir for vid

Dagens løsning skiller allerede mellom disse modellene i kodebasen:

- `EntraController` ligger bak `ProtectedRestController` og Azure AD-validering
- `Token` klassifiserer token som `OBO`, `CCF` eller `UNAUTHENTICATED`
- OBO identifiseres ved at tokenet inneholder brukerens `oid` og `NAVident`
- CC identifiseres ved at claim `idtyp` er `app`
- Produksjonsendepunktene skiller mellom varianter som bruker path-parameter for ansatt og varianter som bruker identitet fra tokenet

## Beslutning

Vi bruker en **tokendrevet sikkerhetsmodell** der OBO og CC behandles som to eksplisitte og forskjellige tilgangsmodeller.

Dette innebærer:

- alle produksjonsendepunkter ligger bak Azure AD-validering
- tokenet avgjør om kall skal behandles som OBO eller CC
- OBO-endepunkter skal hente ansattkontekst fra tokenet, ikke fra frie inputparametere
- CC-endepunkter kan ta inn `navIdent` som input når brukstilfellet krever system-til-system-oppslag
- endepunkter skal eksplisitt håndheve hvilken tokenmodell de støtter

`Token` fungerer som det sentrale abstraheringslaget for claim-utlesing og klassifisering av tokenkontekst.

## Hvorfor dette er valgt

Denne modellen gjør det tydelig at samme API ikke alltid betyr samme sikkerhetskontekst. Den gir en eksplisitt kobling mellom:

- hvilken identitet som ligger i tokenet
- hvilket endepunkt som kalles
- hvilke data som er lov å hente ut

Modellen gjør det også mulig å støtte både brukerinitierte og systeminitierte kall uten å blande tilgangsreglene i samme flyt.

Eksempler fra dagens løsning:

- `enhet` og `tema` finnes både som OBO-variant og CC-variant
- OBO-variantene bruker `token.oboFields` som kilde til `AnsattId` og `oid`
- CC-variantene krever eksplisitt `navIdent` og valideres med `token.assert { erCC }`
- dev-endepunktene holdes utenfor produksjonsmodellen gjennom egen controller og miljøavgrensning

## Alternativer vurdert

### Alternativ A: Ett felles autorisasjonsmønster for alle endepunkter
- **Fordeler:** Mindre eksplisitt API-overflate
- **Ulemper:** Skjuler viktige forskjeller mellom bruker- og systemkontekst, og gjør feilbruk lettere
- **Vurdering:** Avvist fordi sikkerhetsreglene blir mindre tydelige

### Alternativ B: Eksplisitt skille mellom OBO og CC i endepunkter og tokenlogikk (valgt)
- **Fordeler:** Tydelig sikkerhetsmodell, enklere å lese, enklere å teste og enklere å reviewe
- **Ulemper:** Noe mer API-overflate og noe mer controllerlogikk
- **Vurdering:** Beste balanse mellom tydelighet og fleksibilitet

### Alternativ C: Kun støtte én tokenmodell
- **Fordeler:** Enklere sikkerhetsmodell og færre varianter
- **Ulemper:** Ville ikke dekke både brukerinitierte og systeminitierte behov
- **Vurdering:** Ikke realistisk for tjenestens bruksmønster

## Arkitektoniske føringer

### Identitetskilde
- OBO-kall skal bruke identitet fra tokenet som sannhetskilde
- CC-kall kan bruke inputparameter for målansatt, men bare når endepunktet eksplisitt er laget for dette
- Claim-utlesing og tokenklassifisering skal holdes samlet i `Token`

### Endepunktsdesign
- Endepunkter skal eksplisitt uttrykke om de støtter OBO, CC eller begge deler gjennom separate operasjoner
- Sikkerhetskrav skal være synlige i controllerlaget, ikke skjult langt inne i tjenestelogikken
- Produksjons-API-et skal ikke avhenge av den mer åpne dev-controlleren

### Produksjon kontra dev
- Produksjonsendepunkter skal alltid ligge bak tokenvalidering
- Endepunkter som brukes til manuell testing eller feilsøking, skal fortsatt avgrenses til ikke-produksjonsmiljøer

## Konsekvenser

### Positive
- Tydeligere skille mellom bruker- og systemkontekst
- Lavere risiko for at data hentes på vegne av feil identitet
- Enklere kodegjennomgang fordi sikkerhetsreglene ligger nær endepunktene
- Bedre testbarhet av tokenklassifisering og tilgangsregler

### Negative
- Flere endepunktsvarianter gir større API-overflate
- Controllerlaget får noe duplisering mellom OBO- og CC-tilfeller
- Nye endepunkter må designes bevisst for riktig tokenmodell

### Risiko
- Nye endepunkter kan få feil sikkerhetsmodell dersom OBO og CC ikke vurderes eksplisitt
- Manglende eller feil claims i token kan gi uklar feiladferd dersom controllerlaget ikke håndhever modellen tydelig
- For mye logikk utenfor `Token` kan føre til inkonsistent tolkning av claims

## Aksjonspunkter

- [x] Behold `ProtectedRestController` og Azure AD-validering for produksjons-API-et
- [x] Behold `Token` som samlet abstraksjon for claim-utlesing og tokenklassifisering
- [x] Behold eksplisitt skille mellom OBO- og CC-endepunkter der sikkerhetskonteksten er forskjellig
- [ ] Vurder å dokumentere støttet tokenmodell per produksjonsendepunkt i API-dokumentasjonen
- [ ] Vurder å utvide tester som dokumenterer hvilke claims som er minimumskrav for OBO og CC

