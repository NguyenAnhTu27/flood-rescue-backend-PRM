package com.floodrescue.module.asset.service;

import com.floodrescue.module.asset.dto.request.CreateAssetRequest;
import com.floodrescue.module.asset.dto.request.AssetStatusUpdateRequest;
import com.floodrescue.module.asset.dto.response.AssetResponse;
import com.floodrescue.shared.enums.AssetStatus;

import java.util.List;

public interface AssetService {

    AssetResponse createAsset(CreateAssetRequest request);

    List<AssetResponse> getAssets(AssetStatus status);

    AssetResponse getAsset(Long id);

    AssetResponse updateAssetStatus(Long id, AssetStatusUpdateRequest request);
}
