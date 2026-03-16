package com.floodrescue.module.rescue.dto.response;

import com.floodrescue.shared.enums.AttachmentFileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentUploadResponse {

    private String fileUrl;
    private AttachmentFileType fileType;
}

