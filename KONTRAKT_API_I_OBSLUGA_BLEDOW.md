# Kontrakt API i obsluga bledow (zadanie 1.2)

## 1. Cel dokumentu

Ten dokument definiuje dojrzaly standard API dla projektu:

- prosty i przewidywalny dla klienta API,
- zgodny z praktykami rynkowymi,
- rozszerzalny bez lamania kompatybilnosci,
- przyjazny dla uzytkownika koncowego i diagnostyki technicznej.

Dokument jest po polsku, ale nazwy techniczne (endpointy, pola JSON, statusy) sa po angielsku.

## 2. Zasady ogolne (high-level)

1. HTTP status code jest glownym sygnalem powodzenia lub bledu operacji.
2. Odpowiedzi sukcesu zwracaja dane domenowe i metadane techniczne.
3. Odpowiedzi bledow sa oparte o RFC Problem Details (`application/problem+json`) z rozszerzeniami domenowymi.
4. Kazda odpowiedz ma `requestId` (korelacja), a blad dodatkowo `traceId` (diagnostyka rozproszona).
5. API jest wersjonowane i rozwijane kompatybilnie wstecz.

## 3. Zasady nazewnictwa REST API (angielski)

### 3.1 Konwencja sciezek

- Prefiks wersji: `/api/v1`
- Nazwy zasobow po angielsku i w liczbie mnogiej: `/orders`, `/printing-points`, `/clients`
- Kebab-case: `/wallet-top-ups`, `/rate-sheets`
- Pojedynczy zasob: `/orders/{orderId}`
- Relacje podrzedne: `/printing-points/{printingPointId}/operators`
- Bez czasownikow w URI (wyjatki tylko dla akcji biznesowych trudnych do modelowania CRUD, np. `/orders/{orderId}/cancel`)

### 3.2 Metody HTTP

- `GET`: odczyt
- `POST`: utworzenie zasobu / akcja biznesowa
- `PUT`: pelna podmiana
- `PATCH`: czesciowa aktualizacja
- `DELETE`: usuniecie/dezaktywacja

### 3.3 Przykladowe endpointy

- `GET /api/v1/printing-points`
- `GET /api/v1/printing-points/{printingPointId}`
- `GET /api/v1/clients/{clientId}/wallet`
- `POST /api/v1/clients/{clientId}/wallet-top-ups`
- `GET /api/v1/orders`
- `POST /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `PATCH /api/v1/orders/{orderId}`
- `POST /api/v1/orders/{orderId}/cancel`
- `PATCH /api/v1/orders/{orderId}/status`
- `POST /api/v1/files/upload-requests`

## 4. Kontrakt odpowiedzi sukcesu

## 4.1 Zasada

W sukcesie zwracamy dane domenowe i metadane.

### 4.2 Typowe kody HTTP dla sukcesu

- `200 OK`: odczyt lub aktualizacja
- `201 Created`: utworzenie nowego zasobu
- `202 Accepted`: operacja asynchroniczna przyjeta do przetworzenia
- `204 No Content`: sukces bez ciala odpowiedzi

### 4.3 Przyklad sukcesu (`201 Created`)

```json
{
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

### 4.4 Przyklad listy z paginacja

```json
{
  "data": [
    {
      "orderId": "ord_01JX9YQ4",
      "status": "PENDING"
    },
    {
      "orderId": "ord_01JX9YQ5",
      "status": "READY"
    }
  ],
  "meta": {
    "requestId": "f3f79480-2d52-41ca-b0f7-f7a9fcf742ae",
    "page": 0,
    "size": 20,
    "totalItems": 2,
    "totalPages": 1,
    "timestamp": "2026-04-14T11:50:12Z"
  },
  "links": {
    "self": "/api/v1/orders?page=0&size=20",
    "next": null,
    "prev": null
  }
}
```

## 5. Kontrakt odpowiedzi bledu (Problem Details)

### 5.1 Zasada

Format bledu opieramy o `application/problem+json` (RFC Problem Details), rozszerzony o pola domenowe i pola diagnostyczne.

### 5.2 Obowiazkowe pola

- `type`: URI typu bledu (np. `https://api.polygraphic-centre.dev/problems/validation-error`)
- `title`: krotka nazwa bledu
- `status`: HTTP status code
- `detail`: opis techniczny
- `instance`: URI lub sciezka wystapienia bledu
- `code`: stabilny kod aplikacyjny (dla frontendu i automatyki)
- `userMessage`: prosty komunikat dla uzytkownika
- `action`: wskazowka "co dalej"
- `requestId`: identyfikator korelacyjny
- `traceId`: identyfikator sledzenia rozproszonego
- `timestamp`: czas wystapienia bledu (UTC)

### 5.3 Pola opcjonalne

- `errors`: lista szczegolow walidacji
- `retryable`: czy blad mozna ponowic automatycznie (`true/false`)
- `docs`: link do dokumentacji bledu

### 5.4 Przyklad bledu walidacji (`422`)

```json
{
  "type": "https://api.polygraphic-centre.dev/problems/validation-error",
  "title": "Validation Error",
  "status": 422,
  "detail": "Validation failed for request body.",
  "instance": "/api/v1/orders",
  "code": "VALIDATION_ERROR",
  "userMessage": "Nie udalo sie zapisac zamowienia, bo czesc danych jest niepoprawna.",
  "action": "Popraw oznaczone pola i sprobuj ponownie.",
  "errors": [
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
  "retryable": false,
  "requestId": "0f6b6ca8-95af-4e1f-9b0b-3e31f5f49f8e",
  "traceId": "3e7e26f4d3f498f1",
  "timestamp": "2026-04-14T11:44:00Z"
}
```

### 5.5 Przyklad bledu biznesowego (`409`)

```json
{
  "type": "https://api.polygraphic-centre.dev/problems/order-status-transition-not-allowed",
  "title": "Order Transition Not Allowed",
  "status": 409,
  "detail": "Order status transition is not allowed.",
  "instance": "/api/v1/orders/ord_01JX9YQ4/status",
  "code": "ORDER_STATUS_TRANSITION_NOT_ALLOWED",
  "userMessage": "Nie mozna wykonac tej zmiany statusu zamowienia.",
  "action": "Odswiez widok zamowienia i sprawdz jego aktualny status.",
  "errors": [
    {
      "field": "status",
      "issue": "transition from READY to APPROVED is forbidden",
      "userHint": "Skontaktuj sie z obsluga, jesli to wyglada na pomylke."
    }
  ],
  "retryable": false,
  "requestId": "2c505f98-bbde-45d6-9361-27fefd102c52",
  "traceId": "31ab5c09f2a0d7cc",
  "timestamp": "2026-04-14T11:45:10Z"
}
```

### 5.6 Przyklad bledu autoryzacji (`403`)

```json
{
  "type": "https://api.polygraphic-centre.dev/problems/access-denied",
  "title": "Access Denied",
  "status": 403,
  "detail": "Access denied for this resource.",
  "instance": "/api/v1/orders/ord_01JX9YQ4",
  "code": "ACCESS_DENIED",
  "userMessage": "Nie masz uprawnien do wykonania tej operacji.",
  "action": "Zaloguj sie na konto z odpowiednia rola lub skontaktuj sie z administratorem.",
  "retryable": false,
  "requestId": "5f8af83f-e11d-4a3b-a786-d5e8f84d53b4",
  "traceId": "f84e0ca10f503bcc",
  "timestamp": "2026-04-14T11:46:01Z"
}
```

## 6. HTTP status + kody aplikacyjne

### 6.1 Mapa statusow HTTP

- `400 Bad Request`: niepoprawna skladnia / brak wymaganych danych
- `401 Unauthorized`: brak waznego tokenu
- `403 Forbidden`: brak uprawnien
- `404 Not Found`: zasob nie istnieje
- `409 Conflict`: konflikt stanu biznesowego
- `422 Unprocessable Entity`: blad walidacji
- `429 Too Many Requests`: limit zapytan przekroczony
- `500 Internal Server Error`: blad nieoczekiwany
- `503 Service Unavailable`: chwilowa niedostepnosc uslugi zewnetrznej

### 6.2 Przykladowe kody aplikacyjne

- `VALIDATION_ERROR`
- `RESOURCE_NOT_FOUND`
- `ACCESS_DENIED`
- `AUTH_TOKEN_INVALID`
- `AUTH_TOKEN_EXPIRED`
- `ORDER_STATUS_TRANSITION_NOT_ALLOWED`
- `PICKUP_TIME_UNAVAILABLE`
- `FILE_UPLOAD_NOT_ALLOWED`
- `EXTERNAL_SERVICE_UNAVAILABLE`
- `INTERNAL_ERROR`

## 7. Observability, tracing i korelacja

### 7.1 Naglowki przychodzace i wychodzace

- `traceparent` (W3C Trace Context) - preferowany
- `tracestate` (opcjonalnie)
- `x-request-id` (fallback i korelacja biznesowa)

### 7.2 Zasady

- Jesli klient przesle `x-request-id`, backend go propaguje.
- Jesli brak identyfikatora, backend generuje `requestId`.
- `traceId` powinno byc zgodne z aktywnym spanem trace.
- `requestId` i `traceId` trafiaja do logow, metryk i odpowiedzi bledow.

## 8. Bezpieczenstwo i prywatnosc odpowiedzi

- Brak stack trace i danych wewnetrznych w odpowiedziach dla klienta.
- Brak ujawniania sekretow, tokenow, hasel i kluczy.
- Komunikaty dla uzytkownika sa zrozumiale, ale nie zdradzaja wrazliwych szczegolow.
- Pelnie techniczne detale sa dostepne tylko w logach serwera.

## 9. Kompatybilnosc i ewolucja kontraktu

### 9.1 Zasady kompatybilnosci

- Nie usuwamy pol bez deprecjacji.
- Nowe pola dodajemy jako opcjonalne.
- Zmiany niekompatybilne robimy w nowej wersji (`/api/v2`).

### 9.2 Deprecation policy

- Oznaczamy pola/endpointy jako deprecated w OpenAPI.
- Uzywamy naglowkow `Deprecation` i `Sunset` dla konca wsparcia.
- Zapewniamy okres przejsciowy i komunikat migracyjny.

### 9.3 Idempotencja i retry

- Dla wrazliwych `POST` (np. finansowych) wymagamy `Idempotency-Key`.
- Odpowiedz bledu moze zawierac `retryable=true`, jesli bezpieczne jest ponowienie.

## 10. OpenAPI/Swagger - minimalny szkic

Minimalny zestaw do wdrozenia:

- OpenAPI `3.1`
- `ApiDataResponse` (sukces)
- `ProblemDetails` (blad)
- `ValidationProblemDetails` (blad walidacji)
- Reuzywalne odpowiedzi dla `400/401/403/404/409/422/429/500/503`

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
                $ref: '#/components/schemas/ApiDataResponse'
        '422':
          description: Validation error
          content:
            application/problem+json:
              schema:
                $ref: '#/components/schemas/ValidationProblemDetails'
components:
  schemas:
    ApiDataResponse:
      type: object
      properties:
        data:
          type: object
        meta:
          type: object
    ProblemDetails:
      type: object
      required: [type, title, status, detail, instance, code, userMessage, action, requestId, traceId, timestamp]
    ValidationProblemDetails:
      allOf:
        - $ref: '#/components/schemas/ProblemDetails'
        - type: object
          properties:
            errors:
              type: array
              items:
                type: object
```

## 11. Definicja ukonczenia zadania 1.2

Zadanie 1.2 uznajemy za ukonczone, gdy:

- endpointy sa nazwane po angielsku i zgodnie z REST,
- HTTP status jest glownym sygnalem wyniku operacji,
- bledy sa zgodne z Problem Details i zawieraja informacje: co sie stalo, czego dotyczy i co dalej,
- tracing i korelacja (`requestId`, `traceId`, `traceparent`) sa wspierane,
- dokumentacja OpenAPI zawiera schematy sukcesu i bledu oraz odpowiedzi reuzywalne.
