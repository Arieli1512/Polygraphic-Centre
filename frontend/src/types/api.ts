export interface ApiValidationIssue {
  field: string;
  issue: string;
  userHint: string;
}

export interface ApiProblemDetails {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  code: string;
  userMessage: string;
  action: string;
  requestId: string;
  traceId: string | null;
  timestamp: string;
  errors: ApiValidationIssue[] | null;
  retryable: boolean;
  docs: string | null;
}

export interface ApiResponseMeta {
  requestId: string;
  timestamp: string;
}

export interface ApiSuccessResponse<T> {
  data: T;
  meta: ApiResponseMeta;
}

export type ApiFieldErrors = Record<string, string>;

export class ApiClientError extends Error {
  status: number;
  code: string;
  userMessage: string;
  action: string;
  requestId: string;
  traceId: string | null;
  errors: ApiValidationIssue[] | null;
  retryable: boolean;

  constructor(problem: ApiProblemDetails) {
    super(problem.userMessage || problem.detail || problem.title);
    this.name = "ApiClientError";
    this.status = problem.status;
    this.code = problem.code;
    this.userMessage = problem.userMessage;
    this.action = problem.action;
    this.requestId = problem.requestId;
    this.traceId = problem.traceId;
    this.errors = problem.errors;
    this.retryable = problem.retryable;
  }
}

export function getApiFieldErrors(
  error: unknown,
  fieldAliases: Record<string, string> = {},
): ApiFieldErrors {
  if (!(error instanceof ApiClientError) || !error.errors?.length) {
    return {};
  }

  return error.errors.reduce<ApiFieldErrors>((accumulator, issue) => {
    const normalizedField = fieldAliases[issue.field] ?? issue.field;
    if (!normalizedField || accumulator[normalizedField]) {
      return accumulator;
    }

    accumulator[normalizedField] = issue.userHint || issue.issue;
    return accumulator;
  }, {});
}
