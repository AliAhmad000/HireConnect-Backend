package com.hireconnect.profile.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AddressRequest {
    @Schema(example = "12B")
    private String houseNo;
    @Schema(example = "MG Road")
    private String street;
    @Schema(example = "Bengaluru")
    private String city;
    @Schema(example = "Karnataka")
    private String state;
    @Schema(example = "India")
    private String country;

    @Min(value = 0, message = "Pincode cannot be negative")
    @Schema(example = "560001")
    private Integer pincode;

    @Schema(example = "CURRENT")
    private String addressType;
}
