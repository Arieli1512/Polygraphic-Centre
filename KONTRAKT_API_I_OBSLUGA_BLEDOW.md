# Kontrakt API i obsluga bledow (zadanie 1.2)

## 1. Cel dokumentu

Ten dokument ustala wspolne zasady dla API backendu:

- jak nazywamy sciezki endpointow,
- jak wyglada odpowiedz sukcesu,
- jak wyglada odpowiedz bledu,
- jakie informacje dostaje uzytkownik i zespol techniczny,
- jak dokumentujemy API w OpenAPI/Swagger.

Dokument jest po polsku, ale nazwy techniczne (endpointy, pola JSON, statusy) sa po angielsku.

## 2. Zasady nazewnictwa REST API (angielski)

### 2.1 Bazowa konwencja

- Wersjonowanie przez prefiks: /api/v1
- Nazwy zasobow po angielsku i w liczbie mnogiej: /orders, /printing-points, /clients
- Kebab-case w segmentach sciezki: /wallet-top-ups, /rate-sheets
- Operacje na pojedynczym zasobie: /orders/{orderId}
- Operacje podrzedne: /orders/{orderId}/status-history
- Brak czasownikow w sciezkach, jesli da sie to wyrazic metoda HTTP

### 2.2 Metody HTTP

- GET: odczyt
- POST: utworzenie zasobu
- PUT: pelna podmiana
- PATCH: czesciowa aktualizacja
- DELETE: usuniecie lub dezaktywacja

### 2.3 Przykladowe endpointy

- GET /api/v1/printing-points
- GET /api/v1/printing-points/{printingPointId}
- GET /api/v1/clients/{clientId}/wallet
- POST /api/v1/clients/{clientId}/wallet-top-ups
- GET /api/v1/orders
- POST /api/v1/orders
- GET /api/v1/orders/{orderId}
- PATCH /api/v1/orders/{orderId}
- POST /api/v1/orders/{orderId}/cancel
- PATCH /api/v1/orders/{orderId}/status
- GET /api/v1/printing-points/{printingPointId}/operators
- GET /api/v1/printing-points/{printingPointId}/rate-sheets
- POST /api/v1/files/upload-requests

## 3. Kontrakt odpowiedzi sukcesu

### 3.1 Wrapper sukcesu

Kazda odpowiedz sukcesu zwraca wspolny wrapper:

- success: true
- message: krotka informacja dla uzytkownika
- data: wlasciwy obiekt lub lista
- meta: dane pomocnicze (np. paginacja, requestId)

### 3.2 Przyklad sukcesu

```json
{
  "success": true,
  "message": "Zamowienie zostalo utworzone.",
  "data": {
    "orderId": "ord_01JX9YQ4",
    "status": "PENDING",
    "pickupAt": "2026-04-20T10:30:00Z"
  },
  "meta": {
    "requestId": "a9b1c77f-22d4-48f3-a3a8-7d9f3f8de3c2",
    "timestamp": "2026-04-14T11:42:22Z"
  }
}
```

## 4. Kontrakt odpowiedzi bledu

### 4.1 Zasady ogolne (best practices)

- Blad ma byc zrozumialy dla uzytkownika nietechnicznego.
- Odpowiedz ma jasno pokazywac:
  - co sie stalo,
  - czego dotyczy problem,
  - co uzytkownik moze zrobic dalej.
- Odpowiedz ma zawierac informacje diagnostyczne dla wsparcia (requestId/traceId), ale bez wycieku danych wrazliwych.
- Uzywamy statusow HTTP zgodnie z semantyka bledu.

### 4.2 Minimalny schemat bledu

- success: false
- error.code: stabilny kod bledu do obslugi po stronie frontend
- error.message: krotki opis techniczny
- error.userMessage: prosty komunikat dla uzytkownika
- error.target: co dokladnie jest problemem (pole, zasob, operacja)
- error.action: co uzytkownik ma zrobic dalej
- error.details: lista szczegolow (np. walidacja pol)
- error.status: kod HTTP
- error.path: endpoint
- error.method: metoda HTTP
- error.timestamp: czas wystapienia bledu
- error.requestId: identyfikator do kontaktu z supportem
- error.traceId: identyfikator techniczny dla logow

### 4.3 Przyklad bledu walidacji (422)

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed for request body.",
    "userMessage": "Nie udalo sie zapisac zamowienia, bo czesc danych jest niepoprawna.",
    "target": "orderRequest",
    "action": "Popraw oznaczone pola i sprobuj ponownie.",
    "details": [
      {
        "field": "copies",
        "issue": "must be greater than 0",
        "userHint": "Podaj liczbe kopii wieksza od zera."
      },
      {
        "field": "pickupAt",
        "issue": "is not available",
        "userHint": "Wybierz inny termin odbioru."
      }
    ],
    "status": 422,
    "path": "/api/v1/orders",
    "method": "POST",
    "timestamp": "2026-04-14T11:44:00Z",
    "requestId": "0f6b6ca8-95af-4e1f-9b0b-3e31f5f49f8e",
    "traceId": "3e7e26f4d3f498f1"
  }
}
```

### 4.4 Przyklad bledu biznesowego (409)

```json
{
  "success": false,
  "error": {
    "code": "ORDER_STATUS_TRANSITION_NOT_ALLOWED",
    "message": "Order status transition is not allowed.",
    "userMessage": "Nie mozna wykonac tej zmiany statusu zamowienia.",
    "target": "order.status",
    "action": "Odswiez widok zamowienia i sprawdz jego aktualny status.",
    "details": [
      {
        "field": "status",
        "issue": "transition from READY to APPROVED is forbidden",
        "userHint": "Skontaktuj sie z obsluga, jesli to wyglada na pomylke."
      }
    ],
    "status": 409,
    "path": "/api/v1/orders/ord_01JX9YQ4/status",
    "method": "PATCH",
    "timestamp": "2026-04-14T11:45:10Z",
    "requestId": "2c505f98-bbde-45d6-9361-27fefd102c52",
    "traceId": "31ab5c09f2a0d7cc"
  }
}
```

### 4.5 Przyklad bledu autoryzacji (403)

```json
{
  "success": false,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "Access denied for this resource.",
    "userMessage": "Nie masz uprawnien do wykonania tej operacji.",
    "target": "orders/{orderId}",
    "action": "Zaloguj sie na konto z odpowiednia rola lub skontaktuj sie z administratorem.",
    "details": [],
    "status": 403,
    "path": "/api/v1/orders/ord_01JX9YQ4",
    "method": "GET",
    "timestamp": "2026-04-14T11:46:01Z",
    "requestId": "5f8af83f-e11d-4a3b-a786-d5e8f84d53b4",
    "traceId": "f84e0ca10f503bcc"
  }
}
```

## 5. Mapa kodow HTTP i kodow aplikacyjnych

### 5.1 Statusy HTTP

- 400 Bad Request: niepoprawna skladnia lub brak wymaganych danych
- 401 Unauthorized: brak waznego tokenu
- 403 Forbidden: brak uprawnienia
- 404 Not Found: zasob nie istnieje
- 409 Conflict: konflikt stanu biznesowego
- 422 Unprocessable Entity: blad walidacji danych
- 429 Too Many Requests: przekroczony limit zapytan
- 500 Internal Server Error: blad nieoczekiwany
- 503 Service Unavailable: chwilowa niedostepnosc uslugi zewnetrznej

### 5.2 Przykladowe kody aplikacyjne

- VALIDATION_ERROR
- RESOURCE_NOT_FOUND
- ACCESS_DENIED
- AUTH_TOKEN_INVALID
- AUTH_TOKEN_EXPIRED
- ORDER_STATUS_TRANSITION_NOT_ALLOWED
- PICKUP_TIME_UNAVAILABLE
- FILE_UPLOAD_NOT_ALLOWED
- EXTERNAL_SERVICE_UNAVAILABLE
- INTERNAL_ERROR

## 6. Reguly bezpieczenstwa dla bledow

- Nie zwracamy stack trace do klienta.
- Nie zwracamy danych tajnych (tokeny, sekrety, hasla, klucze).
- Szczegoly techniczne trafiaja do logow po stronie serwera.
- requestId i traceId musza byc obecne we wszystkich odpowiedziach bledow.

## 7. Kontrakt paginacji i filtrowania (zalecany)

Dla list uzywamy:

- query params: page, size, sort
- meta.page: numer strony
- meta.size: rozmiar strony
- meta.totalItems: liczba rekordow
- meta.totalPages: liczba stron

Przyklad:

- GET /api/v1/orders?page=0&size=20&sort=createdAt,desc

## 8. Szkielet OpenAPI/Swagger

Minimalny zestaw do wdrozenia:

- OpenAPI 3.1
- Jedno wspolne schema: ApiSuccessResponse
- Jedno wspolne schema: ApiErrorResponse
- Components/schemas dla glownej domeny (Order, PrintingPoint, Wallet, RateSheet, PrintSettings)
- Reuzywalne odpowiedzi bledu dla 400/401/403/404/409/422/500

Przykladowy szkic:

```yaml
openapi: 3.1.0
info:
  title: Polygraphic Centre API
  version: 1.0.0
servers:
  - url: /api/v1
paths:
  /orders:
    post:
      summary: Create order
      responses:
        '201':
          description: Created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiSuccessResponse'
        '422':
          $ref: '#/components/responses/ValidationError'
components:
  responses:
    ValidationError:
      description: Validation error
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ApiErrorResponse'
  schemas:
    ApiSuccessResponse:
      type: object
    ApiErrorResponse:
      type: object
```

## 9. Definicja ukonczenia zadania 1.2

Zadanie 1.2 uznajemy za ukonczone, gdy:

- sciezki API sa nazwane po angielsku i zgodnie z REST,
- frontend i backend korzystaja z jednego schematu sukcesu,
- frontend i backend korzystaja z jednego schematu bledu,
- kazdy blad ma informacje: co sie stalo, czego dotyczy i co dalej,
- istnieje dzialajaca dokumentacja OpenAPI ze schematami odpowiedzi.
