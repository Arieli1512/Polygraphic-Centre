## O Dockerze

Docker wykorzystuje mechanizmy izolacji dostępne w jądrze Linuxa (m.in. namespaces i cgroups), dzięki którym możliwe jest uruchamianie aplikacji w odseparowanych środowiskach zwanych kontenerami. W porównaniu do maszyn wirtualnych kontenery są znacznie lżejsze, uruchamiają się szybciej i zużywają mniej zasobów, ponieważ współdzielą jądro systemu operacyjnego.

Na systemie Linux Docker działa bezpośrednio na jądrze systemu. Na systemach Windows i macOS wymagany jest Docker Desktop, który uruchamia niewielką maszynę wirtualną z Linuxem. Wszystkie kontenery działają wewnątrz tej maszyny, dzięki czemu zachowują się identycznie niezależnie od używanego systemu operacyjnego.

Aplikacje uruchamiane przez Dockera są dostarczane w postaci obrazów (images). Obraz zawiera kompletną konfigurację potrzebną do uruchomienia danego programu. Przykładowo obraz postgres:18 zawiera gotową instalację PostgreSQL 18. Po pobraniu obrazu Docker może utworzyć na jego podstawie kontener i uruchomić działającą instancję bazy danych.

## Jak utworzyć bazę danych

Wystarczy zainstalować **Docker** lub **Docker Desktop** (Windows / macOS) i będąc w głównym folderze projektu uruchomić `docker compose up postgres`

To utworzy bazę danych `drobnyd` z użytkownikiem i hasłem `postgres`, `postgres` a sam postgres będzie nasłuchiwał na porcie `5433`.

Jedynie pierwsze wywołanie utworzy samą bazę oraz wywoła skrypty SQL znajdujące się w folderze `db/init`, które to tworzą tabele oraz dodają seed (czyli dane początkowe / testowe).

Wszelkie zmiany jakich dokonacie w bazie danych będą zapisywane w Waszym lokalnym systemie plików i nie utracicie ich po restarcie.

## Resetowanie bazy danych

Gdybyście jednak chcieli zresetować bazę danych, to wywołajcie:
`docker compose down -v`. Flaga `-v` usuwa wolumen, czyli aktualny stan bazy danych. Przy ponownym wywołaniu `docker compose up` baza danych zostanie ponownie utworzona.

## Testowanie bazy danych

Najłatwiej przetestować bazę przez program **pgAdmin 4**.
Po instalacji należy zarejestrować nowy serwer i w konfiguracji wpisać:

![Rejestracja serwera](images/pgadmin-register-server.png)

Password: pgadmin

Po tym wystarczy otworzyć *Query Tool*, będąc w bazie danych *drobnyd*

![Query Tool](images/pgadmin-query-tool.png)







