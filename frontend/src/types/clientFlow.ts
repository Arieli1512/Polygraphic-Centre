export type OrderStatus =
  | "PENDING"
  | "APPROVED"
  | "READY"
  | "PROBLEM_REPORTED"
  | "DISPENSED"
  | "CANCELLED";

export type PrintColorMode = "COLOR" | "GRAYSCALE";
export type PrintDuplex = "SINGLE_SIDED" | "DOUBLE_SIDED";
export type PrintOrientation = "PORTRAIT" | "LANDSCAPE";
export type PrintFinishing = "NONE" | "BINDING" | "STAPLING" | "COVER";

export interface PrintingPointSummary {
  printingPointId: number;
  name: string;
  streetAddress: string;
  city: string;
  postalCode: string;
  country: string;
  hourlyOrderLimit: number;
}

export interface OpeningHoursWindow {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface ExtraPricingSnapshot {
  bindingPrice: number;
  staplingPrice: number;
  coverPrice: number;
}

export interface RateOption {
  paperType: string;
  format: string;
  pagePrice: number;
}

export interface PrintingPointDetails extends PrintingPointSummary {
  openingHours: OpeningHoursWindow[];
  extraPricing: ExtraPricingSnapshot | null;
  rateOptions: RateOption[];
}

export interface PriceEstimateRequest {
  printingPointId: number;
  format: string;
  paperType: string;
  colorMode: PrintColorMode;
  duplex: PrintDuplex;
  orientation: PrintOrientation;
  finishing: PrintFinishing;
  copies: number;
  pageCount: number;
}

export interface PriceEstimate {
  printingPointId: number;
  unitPagePrice: number;
  billableSheets: number;
  copies: number;
  basePrice: number;
  extrasPrice: number;
  totalPrice: number;
  currency: string;
}

export interface UploadRequestPayload {
  fileName: string;
  contentType: string;
  fileSizeBytes: number;
}

export interface UploadRequestResult {
  uploadUrl: string;
  objectPath: string;
  contentType: string;
  fileSizeBytes: number;
  expiresAt: string;
  uploadHeaders: Record<string, string>;
}

export interface CreateOrderPayload extends PriceEstimateRequest {
  filePath: string;
  pickupAt: string;
}

export interface OrderWorkflow {
  orderId: number;
  clientId: number;
  printingPointId: number;
  status: OrderStatus;
  totalPrice: number;
  pickupAt: string;
}

export interface OrderSummary {
  orderId: number;
  printingPointId: number;
  fileName: string;
  totalPrice: number;
  status: OrderStatus;
  pickupAt: string;
  createdAt: string;
}

export interface PrintSettingsSnapshot {
  format: string;
  paperType: string;
  colorMode: PrintColorMode;
  duplex: PrintDuplex;
  orientation: PrintOrientation;
  finishing: PrintFinishing;
  copies: number;
}

export interface OrderStatusEvent {
  status: OrderStatus;
  changedAt: string;
  note: string;
}

export interface OrderDetails {
  orderId: number;
  printingPointId: number;
  fileName: string;
  filePath: string;
  pageCount: number;
  totalPrice: number;
  status: OrderStatus;
  pickupAt: string;
  createdAt: string;
  updatedAt: string;
  printSettings: PrintSettingsSnapshot;
  timeline: OrderStatusEvent[];
}

export interface EmployeeQueueOrder {
  orderId: number;
  clientId: number;
  clientName: string;
  fileName: string;
  pageCount: number;
  status: OrderStatus;
  pickupAt: string;
  totalPrice: number;
  inProgress: boolean;
}

export interface EmployeeOrderDetails {
  orderId: number;
  clientId: number;
  clientName: string;
  clientEmail: string;
  printingPointId: number;
  fileName: string;
  filePath: string;
  pageCount: number;
  totalPrice: number;
  status: OrderStatus;
  pickupAt: string;
  createdAt: string;
  updatedAt: string;
  inProgress: boolean;
  printedAt: string | null;
  printSettings: PrintSettingsSnapshot;
}

export interface EmployeeOrderWorkflow {
  orderId: number;
  clientId: number;
  printingPointId: number;
  status: OrderStatus;
  totalPrice: number;
  pickupAt: string;
}

export interface EmployeeInProgressResult {
  orderId: number;
  operatorId: number;
  printedAt: string;
}

export interface EmployeeIssueReportResult {
  orderId: number;
  status: OrderStatus;
  reason: string;
  action: string;
}

export interface EmployeeDownloadLinkResult {
  orderId: number;
  downloadUrl: string;
  expiresAt: string;
  auditId: string;
}

export interface ManagerRateRule {
  paperType: string;
  format: string;
  pagePrice: number;
}

export interface ManagerOpeningSlot {
  dayOfWeek: number;
  enabled: boolean;
  startTime: string | null;
  endTime: string | null;
}

export interface ManagerExtraPricing {
  bindingPrice: number;
  staplingPrice: number;
  coverPrice: number;
}

export interface ManagerConfiguration {
  printingPointId: number;
  name: string;
  streetAddress: string;
  city: string;
  postalCode: string;
  country: string;
  hourlyOrderLimit: number;
  rateRules: ManagerRateRule[];
  openingHours: ManagerOpeningSlot[];
  extraPricing: ManagerExtraPricing;
}

export interface AdminPrintingPoint {
  printingPointId: number;
  name: string;
  streetAddress: string;
  city: string;
  postalCode: string;
  country: string;
  hourlyOrderLimit: number;
}

export interface AdminOperator {
  operatorId: number;
  printingPointId: number;
  firebaseUid: string;
  email: string;
  employeeNumber: string;
  role: "ADMIN" | "EMPLOYEE";
  status: "ACTIVE" | "BLOCKED";
  createdAt: string;
  updatedAt: string;
}

export interface OrderReportBucket {
  period: string;
  orderCount: number;
  revenue: number;
}

export interface OrderReportSummary {
  from: string;
  to: string;
  reportType: "DAILY" | "MONTHLY";
  totalOrders: number;
  totalRevenue: number;
  averageLeadTimeMinutes: number;
  buckets: OrderReportBucket[];
}
