Projekti kokkuvõte:

- JWT autentimine — registreerimine ja sisselogimine, kõik teised lõpp-punktid nõuavad Bearer tokenit
- Märkmed (Memo) — kasutajapõhine CRUD (pealkiri + sisu)
- Treeningulogi (Workout) — kasutajapõhine CRUD (harjutus, seeriad, kordused, kaal, kuupäev)
- Jalgpalli tulemused — välise api-sports.io API kaudu liigade ja mängude pärimine, filtreeritud
  liigade/meeskondadega

Andmebaasiks on PostgreSQL (Docker Compose'iga). Iga kasutaja näeb ainult oma
andmeid — teenuskiht filtreerib kõik päringud userId järgi. Frontendi ehitan eraldi Angulari abil.
