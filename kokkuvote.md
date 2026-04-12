Projekti kokkuvõte:

See on Spring Boot 4 (Java 21) REST API, mis pakub:

- JWT autentimine — registreerimine ja sisselogimine, kõik teised lõpp-punktid nõuavad Bearer tokenit
- Märkmed (Memo) — kasutajapõhine CRUD (pealkiri + sisu)
- Treeningulogi (Workout) — kasutajapõhine CRUD (harjutus, seeriad, kordused, kaal, kuupäev)
- Jalgpalli tulemused — välise api-sports.io API kaudu liigade ja mängude pärimine, piiratud lubatud
  liigade/meeskondadega

Andmebaasiks on PostgreSQL (Docker Compose'iga), testid jooksevad H2 in-memory baasil. Iga kasutaja näeb ainult oma
andmeid — teenuskiht filtreerib kõik päringud userId järgi. Frontend on eraldi Angular + Material rakendus.