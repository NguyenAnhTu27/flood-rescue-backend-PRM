package com.floodrescue.module.asset.service;

import com.floodrescue.module.asset.dto.request.CreateAssetRequest;
import com.floodrescue.module.asset.dto.request.UpdateAssetRequest;
import com.floodrescue.module.asset.dto.response.AssetResponse;
import com.floodrescue.shared.enums.AssetStatus;
import com.floodrescue.shared.enums.AssetType;

import java.util.List;

public interface AssetService {

    AssetResponse createAsset(CreateAssetRequest request);

    AssetResponse getAssetById(Long id);

    AssetResponse updateAsset(Long id, UpdateAssetRequest request);

    void deleteAsset(Long id);

    AssetResponse changeStatus(Long id, AssetStatus newStatus);

    AssetResponse assignTeam(Long id, Long teamId);

    AssetResponse unassignTeam(Long id);

    List<AssetResponse> getAssets(AssetStatus status);

    List<AssetResponse> getAssetsFiltered(AssetStatus status, AssetType assetType, Long teamId);

    List<AssetResponse> getAssetsByTeam(Long teamId);
}
