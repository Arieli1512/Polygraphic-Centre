# Polygraphic Centre - Wymagania i przewodnik rozwoju

## 1. Opis projektu

Polygraphic Centre to system internetowy (frontend + backend) do obslugi zamowien drukarskich.

System ma zastapic obecny, rozproszony proces przyjmowania zlecen (e-mail, nosniki fizyczne, rozmowy telefoniczne).

W praktyce oznacza to jedno miejsce, w ktorym:

- klient wysyla plik i sklada zamowienie,
- pracownik drukarni realizuje zlecenie,
- menedzer ustawia ceny i dostepnosc,
- administrator zarzadza sklepami i personelem.

Docelowy stos technologiczny:

- frontend: React,
- backend: Spring Boot,
- baza danych: PostgreSQL,
- przechowywanie plikow: Google Cloud Storage,
- logowanie i tozsamosc: Firebase Authentication,
- komunikacja asynchroniczna: Google Pub/Sub.

## 2. Cele biznesowe

- Uproscic skladanie zamowien przez klientow.
- Przyspieszyc codzienna prace drukarni.
- Dac menedzerom mozliwosc szybkiej zmiany cen i godzin pracy.
- Dac administratorom kontrole nad sklepami i kontami personelu.
- Zapewnic odpornosc na chwilowe awarie uslug zewnetrznych (np. e-mail).
- Utrzymac rozwiazanie tak, aby maly zespol mogl je rozwijac i utrzymywac.

## 3. Role i zasady dostepu

W wersji roboczej v0 (zgodnej z erDiagram.mmd) obowiazuje nastepujacy model aktorow:

- GOSC: moze przegladac punkty druku i szacowac koszt, ale nie moze zlozyc zamowienia.
- KLIENT (Client): ma konto klienta i moze skladac zamowienia.
- OPERATOR (Operator): personel punktu druku.
  - EMPLOYEE: obsluga zamowien operacyjnych.
  - ADMIN: rozszerzone uprawnienia dla punktu druku.

Role biznesowe, ktore nadal obowiazuja w koncepcji produktu:

- MENEDZER_PUNKTU: odpowiada za cennik, godziny otwarcia i konfiguracje punktu.
- ADMINISTRATOR: odpowiada za nadzor globalny nad platforma.

W implementacji v0 role te sa mapowane tak:

- MENEDZER_PUNKTU -> OPERATOR z rola ADMIN.
- ADMINISTRATOR -> zakres docelowy, tymczasowo poza ERD v0.

Najwazniejsze zasady uprawnien:

- Klient widzi tylko swoje zamowienia i swoj portfel.
- Operator dziala tylko w zakresie przypisanego punktu druku.
- Operacje administracyjne punktu druku wymagaja roli OPERATOR = ADMIN.
- Kazda operacja zmieniajaca dane musi byc autoryzowana po stronie backendu.

## 4. Wymagania funkcjonalne

### 4.1 Logowanie i autoryzacja

- Rejestracja i logowanie dzialaja przez Firebase Authentication.
- Token JWT przekazuje informacje potrzebne do identyfikacji uzytkownika.
- Backend zawsze weryfikuje role i zakres dostepu.
- Dla danych klienta obowiazuje kontrola wlasciciela (ownership check).

### 4.2 Funkcje goscia

- Przeglad listy drukarni i ich lokalizacji bez logowania.
- Szacowanie ceny wydruku bez zakladania konta.
- Proba finalizacji zamowienia bez logowania konczy sie przekierowaniem do logowania/rejestracji.

### 4.3 Funkcje klienta

- Wysylanie obslugiwanych plikow do chmury.
- Wybor parametrow wydruku (PrintSettings): kolor, format, duplex, orientacja, wykonczenie, liczba kopii.
- Wybor terminu odbioru (pole pickup_at).
- Podglad kosztu przed zatwierdzeniem zamowienia.
- Podglad historii zamowien.
- Edycja lub anulowanie zamowienia zgodnie z zasadami statusow v0.
- Otrzymywanie informacji o zmianie statusu i gotowosci do odbioru.
- Podglad i doladowanie portfela (Wallet, WalletTopUp).

### 4.4 Funkcje operatora

- Podglad aktywnych zamowien, domyslnie posortowanych po czasie odbioru.
- Pobieranie plikow klienta do realizacji.
- Zmiana statusu zamowienia zgodnie z v0: PENDING -> APPROVED -> READY -> DISPENSED oraz sciezki anulowania.
- Rejestracja wykonania wydruku (Printout).

### 4.5 Funkcje operatora z rola ADMIN

- Ustalanie i zmiana cennika stron (RateSheet).
- Ustalanie doplat dodatkowych (ExtraPricing).
- Ustalanie godzin otwarcia i dostepnych godzin odbioru.
- Zarzadzanie operatorami punktu druku.

Ten zakres odpowiada biznesowej roli MENEDZER_PUNKTU w wersji v0.

### 4.6 Funkcje administracyjne (zakres docelowy)

- Zakres globalnego administratora pozostaje celem docelowym.
- W wersji v0 funkcje administracyjne realizowane sa glownie przez OPERATOR = ADMIN w ramach punktu druku.

Uwaga: biznesowa rola ADMINISTRATOR pozostaje czescia koncepcji, mimo ze ERD v0 nie ma jeszcze osobnej encji globalnego administratora.

### 4.7 Powiadomienia i zdarzenia

- Drukarnia musi otrzymac informacje o nowym pliku i nowym zamowieniu.
- Klient musi otrzymywac informacje o zmianie statusu zamowienia.
- Klient musi dostac informacje, gdy zamowienie jest gotowe do odbioru.
- Chwilowa niedostepnosc dostawcy e-mail nie moze blokowac obslugi zamowien.

### 4.8 Kontrakt API i diagnostyka

- HTTP status code jest glownym sygnalem wyniku operacji.
- Odpowiedzi sukcesu zwracaja dane domenowe i metadane (w tym requestId i timestamp).
- Odpowiedzi bledow musza byc zgodne z Problem Details (`application/problem+json`) i zawierac co najmniej: type, title, status, detail, instance, code, userMessage, action, requestId, traceId, timestamp.
- API musi wspierac korelacje i tracing przez `traceparent`, `tracestate` i `x-request-id`.
- Dla wrazliwych operacji POST (np. finansowych) API musi wspierac `Idempotency-Key`.
- Deprecjacja endpointow i pol API musi byc komunikowana przez naglowki `Deprecation` i `Sunset` oraz przez OpenAPI.

## 5. Wymagania niefunkcjonalne

### 5.1 Wydajnosc i skalowanie

- Dla typowych operacji CRUD API powinno odpowiadac ponizej 500 ms w 95% przypadkow (bez czasu przesylu duzego pliku).
- Wysylanie duzych plikow nie moze blokowac glownej sciezki backendu.
- Rozwiazanie powinno skalowac sie przez uslugi chmurowe i komunikacje asynchroniczna.

### 5.2 Niezawodnosc

- Operacje finansowe i saldo musza byc transakcyjne.
- Gdy wysylka e-mail sie nie powiedzie, proces zamowienia ma dzialac dalej, a wiadomosc ma trafic do ponowienia.
- System nie moze tracic danych zamowien przy chwilowych awariach uslug zewnetrznych.

### 5.3 Bezpieczenstwo

- Kazda operacja zmieniajaca dane wymaga waznego JWT.
- Uprawnienia sa egzekwowane po stronie serwera.
- Dostep do plikow jest ograniczony do uprawnionych osob.

### 5.4 Obserwowalnosc i operacyjnosc

- Kazde zapytanie API musi byc mozliwe do prześledzenia po requestId i traceId.
- Logi backendu musza laczyc identyfikatory korelacyjne z bledami i metrykami.
- Odpowiedzi bledow nie moga ujawniac danych tajnych ani stack trace.
- System powinien umozliwiac szybka diagnostyke incydentow w architekturze rozproszonej.

## 6. Architektura docelowa

System sklada sie z nastepujacych elementow:

- frontend (React SPA),
- backend (Spring Boot REST API),
- baza danych PostgreSQL,
- magazyn plikow w Google Cloud Storage,
- kolejki zdarzen w Google Pub/Sub,
- uwierzytelnianie przez Firebase Authentication,
- lekki worker/usluga do przetwarzania powiadomien.

Preferowany model wysylki pliku:

- frontend pobiera podpisany URL z backendu,
- plik trafia bezposrednio do storage,
- backend zapisuje metadane pliku i laczy je z zamowieniem.

## 7. Plan wdrozenia

### Faza 1: Fundamenty

- Ujednolicenie slownictwa i modelu rol.
- Definicja cyklu zycia zamowienia i podstawowych zasad biznesowych.
- Ustalenie stylu API i formatu bledow.
- Ustalenie konwencji projektu (nazewnictwo, struktura, konfiguracja).
- Przygotowanie dokumentacji srodowisk dla calego zespolu.

### Faza 2: Fundament backendu

- Integracja bazy danych i warstwy trwalosci.
- Dodanie encji, repozytoriow i warstwy serwisowej.
- Dodanie walidacji i globalnej obslugi bledow.
- Dodanie bezpieczenstwa i weryfikacji tokenu.
- Dodanie dokumentacji API (OpenAPI/Swagger).

### Faza 3: Fundament frontendu

- Zastapienie aplikacji startowej docelowa powloka.
- Dodanie routingu i struktury stron.
- Dodanie obslugi stanu autoryzacji.
- Dodanie wspolnego klienta API.
- Dodanie wspolnych wzorcow formularzy i walidacji.

### Faza 4: Glowny przeplyw klienta

- Przegladanie drukarni.
- Szacowanie kosztu i tworzenie zamowienia.
- Wysylka plikow.
- Historia i modyfikacja zamowien.

### Faza 5: Operacje personelu

- Kolejka zamowien.
- Sortowanie i filtrowanie.
- Szczegoly zamowienia i pobieranie pliku.
- Zmiany statusu i obsluga problemow.

### Faza 6: Konfiguracja i administracja

- Ekrany konfiguracji drukarni.
- Zarzadzanie cennikiem, pojemnoscia i dostepnoscia.
- Zarzadzanie personelem i sklepami.
- Raportowanie.

### Faza 7: Zdarzenia i powiadomienia

- Publikacja zdarzen dla istotnych zmian zamowien.
- Asynchroniczne powiadomienia e-mail.
- Mechanizmy ponowien i dead-letter.

### Faza 8: Jakosc i gotowosc do wydania

- Testy jednostkowe, integracyjne i end-to-end.
- Linting i formatowanie.
- Konfiguracja wdrozen i checklista wydan.
- Wytyczne kopii zapasowych, monitoringu i odzyskiwania.

## 8. Podzial pracy zespolu (5 strumieni)

1. Backend i model danych.
2. Uwierzytelnianie i autoryzacja.
3. Frontend dla klienta.
4. Frontend dla personelu i administracji.
5. Integracje, powiadomienia i jakosc.

## 9. Zasady realizacji

### 9.1 Zasady pracy

- Najpierw najprostsza wersja, ktora spelnia wymaganie.
- Kontrakty frontend-backend sa zawsze uzgadniane.
- Preferowane sa male, pionowe przyrosty funkcji.
- Brak implementacji funkcji bez jasnego modelu danych i zasad dostepu.
- Wspolne slownictwo obowiazuje w kodzie, dokumentacji, testach i UI.

### 9.2 Definicja ukonczenia zadania

Zadanie jest zakonczone tylko wtedy, gdy:

- funkcja dziala od konca do konca,
- bledy sa obsluzone,
- testy sa dodane lub zaktualizowane,
- dokumentacja jest zaktualizowana (jesli dotyczy),
- funkcja jest zweryfikowana lokalnie.

### 9.3 Co standaryzujemy najwczesniej

- Nazwy rol.
- Statusy zamowien.
- Limity typow i rozmiarow plikow.
- Sciezki API i ksztalt odpowiedzi.
- Obsluge dat i czasu.
- Klucze konfiguracji lokalnej i chmurowej.

W zakresie zadania 1.2 obowiazujacy standard kontraktu API i formatu bledow znajduje sie w pliku: KONTRAKT_API_I_OBSLUGA_BLEDOW.md.
W zakresie modelu domeny v0 obowiazuje slownik i mapowanie z pliku: SLOWNIK_I_MODEL_DOMENY.md.

## 10. Srodowisko lokalne

### 10.1 Co trzeba zainstalowac

- Git,
- Java 25,
- Node.js 18 lub nowszy,
- VS Code,
- terminal,
- opcjonalnie Docker Desktop / Docker Engine.

### 10.2 Standard lokalnej bazy

Lokalna baza PostgreSQL dziala w Dockerze. Zespol ma wspolne skrypty do:

- startu bazy,
- zatrzymania i usuniecia kontenera,
- wykonania zrzutu (dump),
- odtworzenia stanu z dumpa.

Skrypty musza byc trzymane w repozytorium, aby kazdy uzywal tych samych ustawien.

### 10.3 Kroki pierwszego uruchomienia

1. Sklonuj repozytorium.
2. Otworz projekt w VS Code.
3. Uruchom dwa terminale (backend i frontend).
4. Zainstaluj zaleznosci frontendu.
5. Sprawdz wersje Java i Node.
6. Utworz lokalne pliki srodowiskowe.
7. Uruchom backend zgodnie z backend/README.md.
8. Uruchom frontend zgodnie z frontend/README.md.
9. Sprawdz, czy oba serwisy dzialaja bez bledow.

### 10.4 Zmienne srodowiskowe

Minimalny zakres:

- polaczenie do PostgreSQL,
- konfiguracja Firebase,
- konfiguracja Google Cloud Storage,
- konfiguracja Pub/Sub,
- konfiguracja e-mail,
- URL backendu dla frontendu.

## 11. Strategia testow

- Testy backendu: logika cen, walidacje, przejscia statusow, repozytoria, autoryzacja.
- Testy frontendu: formularze, routing oparty o role, obsluga bledow API.
- Testy end-to-end: pelne scenariusze goscia, klienta, pracownika, menedzera i administratora.

## 12. Wdrazanie i operacje

Wymagane elementy wdrozenia:

- backend,
- frontend,
- PostgreSQL,
- Google Cloud Storage,
- Pub/Sub,
- integracja e-mail.

Obszary operacyjne:

- konfiguracja per srodowisko,
- bezpieczna obsluga sekretow,
- kopie zapasowe i odzyskiwanie,
- logowanie i monitoring,
- ponowienia i dead-letter dla wiadomosci asynchronicznych.

Checklista wydania:

- testy przechodza,
- build przechodzi,
- zmienne srodowiskowe sa uzupelnione,
- zasoby chmurowe sa dostepne,
- kluczowe przeplywy sa przetestowane.

## 13. Ograniczenia i zalozenia

- Repozytorium jest na etapie szkieletu; wiekszosc funkcji trzeba zbudowac.
- Architektura ma pozostac cloud-friendly i event-driven.
- Priorytetem jest stabilny fundament, a nie dopracowanie UI na starcie.
- Docker jest standardem dla lokalnej bazy, ale nie blokuje uruchamiania aplikacji lokalnie.

## 14. Pierwsze zalecane zadania

1. Ujednolicenie slownictwa i zakresu projektu.
2. Rozszerzenie instrukcji uruchamiania srodowiska.
3. Definicja modelu danych i cyklu zycia zamowienia.
4. Przygotowanie bezpieczenstwa backendu i kontraktow API.
5. Zastapienie frontendu startowego docelowa powloka aplikacji.

## 15. Decyzje ustalone dla biezacego zakresu

- Lokalna baza: PostgreSQL w Dockerze, z wersjonowanymi skryptami start/stop/dump/restore.
- Autoryzacja: model hybrydowy.
  - Firebase odpowiada za logowanie i token.
  - Minimalne custom claims moga sluzyc jako wskazowka roli.
  - Backendowa baza danych jest zrodlem prawdy dla zakresu uprawnien.
  - Backend egzekwuje uprawnienia dla kazdej operacji chronionej.
- Raporty: PDF jest wymagany w pierwszym wydaniu; CSV jest opcjonalne.
- Zakres pierwszego wydania: obejmuje panel menedzera i administratora (nawet jesli czesc widokow bedzie tymczasowo szkieletowa).
