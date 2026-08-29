# TODO GCS Integration Backlog

Ten plik agreguje oznaczenia TODO dotyczace docelowej integracji frontend + backend z Google Cloud Storage.

## S4-4 Upload pliku klienta

- TODO[GCS-INTEGRATION][S4-4] Backend signer:
  - backend/src/main/java/com/drobnyd/drobnyd/controller/FileUploadController.java
  - Zamienic placeholder uploadUrl na realny signed URL GET/PUT z polityka content-type/size.
- TODO[GCS-INTEGRATION][S4-4] Frontend upload:
  - frontend/src/pages/ClientNewOrderPage.tsx
  - Zamienic symulacje postepu na realny upload HTTP PUT do signed URL + mapowanie bledow providera.

## S5-5 Pobieranie pliku przez pracownika

- TODO[GCS-INTEGRATION][S5-5] Secure download:
  - backend/src/main/java/com/drobnyd/drobnyd/controller/EmployeeOrderController.java
  - Podmienic placeholder download URL na signed GET URL z GCS + weryfikacja istnienia obiektu.
- TODO[GCS-INTEGRATION][S5-5] Audit download:
  - backend/src/main/java/com/drobnyd/drobnyd/controller/EmployeeOrderController.java
  - Rozszerzyc audit o zapis trwaly (tabela/log sink) i korelacje z requestId/operatorId.
