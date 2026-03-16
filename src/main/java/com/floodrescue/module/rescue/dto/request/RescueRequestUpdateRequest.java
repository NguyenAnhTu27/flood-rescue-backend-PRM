package com.floodrescue.module.rescue.dto.request;

import com.floodrescue.shared.enums.AttachmentFileType;
import com.floodrescue.shared.enums.RescuePriority;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RescueRequestUpdateRequest {

    @Min(value = 1, message = "Số người bị ảnh hưởng phải lớn hơn 0")
    private Integer affectedPeopleCount;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String addressText;

    private RescuePriority priority;

    private List<AttachmentRequest> attachments;

    @Getter
    @Setter
    public static class AttachmentRequest {
        @NotBlank(message = "URL file không được để trống")
        @Size(max = 500, message = "URL file không được vượt quá 500 ký tự")
        private String fileUrl;

        private AttachmentFileType fileType;
    }
}
