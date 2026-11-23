package com.tecgensoft.http.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class PayInvoiceRequest {

    @Schema(description = "Payment method", requiredMode = Schema.RequiredMode.REQUIRED, example = "BKASH")
    @NotBlank(message = "Payment method is required")
    @Size(max = 32, message = "Payment method cannot exceed 32 characters")
    private String paymentMethod;

    @Schema(description = "Gateway transaction id", example = "TXN123456")
    @Size(max = 160, message = "Transaction id cannot exceed 160 characters")
    private String transactionId;
}
