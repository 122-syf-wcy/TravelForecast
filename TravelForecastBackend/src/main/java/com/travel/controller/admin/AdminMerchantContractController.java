package com.travel.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.dto.admin.AdminContractUpdateRequest;
import com.travel.entity.merchant.MerchantContract;
import com.travel.service.merchant.MerchantContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/merchant/contracts")
@RequiredArgsConstructor
public class AdminMerchantContractController {

    private final MerchantContractService contractService;

    /**
     * 管理员分页查询所有合同
     */
    @GetMapping
    public Result<Page<MerchantContract>> getContracts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        log.info("管理员查询合同列表: pageNum={}, pageSize={}, keyword={}, status={}", pageNum, pageSize, keyword, status);
        Page<MerchantContract> page = contractService.getAdminContractPage(pageNum, pageSize, keyword, status);
        return Result.success(page);
    }

    /**
     * 管理员查看合同详情
     */
    @GetMapping("/{contractId}")
    public Result<MerchantContract> getContractDetail(@PathVariable Long contractId) {
        log.info("管理员查看合同详情: contractId={}", contractId);
        MerchantContract contract = contractService.getContractById(contractId);
        if (contract == null) {
            return Result.error("合同不存在");
        }
        return Result.success(contract);
    }

    /**
     * 管理员更新合同（包括状态）
     */
    @PutMapping("/{contractId}")
    public Result<MerchantContract> updateContract(
            @PathVariable Long contractId,
            @RequestBody AdminContractUpdateRequest request) {

        log.info("管理员更新合同 {}: {}", contractId, request.getContractNo());
        MerchantContract updatedContract = contractService.updateContract(
                contractId,
                request.getContractNo(),
                request.getContractName(),
                request.getContractUrl(),
                request.getStartDate(),
                request.getEndDate(),
                request.getRemarks(),
                request.getStatus()
        );
        return Result.success(updatedContract, "合同更新成功");
    }

    /**
     * 管理员删除合同
     */
    @DeleteMapping("/{contractId}")
    public Result<Void> deleteContract(@PathVariable Long contractId) {
        log.info("管理员删除合同: {}", contractId);
        boolean deleted = contractService.deleteContract(contractId);
        if (!deleted) {
            return Result.error("删除失败，合同不存在或已被删除");
        }
        return Result.success(null, "合同删除成功");
    }

    /**
     * 管理员更新合同状态
     */
    @PutMapping("/{contractId}/status")
    public Result<Void> updateContractStatus(
            @PathVariable Long contractId,
            @RequestBody Map<String, String> statusUpdate) {
        String status = statusUpdate.get("status");
        if (status == null || status.isEmpty()) {
            return Result.error("状态不能为空");
        }
        log.info("管理员更新合同 {} 状态为: {}", contractId, status);
        boolean updated = contractService.updateContractStatus(contractId, status);
        if (!updated) {
            return Result.error("更新状态失败，合同不存在");
        }
        return Result.success(null, "合同状态更新成功");
    }
}
