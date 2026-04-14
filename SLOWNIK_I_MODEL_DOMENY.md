# Slownik i model domeny (wersja robocza v0)

## 1. Cel dokumentu

Ten dokument ustala wspolny jezyk projektu Polygraphic Centre.
Model opisany tutaj jest wersja robocza v0 i wynika bezposrednio z pliku erDiagram.mmd.
To punkt wyjscia do implementacji, a nie finalny design.

Jest to zrodlo prawdy dla:

- nazw rol,
- nazw kluczowych encji,
- cyklu zycia zamowienia,
- podstawowych zasad dostepu.

Kazda nowa funkcja, ekran, endpoint i test powinny uzywac nazw zgodnych z tym dokumentem.

## 2. Aktorzy systemu i role

### 2.1 Role biznesowe

- GOSC (Guest): osoba bez logowania.
- KLIENT (Client): osoba skladajaca i sledzaca swoje zamowienia.
- OPERATOR (Operator): pracownik punktu druku obslugujacy zamowienia.
- MENEDZER_PUNKTU (PointManager): osoba zarzadzajaca konfiguracja punktu druku.
- ADMINISTRATOR (Administrator): rola globalna dla calej platformy.

W wersji v0 roznienie uprawnien personelu jest tymczasowo przez pole roli operatora:

- EMPLOYEE: obsluga operacyjna zamowien.
- ADMIN: uprawnienia rozszerzone dla punktu druku.

Mapowanie biznes -> techniczne (v0):

- OPERATOR -> Operator.role = EMPLOYEE
- MENEDZER_PUNKTU -> Operator.role = ADMIN
- ADMINISTRATOR -> tymczasowo poza ERD v0 (docelowo osobna reprezentacja globalna)

### 2.2 Reprezentacja roli w bazie (v0)

- Tabela Client nie ma pola roli i reprezentuje klienta.
- Tabela Operator ma pole role z wartosciami: ADMIN | EMPLOYEE.
- Gosc nie ma rekordu w bazie (wynika z braku zalogowania).

## 3. Kluczowe encje domenowe (v0)

Lista obejmuje encje zaprojektowane obecnie w ERD:

- Client: konto klienta (dane tozsamosci i status).
- Wallet: portfel klienta (saldo, status portfela).
- WalletTopUp: doladowanie portfela.
- PrintingPoint: punkt druku realizujacy zamowienia.
- OpeningHours: godziny otwarcia punktu druku.
- Operator: pracownik punktu druku (z rola ADMIN lub EMPLOYEE).
- RateSheet: cennik za strone zalezny od papieru i formatu.
- ExtraPricing: dodatkowe oplaty (oprawa, zszywanie, okladka).
- Order: zamowienie klienta.
- PrintSettings: parametry druku dla zamowienia.
- Printout: informacja o fizycznym wydruku i operatorze.

## 4. Relacje biznesowe (opis prosty)

- Client ma dokladnie jeden Wallet.
- Wallet moze miec wiele wpisow WalletTopUp.
- PrintingPoint ma wiele rekordow OpeningHours.
- PrintingPoint zatrudnia wielu Operator.
- PrintingPoint ma cennik w RateSheet i dodatki w ExtraPricing.
- Client sklada wiele Order w wybranym PrintingPoint.
- Kazde Order ma jedno PrintSettings.
- Order moze miec jeden Printout (po wydruku).
- Printout wskazuje Operator, ktory wykonal wydruk.

## 5. Cykl zycia zamowienia (automat stanow)

### 5.1 Statusy kanoniczne

- PENDING: zamowienie oczekuje na decyzje punktu druku.
- APPROVED: zamowienie przyjete do realizacji.
- READY: zamowienie gotowe do odbioru.
- DISPENSED: zamowienie wydane klientowi.
- CANCELLED: zamowienie anulowane.

### 5.2 Dozwolone przejscia

- PENDING -> APPROVED
- PENDING -> CANCELLED
- APPROVED -> READY
- APPROVED -> CANCELLED
- READY -> DISPENSED

### 5.3 Zasady biznesowe do egzekwowania

- Klient moze anulowac zamowienie, dopoki nie zostalo wydane (DISPENSED).
- Przejscia statusow sa walidowane po stronie backendu.
- Kazda zmiana statusu zapisuje wpis w historii i moze uruchomic powiadomienie (mechanizm docelowy).

## 6. Slownik terminow

- Klient (Client): osoba skladajaca zamowienie.
- Portfel (Wallet): saldo klienta uzywane do rozliczen.
- Doladowanie portfela (WalletTopUp): zasilenie salda.
- Punkt druku (PrintingPoint): lokalizacja realizujaca wydruk.
- Godziny otwarcia (OpeningHours): harmonogram punktu druku.
- Operator (Operator): pracownik punktu druku.
- Cennik stron (RateSheet): ceny za strone zaleznie od papieru i formatu.
- Doplaty dodatkowe (ExtraPricing): oplaty za dodatki wykonczenia.
- Zamowienie (Order): glowny rekord zlecenia klienta.
- Ustawienia druku (PrintSettings): parametry wydruku zamowienia.
- Wydruk (Printout): zapis wykonania wydruku przez operatora.

## 7. Minimalne zasady spojnosc dokumentacji

- Uzywamy wyraznie nazw biznesowych: GOSC, KLIENT, OPERATOR, MENEDZER_PUNKTU, ADMINISTRATOR.
- W dokumentacji technicznej stosujemy nazwy kodowe: Guest, Client, Operator, PointManager, Administrator.
- W implementacji v0 mapujemy role biznesowe na aktualny model Operator.role i opisujemy to wprost przy kazdym fragmencie, gdzie moze powstac niejednoznacznosc.
- Nazwy statusow i encji w API, backendzie, frontendzie i testach musza byc zgodne z tym dokumentem.

## 8. Nazwy API po stronie kodu (angielski)

Nazwy zasobow REST API powinny byc po angielsku i w liczbie mnogiej:

- klienci: /clients
- portfele: /wallets
- doladowania portfeli: /wallet-top-ups
- punkty druku: /printing-points
- godziny otwarcia: /opening-hours
- operatorzy: /operators
- cenniki stron: /rate-sheets
- doplaty dodatkowe: /extra-pricing
- zamowienia: /orders
- ustawienia druku: /print-settings
- wydruki: /printouts

## 9. Co dalej po przyjeciu dokumentu

- Uzgodnic finalne nazwy enumow i tabel z zespolem backendowym.
- Potwierdzic finalny diagram stanu zamowienia dla wersji docelowej (poza v0).
- W kolejnych zadaniach (1.2 i 2.x) odnosic sie do tego dokumentu jako standardu nazewnictwa.
- Dla kontraktu API i formatu bledow stosowac: KONTRAKT_API_I_OBSLUGA_BLEDOW.md.

## 10. Mapowanie nazw z poprzedniej wersji dokumentacji

- PrintShop -> PrintingPoint
- PricingRule -> RateSheet (oraz czesciowo ExtraPricing)
- PrintOption -> PrintSettings
- PickupSlot -> pickup_at (pole w Order)
- Balance / ClientBalance -> Wallet
- User/Worker/Manager/Admin (osobne encje) -> Client i Operator
- MENEDZER_PUNKTU (biznes) -> Operator.role = ADMIN (v0)
- ADMINISTRATOR (biznes) -> rola docelowa poza ERD v0
- READY_FOR_PICKUP -> READY
- COLLECTED -> DISPENSED
