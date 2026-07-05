export interface ApiLinks {
    self: string;
    next: string | null;
    prev: string | null;
}

export interface ApiPageMeta {
    requestId: string;
    page: number;
    size: number;
    totalItems: number;
    totalPages: number;
    timestamp: string; // Instants are serialized as ISO-8601 strings in JSON
}

export interface ApiPageResponse<T> {
    data: T[]; // Maps directly to List<T> data from your Java record
    meta: ApiPageMeta;
    links: ApiLinks;
}