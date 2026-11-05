# Arbetsprov Bolagsverket
Som *ett steg i processen* till *tjänsten IT-specialist systemutveckling* har Bolagsverket bett om ett arbetsprov.  Önskemålet är ett REST API där *det är möjligt att*:
- lista länder som vid start importerats från [Rest Countries](https://restcountries.com/) till en egen databas (API:et får inte agera proxy mot Rest Countries)
- hantera användares besökta länder
- spara en kort anteckning per besök

**Varning**: Detta repo och det tillhörande GitHub-projektet är publika.

## Kom igång
Kräver **JDK 25** eller enbart **Docker**.  Maven följer med i repot (`mvnw`).

| Vad | Linux / macOS | Windows (PowerShell) |
|---|---|---|
| Bygga och köra alla tester | `eng/scripts/build.sh` | `eng\scripts\build.ps1` |
| Starta tjänsten på port 8080 | `eng/scripts/run-local.sh` | `eng\scripts\run-local.ps1` |
| Smoke test mot en körande tjänst | `eng/scripts/smoke.sh` | (via Git Bash) |

Med Docker (från repots rot):
```
docker build -f service/Dockerfile -t countries-api .
docker run --rm -p 8080:8080 countries-api
```

Öppna sedan http://localhost:8080 som leder till Swagger där samtliga endpoints kan testas.  Anrop mot besök kräver headern `X-User-Id` (tex. `alice`).  Samma anrop finns även under [eng/http](./eng/http/).

[Rest Countries](https://restcountries.com/) kräver numera en API-nyckel.  Utan nyckel startar tjänsten från en inbyggd ögonblicksbild (250 länder, 2026-11-04).  Med nyckel importeras länderna live:
```
docker run --rm -p 8080:8080 -e RESTCOUNTRIES_API_KEY=<nyckel> countries-api
```

**Varning**: Utan **RESTCOUNTRIES_API_KEY** används en lokal [kopia av JSON](./countries/src/main/resources/se/arbetsprov/countries/countries-snapshot.json) data om länder

## IT-specialist systemutveckling
I annonsen för IT-specialist systemutveckling efterfrågar Bolagsverket erfarenhet av:
- CI/CD-pipelines
- mikrotjänster (REST API)
- Docker
- Maven/Gradle
- VS Code
- Java med Spring Boot

Andra färdigheter önskas men ovanstående är tillräckligt styrande för de tekniska besluten.  Enligt annonsen har Bolagsverket *inlett en förflyttning mot att använda AI som en naturlig del av utvecklingsteamens arbete*.

Bolagsverket söker:
- god analytisk förmåga
- löser problem i projekttempo men behåller ett långsiktigt perspektiv
- nyfiken
- uppdaterad inom ditt område
- erfarenhet av AI

Arbetsprovet har lösts utifrån behoven ovan på cirka 4 timmar.

## Arbetssätt
Java har inte programmerats sedan 2015 och de senaste 10 åren har arbetet främst ägnats åt C# i Azure-molnet.  Inför ansökan lästes mycket in om modern Java, främst virtuella trådar (projekt Loom) och stödet för dem i Spring Boot 3.2 (projektet använder Spring Boot 4.1).  Reaktiv programmering och virtuella trådar passar dåligt ihop så lösningen är helt synkron, dvs. blockerande anrop på virtuella trådar utan reaktiva typer som **Mono**.

Mycket av tiden gick åt till att översätta mönster från C# till Java.

Eftersom annonsen inte angav hur AI ska användas har [Claude Code](https://claude.com/claude-code) drivit [spec-kit](https://github.com/github/spec-kit) där principer och antaganden kartlagts.  Arkitekturen och principerna har bestämts av mig och samtliga testfall har granskats och godkänts innan någon kod skrevs.  Claude Code har sedan skrivit merparten av koden, testerna och CI/CD-skripten så att lösningen kan köras lokalt både i och utanför Docker.

## Arkitektur
Allt ligger i ett monorepo och inte enbart på grund av att utvecklingskontexten är ett arbetsprov.  Det finns fördelar med monorepo som annars mest förekommer hos stora bolag tex. Google eller Microsoft.  Den största är en gemensam CI/CD för alla interna projekt i repot.  Under [eng](./eng/) finns gemensamma CI/CD-funktioner:
- bygg
- test
- smoke test
- anrop per user story ([eng/http](./eng/http/))

CI ([.github/workflows/ci.yml](./.github/workflows/ci.yml)) bygger, testar, startar tjänsten, kör smoke test och bygger Docker-imagen.  Många fler funktioner skulle kunna finnas tex. gemensam bygglogik med Gradle (trots valet av Maven i detta repo), hemliga nycklar, miljövariabler med mera.

Det finns en skiljelinje mellan *klienter* och *tjänster*.  En *klient* använder HTTP/S mot *tjänstens* REST API.  Därför finns en gemensam [HTTP-pipeline](./core/) där återkommande steg (policyer) med konfigurerbara strategier körs i en bestämd ordning.  Exempel på steg i kontakten med en fjärrtjänst:
- autentisering/auktorisering
- återförsök
- strypventil (throttling)

Utan insyn i hur Bolagsverket kommunicerar mellan sina mikrotjänster (tex. REST eller gRPC) kändes det enklast med tunna *klienter* för länder och besök ovanpå en HTTP-pipeline.

Det visade sig direkt att en auktoriseringspolicy var nödvändig eftersom en API-nyckel måste skickas med mot [Rest Countries](https://restcountries.com/).  Återförsökspolicyn är enbart ett skal och strypventil finns inte men båda visar var sådana steg skulle placeras i pipelinen.

En HTTP-pipeline innebär att *klienter* kan fokusera på sina krav.  [Landklienten](./countries/) importerar länderna en gång vid start (3 anrop om 100 länder, ca 0,7 s per anrop) och svarar sedan alltid från sin cache.  Utan API-nyckel, eller om [Rest Countries](https://restcountries.com/) inte svarar, fylls cachen från ögonblicksbilden.  Interaktionen med länder klarar:
- sortering
- filtrering
- paginering

[Besöksklienten](./visits/) har ingen fjärrtjänst utan en lokal cache per användare.  Testerna använder en fejkad klocka för att få förutsägbara tider.

[Tjänsten](./service/) vilar på Spring Boot och har en Dockerfil (**observera**: portmappningen i `docker run --rm -p 8080:8080 countries-api`).  Tjänsten består av enkla CRUD-anrop som slussas via de 2 *klienterna*, som i verkligheten förmodligen skulle ha egna bakomliggande API:er.  I ett riktigt upplägg skulle tjänsten ligga i ett eget repo.

## Begränsningar
- besök finns bara i minnet och försvinner vid omstart
- flera instanser av tjänsten delar inte besök (en delad databas skulle behövas)
- `X-User-Id` litas på som den är, ingen riktig autentisering
- ögonblicksbilden av länder uppdateras manuellt

## AI (bonus)
Ett exempel på hur ett handskrivet index i FAISS-anda (FLAT) tillsammans med en flerspråkig embedding-modell ([multilingual-e5-small](https://huggingface.co/intfloat/multilingual-e5-small)) kan dirigera en NL (natural language) fråga eller direktiv till ett minimalt LLM med ansvaret att fylla i parametrarna till ett REST API-anrop.  Anropet besvaras av tjänstens controller och JSON-svaret visas i chatten på svenska eller engelska.  Uppsättningen är en extrem förenkling men redovisar hur ett användargränssnitt kan utnyttja AI eller samspela med resurser som en SQL-databas.

[ai](./ai/) är en *klient* precis som länder och besök, dvs. en CLI som anropar den körande [tjänsten](./service/) via [HTTP-pipelinen](./core/).  Tjänsten i Docker är det enda externa.  Flödet:
- frågan omvandlas till en vektor med multilingual-e5-small (svenska frågor hittar engelska beskrivningar)
- FLAT-indexet jämför vektorn mot en katalog med 6 operationer skrivna på engelska (tex. *List countries*), var och en med en beskrivning och några korta exempel, och väljer den närmaste
- under tröskeln 0,80 godtas en träff bara om den tydligt slår näst bästa operation (minst 0,015 före, ned till 0,77), annars blir svaret *Jag förstod inte*
- ett litet LLM ([qwen2.5:1.5b](https://ollama.com/library/qwen2.5) via [Ollama](https://ollama.com/)) fyller enbart i parametrarna (tex. region och sortering), aldrig adressen
- anropet byggs från katalogen, värdena kontrolleras och tjänsten anropas
- svaret visas med fasta mallar på svenska eller engelska (eller rå JSON med `--json`), språket avgörs lokalt utifrån vanliga ord i frågan och inte av LLM:et

FLAT valdes framför IVF_PQ eftersom katalogen är liten.  IVF_PQ behöver tusentals vektorer att träna på och lönar sig först vid miljontals poster, därför finns den enbart dokumenterad.

Två faser, så att det är ett eget val att hämta modeller lokalt:
```
eng/scripts/ask.sh --setup
eng/scripts/ask.sh --user alice
```
(Windows: `eng\scripts\ask.ps1`)

`--setup` kontrollerar och frågar `Installera? (Yes/No)` för varje sak som saknas (enbart `Yes`/`yes`/`No`/`no` godtas):
- modellen `model_quantized.onnx` (118 MB, Xenova) och `tokenizer.json` (17 MB, intfloat), båda MIT, till `~/.cache/arbetsprov/models` med kontroll av SHA-256
- Ollama via `winget` (Windows) eller `brew` (macOS), på Linux skrivs installationskommandot ut
- `ollama pull qwen2.5:1.5b` (cirka 1 GB)

`--user` kräver att setup körts, annars avbryts den direkt med en förklaring.  Standardvärden visas med `--help` (tjänst `http://localhost:8080`, språk `sv`).  I chatten byter `/sv` och `/en` språk och `/exit` avslutar:
```
(chat) Vad vill du göra?
(alice) lista de fem största länderna i Europa
(chat) GET /api/countries?region=Europe&sort=population&direction=desc&size=5
(chat) 1. Russia – 146 028 325 invånare
```

Felkoder: 2 felaktiga alternativ, 3 något saknas (kör `--setup`), 4 tjänsten eller Ollama svarar inte.  CI behöver varken modell eller Ollama eftersom testerna använder stubbar och WireMock.

Begränsningar med ett så litet LLM (1,5 miljarder parametrar), uppmätta mot den riktiga tjänsten:
- antal kan utelämnas (*de tre minsta* ger 20 länder i rätt ordning)
- landskoder kan bli fel (*Danmark* blev `DEN` i stället för `DNK`, tjänsten svarar då 404 och chatten fortsätter)
- en anteckning kan hittas på trots att ingen angavs
