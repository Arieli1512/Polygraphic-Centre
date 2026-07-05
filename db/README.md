## O Dockerze

Docker wykorzystuje mechanizmy izolacji dostępne w jądrze Linuxa (m.in. namespaces i cgroups), dzięki którym możliwe jest uruchamianie aplikacji w odseparowanych środowiskach zwanych kontenerami. W porównaniu do maszyn wirtualnych kontenery są znacznie lżejsze, uruchamiają się szybciej i zużywają mniej zasobów, ponieważ współdzielą jądro systemu operacyjnego.

Na systemie Linux Docker działa bezpośrednio na jądrze systemu. Na systemach Windows i macOS wymagany jest Docker Desktop, który uruchamia niewielką maszynę wirtualną z Linuxem. Wszystkie kontenery działają wewnątrz tej maszyny, dzięki czemu zachowują się identycznie niezależnie od używanego systemu operacyjnego.

Aplikacje uruchamiane przez Dockera są dostarczane w postaci obrazów (images). Obraz zawiera kompletną konfigurację potrzebną do uruchomienia danego programu. Przykładowo obraz postgres:18 zawiera gotową instalację PostgreSQL 18. Po pobraniu obrazu Docker może utworzyć na jego podstawie kontener i uruchomić działającą instancję bazy danych.

## Uruchamianie bazy danych

Przede wszystkim, zainstalujcie **Docker** lub **Docker Desktop** (Windows / macOS).

Następnie możecie skorzystać z gotowych skryptów.
> Jeśli korzystacie z Windows, musicie uruchamiać skrypty przy pomocy Git Bash (instalowany razem z Git for Windows).

Zakładając, że znajdujecie się terminalem w głównym folderze projektu:

- `db/scripts/start.sh`

Startuje Postgres w kontenerze Docker.
Jedynie pierwsze wywołanie utworzy samą bazę oraz wywoła skrypty SQL znajdujące się w folderze `db/init`, które tworzą tabele oraz dodają seed (czyli dane początkowe / testowe).

- `db/scripts/stop.sh`

Zatrzymuje kontener z Postgresem.

- `db/scripts/start.sh --watch`

Uruchamia Postgres w trybie nasłuchiwania. Wtedy zamiast wykonywać skrypt `stop.sh`, wystarczy zabić sam proces w terminalu przez naciśnięcie `Ctrl + C`.

- `db/scripts/reset.sh`

Resetuje bazę danych do stanu początkowego.
Domyślnie, wszelkie zmiany jakich dokonacie w bazie danych będą zapisywane w Waszym lokalnym systemie plików i nie utracicie ich po restarcie. Jednak wykonanie tego skryptu te zmiany usunie.

- `db/scripts/dump.sh`

Zastąpi plik `db/backups/dump.sql` aktualnym stanem bazy danych. Taki plik jest wersjonowany (zapisywany w repozytorium git), dzięki czemu dane te można łatwo odzyskać. Może przyjąć opcjonalny argument określający alternatywną nazwę pliku wynikowego, np: `db/scripts/dump.sh sprint-3` utworzy plik `db/backups/sprint-3.sql`

- `db/scripts/restore.sh`

Usuwa obecny stan bazy danych i przywraca stan zapisany w pliku `db/backups/dump.sql`.

- `db/scripts/run.sh`

Otwiera interaktywną konsolę, w której można wpisywać polecenia SQL i testować bazę danych.
Podpowiedź: wpisanie `quit` zamyka konsolę.

## Testowanie bazy danych

Najłatwiej przetestować przez powyższy skrypt `db/scripts/run.sh`.

Alternatywnie, dla bardziej zaawansowanego użycia, można zainstalować program **pgAdmin 4**.
Po instalacji należy zarejestrować nowy serwer i w konfiguracji wpisać:

![Rejestracja serwera](images/pgadmin-register-server.png)

Password: pgadmin

Po tym wystarczy otworzyć *Query Tool*, będąc w bazie danych *drobnyd*

![Query Tool](images/pgadmin-query-tool.png)

## Konta logowania Firebase

Rekordy `clients` i `operators` w seedzie są danymi demonstracyjnymi bazy PostgreSQL. Same w sobie nie tworzą kont Firebase Auth i nie gwarantują możliwości logowania.

Aby zalogować się jako klient, pracownik albo administrator, potrzebujesz obu elementów:

1. konta w Firebase Authentication z konkretnym `firebase_uid`,
2. odpowiadającego mu rekordu w PostgreSQL (`clients` albo `operators`).

W praktyce oznacza to, że:

- seedowe rekordy operatorów nie są automatycznie loginowalne,
- zwykła rejestracja przez aplikację tworzy konto `CLIENT`,
- konto `EMPLOYEE` lub `ADMIN` trzeba najpierw utworzyć w Firebase, a potem powiązać z rekordem `operators`.

Najprostszy lokalny bootstrap dla operatora lub administratora jest taki:

1. utwórz użytkownika w Firebase Authentication,
2. skopiuj jego `UID`,
3. dodaj rekord do tabeli `operators` z tym samym `firebase_uid`,
4. dopiero potem loguj się tym kontem w aplikacji.

Baza PostgreSQL sama nie tworzy kont Firebase i nie może tego zrobić SQL-em.

Bardziej ogólnie, PostgreSQL jest dostępny pod:

- Host: localhost
- Port: 5433
- Database: drobnyd
- User: postgres
- Password: postgres
