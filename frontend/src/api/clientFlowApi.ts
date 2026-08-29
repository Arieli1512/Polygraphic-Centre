import api from "./api";
import type { ApiSuccessResponse } from "../types/api";
import type {
  AdminOperator,
  AdminPrintingPoint,
  ClientWalletSnapshot,
  ClientWalletTopUpResult,
  CreateOrderPayload,
  EmployeeDownloadLinkResult,
  EmployeeInProgressResult,
  EmployeeIssueReportResult,
  EmployeeOrderDetails,
  EmployeeOrderWorkflow,
  EmployeeQueueOrder,
  ManagerConfiguration,
  ManagerExtraPricing,
  ManagerOpeningSlot,
  ManagerRateRule,
  OrderDetails,
  OrderReportSummary,
  OrderSummary,
  OrderWorkflow,
  OrderStatus,
  PriceEstimate,
  PriceEstimateRequest,
  PrintingPointDetails,
  PrintingPointSummary,
  UploadRequestPayload,
  UploadRequestResult,
} from "../types/clientFlow";

function unwrapData<T>(response: ApiSuccessResponse<T>): T {
  return response.data;
}

export async function fetchPrintingPoints(): Promise<PrintingPointSummary[]> {
  const response = await api.get<ApiSuccessResponse<PrintingPointSummary[]>>(
    "/v1/printing-points",
  );
  return unwrapData(response.data);
}

export async function fetchPrintingPointDetails(
  printingPointId: number,
): Promise<PrintingPointDetails> {
  const response = await api.get<ApiSuccessResponse<PrintingPointDetails>>(
    `/v1/printing-points/${printingPointId}`,
  );
  return unwrapData(response.data);
}

export async function calculateEstimate(
  payload: PriceEstimateRequest,
): Promise<PriceEstimate> {
  const response = await api.post<ApiSuccessResponse<PriceEstimate>>(
    "/v1/pricing/estimates",
    payload,
  );
  return unwrapData(response.data);
}

export async function createUploadRequest(
  payload: UploadRequestPayload,
): Promise<UploadRequestResult> {
  const response = await api.post<ApiSuccessResponse<UploadRequestResult>>(
    "/v1/files/upload-requests",
    payload,
  );
  return unwrapData(response.data);
}

export async function fetchClientWalletSnapshot(): Promise<ClientWalletSnapshot> {
  const response =
    await api.get<ApiSuccessResponse<ClientWalletSnapshot>>(
      "/v1/client/wallet",
    );
  return unwrapData(response.data);
}

export async function topUpClientWallet(
  amount: number,
): Promise<ClientWalletTopUpResult> {
  const response = await api.post<ApiSuccessResponse<ClientWalletTopUpResult>>(
    "/v1/client/wallet/top-ups",
    { amount },
  );
  return unwrapData(response.data);
}

export async function uploadFileToSignedUrl(
  uploadRequest: UploadRequestResult,
  file: File,
  onProgress?: (percent: number) => void,
): Promise<void> {
  await new Promise<void>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("PUT", uploadRequest.uploadUrl, true);
    xhr.timeout = 60_000;

    Object.entries(uploadRequest.uploadHeaders).forEach(([key, value]) => {
      xhr.setRequestHeader(key, value);
    });

    if (!uploadRequest.uploadHeaders["content-type"]) {
      xhr.setRequestHeader("Content-Type", uploadRequest.contentType);
    }

    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable || !onProgress) {
        return;
      }

      const percent = Math.min(
        100,
        Math.max(0, Math.round((event.loaded / event.total) * 100)),
      );
      onProgress(percent);
    };

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        onProgress?.(100);
        resolve();
        return;
      }

      reject(
        new Error(
          `Upload to storage failed with status ${xhr.status}: ${xhr.responseText || "unknown error"}`,
        ),
      );
    };

    xhr.onerror = () => {
      reject(
        new Error(
          "Network error while uploading file to storage. This is usually caused by bucket CORS mismatch, blocked preflight OPTIONS request, or connectivity issues.",
        ),
      );
    };

    xhr.ontimeout = () => {
      reject(
        new Error(
          "Timed out while uploading file to storage. Check network stability and bucket CORS settings.",
        ),
      );
    };

    xhr.onabort = () => {
      reject(new Error("File upload was aborted"));
    };

    xhr.send(file);
  });
}

export async function createClientOrder(
  payload: CreateOrderPayload,
): Promise<OrderWorkflow> {
  const response = await api.post<ApiSuccessResponse<OrderWorkflow>>(
    "/v1/client/orders",
    payload,
  );
  return unwrapData(response.data);
}

export interface OrderListFilters {
  status?: string;
  printingPointId?: number;
  sortBy?: "createdAtDesc" | "createdAtAsc" | "pickupAtAsc" | "pickupAtDesc";
}

export async function fetchClientOrders(
  filters: OrderListFilters = {},
): Promise<OrderSummary[]> {
  const response = await api.get<ApiSuccessResponse<OrderSummary[]>>(
    "/v1/client/orders",
    { params: filters },
  );
  return unwrapData(response.data);
}

export async function fetchClientOrderDetails(
  orderId: number,
): Promise<OrderDetails> {
  const response = await api.get<ApiSuccessResponse<OrderDetails>>(
    `/v1/client/orders/${orderId}`,
  );
  return unwrapData(response.data);
}

export async function cancelClientOrder(
  orderId: number,
): Promise<OrderWorkflow> {
  const response = await api.post<ApiSuccessResponse<OrderWorkflow>>(
    `/v1/client/orders/${orderId}/cancel`,
  );
  return unwrapData(response.data);
}

export async function fetchEmployeeQueue(
  status?: OrderStatus,
): Promise<EmployeeQueueOrder[]> {
  const response = await api.get<ApiSuccessResponse<EmployeeQueueOrder[]>>(
    "/v1/employee/orders",
    {
      params: status ? { status } : undefined,
    },
  );
  return unwrapData(response.data);
}

export async function fetchEmployeeOrderDetails(
  orderId: number,
): Promise<EmployeeOrderDetails> {
  const response = await api.get<ApiSuccessResponse<EmployeeOrderDetails>>(
    `/v1/employee/orders/${orderId}`,
  );
  return unwrapData(response.data);
}

export async function updateEmployeeOrderStatus(
  orderId: number,
  targetStatus: OrderStatus,
): Promise<EmployeeOrderWorkflow> {
  const response = await api.post<ApiSuccessResponse<EmployeeOrderWorkflow>>(
    `/v1/employee/orders/${orderId}/status`,
    { targetStatus },
  );
  return unwrapData(response.data);
}

export async function markEmployeeOrderInProgress(
  orderId: number,
): Promise<EmployeeInProgressResult> {
  const response = await api.post<ApiSuccessResponse<EmployeeInProgressResult>>(
    `/v1/employee/orders/${orderId}/in-progress`,
  );
  return unwrapData(response.data);
}

export async function reportEmployeeOrderIssue(
  orderId: number,
  reason: string,
): Promise<EmployeeIssueReportResult> {
  const response = await api.post<
    ApiSuccessResponse<EmployeeIssueReportResult>
  >(`/v1/employee/orders/${orderId}/issue-report`, { reason });
  return unwrapData(response.data);
}

export async function generateEmployeeDownloadLink(
  orderId: number,
): Promise<EmployeeDownloadLinkResult> {
  const response = await api.post<
    ApiSuccessResponse<EmployeeDownloadLinkResult>
  >(`/v1/employee/orders/${orderId}/download-link`);
  return unwrapData(response.data);
}

export async function fetchManagerConfiguration(): Promise<ManagerConfiguration> {
  const response = await api.get<ApiSuccessResponse<ManagerConfiguration>>(
    "/v1/manager/printing-point/configuration",
  );
  return unwrapData(response.data);
}

export async function updateManagerCapacity(
  hourlyOrderLimit: number,
): Promise<{ printingPointId: number; hourlyOrderLimit: number }> {
  const response = await api.put<
    ApiSuccessResponse<{ printingPointId: number; hourlyOrderLimit: number }>
  >("/v1/manager/printing-point/capacity", { hourlyOrderLimit });
  return unwrapData(response.data);
}

export async function createManagerRateRule(
  payload: ManagerRateRule,
): Promise<ManagerRateRule> {
  const response = await api.post<ApiSuccessResponse<ManagerRateRule>>(
    "/v1/manager/printing-point/rate-rules",
    payload,
  );
  return unwrapData(response.data);
}

export async function updateManagerRateRule(
  payload: ManagerRateRule,
): Promise<ManagerRateRule> {
  const response = await api.put<ApiSuccessResponse<ManagerRateRule>>(
    "/v1/manager/printing-point/rate-rules",
    payload,
  );
  return unwrapData(response.data);
}

export async function deleteManagerRateRule(
  paperType: string,
  format: string,
): Promise<void> {
  await api.delete<ApiSuccessResponse<{ deleted: boolean }>>(
    "/v1/manager/printing-point/rate-rules",
    { params: { paperType, format } },
  );
}

export async function updateManagerExtraPricing(
  payload: ManagerExtraPricing,
): Promise<ManagerExtraPricing> {
  const response = await api.put<ApiSuccessResponse<ManagerExtraPricing>>(
    "/v1/manager/printing-point/extra-pricing",
    payload,
  );
  return unwrapData(response.data);
}

export async function updateManagerOpeningHours(
  slots: ManagerOpeningSlot[],
): Promise<ManagerOpeningSlot[]> {
  const response = await api.put<ApiSuccessResponse<ManagerOpeningSlot[]>>(
    "/v1/manager/printing-point/opening-hours",
    { slots },
  );
  return unwrapData(response.data);
}

export async function fetchAdminPrintingPoints(): Promise<
  AdminPrintingPoint[]
> {
  const response = await api.get<ApiSuccessResponse<AdminPrintingPoint[]>>(
    "/v1/admin/printing-points",
  );
  return unwrapData(response.data);
}

export interface CreateAdminPrintingPointPayload {
  name: string;
  streetAddress: string;
  city: string;
  postalCode: string;
  country: string;
  hourlyOrderLimit: number;
}

export async function createAdminPrintingPoint(
  payload: CreateAdminPrintingPointPayload,
): Promise<AdminPrintingPoint> {
  const response = await api.post<ApiSuccessResponse<AdminPrintingPoint>>(
    "/v1/admin/printing-points",
    payload,
  );
  return unwrapData(response.data);
}

export async function updateAdminPrintingPoint(
  printingPointId: number,
  payload: CreateAdminPrintingPointPayload,
): Promise<AdminPrintingPoint> {
  const response = await api.put<ApiSuccessResponse<AdminPrintingPoint>>(
    `/v1/admin/printing-points/${printingPointId}`,
    payload,
  );
  return unwrapData(response.data);
}

export async function toggleAdminPrintingPointSkeleton(
  printingPointId: number,
  enabled: boolean,
): Promise<{ mode: string; note: string }> {
  const response = await api.post<
    ApiSuccessResponse<{ mode: string; note: string }>
  >(`/v1/admin/printing-points/${printingPointId}/toggle-enabled`, { enabled });
  return unwrapData(response.data);
}

export async function fetchAdminOperators(
  printingPointId?: number,
): Promise<AdminOperator[]> {
  const response = await api.get<ApiSuccessResponse<AdminOperator[]>>(
    "/v1/admin/operators",
    { params: printingPointId ? { printingPointId } : undefined },
  );
  return unwrapData(response.data);
}

export interface CreateAdminOperatorPayload {
  printingPointId: number;
  email: string;
  employeeNumber: string;
  role: "ADMIN" | "EMPLOYEE";
}

export interface CreateAdminOperatorResult {
  operatorId: number;
  printingPointId: number;
  firebaseUid: string;
  email: string;
  employeeNumber: string;
  role: "ADMIN" | "EMPLOYEE";
  status: "ACTIVE" | "BLOCKED";
  createdAt: string;
  updatedAt: string;
  inviteLink: string;
  inviteSent: boolean;
}

export async function createAdminOperator(
  payload: CreateAdminOperatorPayload,
): Promise<CreateAdminOperatorResult> {
  const response = await api.post<
    ApiSuccessResponse<CreateAdminOperatorResult>
  >("/v1/admin/operators", payload);
  return unwrapData(response.data);
}

export async function updateAdminOperator(
  operatorId: number,
  payload: { role?: "ADMIN" | "EMPLOYEE"; status?: "ACTIVE" | "BLOCKED" },
): Promise<AdminOperator> {
  const response = await api.patch<ApiSuccessResponse<AdminOperator>>(
    `/v1/admin/operators/${operatorId}`,
    payload,
  );
  return unwrapData(response.data);
}

export interface GenerateOrderReportPayload {
  from: string;
  to: string;
  reportType: "DAILY" | "MONTHLY";
  printingPointId?: number;
}

export async function generateOrderReport(
  payload: GenerateOrderReportPayload,
): Promise<OrderReportSummary> {
  const response = await api.post<ApiSuccessResponse<OrderReportSummary>>(
    "/v1/admin/reports/orders-summary",
    payload,
  );
  return unwrapData(response.data);
}

export function buildOrderReportExportUrl(
  payload: GenerateOrderReportPayload,
  format: "PDF" | "CSV",
): string {
  const params = new URLSearchParams({
    from: payload.from,
    to: payload.to,
    reportType: payload.reportType,
    format,
  });

  if (payload.printingPointId != null) {
    params.set("printingPointId", String(payload.printingPointId));
  }

  return `/api/v1/admin/reports/orders-summary/export?${params.toString()}`;
}

export async function exportOrderReport(
  payload: GenerateOrderReportPayload,
  format: "PDF" | "CSV",
): Promise<Blob> {
  const response = await api.get<Blob>(
    "/v1/admin/reports/orders-summary/export",
    {
      params: {
        from: payload.from,
        to: payload.to,
        reportType: payload.reportType,
        format,
        ...(payload.printingPointId != null
          ? { printingPointId: payload.printingPointId }
          : {}),
      },
      responseType: "blob",
    },
  );
  return response.data;
}
