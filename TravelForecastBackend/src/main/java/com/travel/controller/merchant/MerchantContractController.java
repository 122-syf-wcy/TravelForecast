package com.travel.controller.merchant;

import com.travel.common.Result;
import com.travel.dto.merchant.ContractUploadRequest;
import com.travel.entity.merchant.MerchantContract;
import com.travel.service.merchant.MerchantContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/merchant/contracts")
@RequiredArgsConstructor
public class MerchantContractController {

    private final MerchantContractService contractService;

    /**
     * 商家上传合同
     */
    @PostMapping
    public Result<MerchantContract> uploadContract(
            @RequestBody ContractUploadRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId) {

        if (userId == null) {
            return Result.error("未登录");
        }
        log.info("商家 {} 上传合同: {}", userId, request.getContractNo());
        MerchantContract contract = contractService.uploadContract(
                userId,
                request.getContractNo(),
                request.getContractName(),
                request.getContractUrl(),
                request.getStartDate(),
                request.getEndDate(),
                request.getRemarks()
        );
        return Result.success(contract, "合同上传成功");
    }

    /**
     * 商家查看自己的合同列表
     */
    @GetMapping
    public Result<List<MerchantContract>> getMyContracts(
            @RequestAttribute(value = "userId", required = false) Long userId) {

        if (userId == null) {
            return Result.error("未登录");
        }
        log.info("商家 {} 查询合同列表", userId);
        List<MerchantContract> contracts = contractService.getContractsByUserId(userId);
        return Result.success(contracts);
    }

    /**
     * 商家更新合同
     */
    @PutMapping("/{contractId}")
    public Result<MerchantContract> updateContract(
            @PathVariable Long contractId,
            @RequestBody ContractUploadRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId) {

        if (userId == null) {
            return Result.error("未登录");
        }

        MerchantContract existingContract = contractService.getContractById(contractId);
        if (existingContract == null || !existingContract.getUserId().equals(userId)) {
            return Result.error("无权修改该合同");
        }

        log.info("商家 {} 更新合同 {}: {}", userId, contractId, request.getContractNo());
        MerchantContract updatedContract = contractService.updateContract(
                contractId,
                request.getContractNo(),
                request.getContractName(),
                request.getContractUrl(),
                request.getStartDate(),
                request.getEndDate(),
                request.getRemarks(),
                existingContract.getStatus()
        );
        return Result.success(updatedContract, "合同更新成功");
    }

    /**
     * 商家删除合同
     */
    @DeleteMapping("/{contractId}")
    public Result<Void> deleteContract(
            @PathVariable Long contractId,
            @RequestAttribute(value = "userId", required = false) Long userId) {

        if (userId == null) {
            return Result.error("未登录");
        }

        MerchantContract existingContract = contractService.getContractById(contractId);
        if (existingContract == null || !existingContract.getUserId().equals(userId)) {
            return Result.error("无权删除该合同");
        }

        log.info("商家 {} 删除合同 {}", userId, contractId);
        contractService.deleteContract(contractId);
        return Result.success(null, "合同删除成功");
    }
}
