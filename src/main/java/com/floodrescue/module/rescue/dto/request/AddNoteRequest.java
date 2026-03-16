package com.floodrescue.module.rescue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddNoteRequest {

    @NotBlank(message = "Ghi chú không được để trống")
    @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự")
    private String note;
}
