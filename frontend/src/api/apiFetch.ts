// Pobieranie adresu URL z konfiguracji Vite (.env) lub fallback na lokalny serwer Spring Boot
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
//const BASE_URL = 'http://localhost:5000'  //Mock-Backend
const API_VERSION = 'v1'

// Interfejs błędu zgodny ze specyfikacją Problem Details (application/problem+json)
export interface ApiProblemDetails {
    type: string;
    title: string;
    status: number;
    detail: string;
    instance: string;
    code: string;
    userMessage: string;
    action?: string;
    requestId: string;
    traceId: string;
    timestamp: string;
    [key: string]: unknown; // Elastyczność na dodatkowe pola struktury błędu
}

// Rozszerzamy standardowy interfejs RequestInit o wygodne przekazywanie obiektu body
interface ApiFetchOptions extends RequestInit {
    bodyData?: unknown;
    idempotencyKey?: string;
}

/**
 * Generyczna funkcja do wykonywania zapytań HTTP do backendu.
 * Zapewnia automatyczne typowanie zwracanych danych, nagłówki oraz obsługę błędów.
 */
export async function apiFetch<T>(endpoint: string, options: ApiFetchOptions = {}): Promise<T> {
    const { bodyData, headers, idempotencyKey, ...customConfig } = options;

    const defaultHeaders: Record<string, string> = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
    };

    if (idempotencyKey) {
        defaultHeaders['Idempotency-Key'] = idempotencyKey;
    }

    const incomingHeaders = (headers as Record<string, string>) || {};
    if (!incomingHeaders['x-request-id'] && !incomingHeaders['X-Request-ID']) {
        defaultHeaders['X-Request-ID'] = crypto.randomUUID();
    }

    // 💡 MIEJSCE NA PRZYSZŁOŚĆ: Gdy wdrożymy autoryzację, w tym miejscu będziemy 
    // pobierać aktualny token z Firebase i wstrzykiwać go jako Bearer token:
    // const token = await getCurrentUserFirebaseToken();
    // if (token) {
    //     defaultHeaders['Authorization'] = `Bearer ${token}`;
    // }


    const config: RequestInit = {
        // Jeśli przekazujemy dane w bodyData, domyślnie ustawiamy metodę POST, w innym wypadku GET
        method: bodyData ? 'POST' : 'GET',
        ...customConfig,
        headers: {
            ...defaultHeaders,
            ...headers,
        },
    };
    // ✨ HANDLE FORMDATA VS JSON LOGIC HERE
    if (bodyData && (bodyData instanceof FormData)) {
        // Remove the application/json header so the browser defaults to multipart/form-data
        if (config.headers && typeof config.headers === 'object') {
            delete (config.headers as Record<string, string>)['Content-Type'];
        }
        config.body = bodyData; // Pass raw FormData directly

        const orderRaw = bodyData.get("order");
        if (orderRaw && typeof orderRaw === 'string') {
            const orderObj = JSON.parse(orderRaw);
            if (orderObj && orderObj.order_id) config.method = 'PUT';
        }
    } else {
        // Otherwise, keep application/json and stringify your standard object data
        config.body = bodyData ? JSON.stringify(bodyData) : undefined;
    }

    // Zabezpieczenie przed podwójnym slashem w adresie URL
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    const targetUrl = `${BASE_URL}/${API_VERSION}${cleanEndpoint}`;

    let response: Response;
    try {
        response = await fetch(targetUrl, config);
    } catch (error) {
        // Przekazujemy błąd dalej do obsługi np. przez TanStack Query (useQuery / useMutation)
        console.error(`[API Error] Zapytanie do ${endpoint} nie powiodło się:`, error);
        throw error;
    }

    // Obsługa błędnych odpowiedzi serwera (statusy poza zakresem 200-299)
    if (!response.ok) {
        let errorData: ApiProblemDetails | null;
        try {
            // Próba odczytania struktury błędu z backendu (np. standard RFC 7807)
            errorData = await response.json() as ApiProblemDetails;
        } catch {
            errorData = null;
        }

        // Logujemy pełny kontekst błędu w celach diagnostycznych
        if (errorData) {
            console.warn(
                `[API Problem Details] Kod błędu: ${errorData.code}, ` +
                `RequestID: ${errorData.requestId}, TraceID: ${errorData.traceId}, ` +
                `Szczegóły: ${errorData.detail}`
            );
        }

        // Wyciąganie komunikatu błędu dla użytkownika lub fallback do statusu HTTP
        const errorMessage = errorData?.userMessage || errorData?.detail || `Błąd serwera (Status ${response.status})`;
        // Tworzymy obiekt błędu i opcjonalnie doklejamy do niego pełne szczegóły z backendu
        const error = new Error(errorMessage);
        const errorWithDetails = error as Error & { problemDetails: ApiProblemDetails | null };
        errorWithDetails.problemDetails = errorData;

        throw errorWithDetails;
    }

    // Obsługa braku zawartości (np. HTTP 204 No Content dla mutacji usuwania/edycji)
    if (response.status === 204) {
        return {} as T;
    }

    // Zwracamy sparsowany obiekt o typie T
    return await response.json() as T;

}