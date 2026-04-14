package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.UserAddress;
import com.travel.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final UserAddressMapper addressMapper;

    @GetMapping("/list")
    public Result<List<UserAddress>> list(@RequestParam Long userId) {
        LambdaQueryWrapper<UserAddress> qw = new LambdaQueryWrapper<>();
        qw.eq(UserAddress::getUserId, userId)
          .orderByDesc(UserAddress::getIsDefault)
          .orderByDesc(UserAddress::getUpdatedAt);
        return Result.success(addressMapper.selectList(qw));
    }

    @PostMapping("/save")
    public Result<UserAddress> save(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long id = body.containsKey("id") && body.get("id") != null
                ? Long.valueOf(body.get("id").toString()) : null;

        UserAddress addr;
        if (id != null) {
            addr = addressMapper.selectById(id);
            if (addr == null || !addr.getUserId().equals(userId)) {
                return Result.error("地址不存在");
            }
        } else {
            addr = new UserAddress();
            addr.setUserId(userId);
            addr.setCreatedAt(LocalDateTime.now());
        }

        addr.setName(body.getOrDefault("name", "").toString());
        addr.setPhone(body.getOrDefault("phone", "").toString());
        addr.setProvince(body.getOrDefault("province", "").toString());
        addr.setCity(body.getOrDefault("city", "").toString());
        addr.setDistrict(body.getOrDefault("district", "").toString());
        addr.setDetail(body.getOrDefault("detail", "").toString());
        addr.setIsDefault(Boolean.parseBoolean(body.getOrDefault("isDefault", "false").toString()));
        addr.setUpdatedAt(LocalDateTime.now());

        // 如果设为默认，先取消其他默认
        if (Boolean.TRUE.equals(addr.getIsDefault())) {
            clearDefault(userId);
        }

        if (id != null) {
            addressMapper.updateById(addr);
        } else {
            // 第一个地址自动设为默认
            long count = addressMapper.selectCount(
                new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId));
            if (count == 0) {
                addr.setIsDefault(true);
            }
            addressMapper.insert(addr);
        }

        return Result.success(addr);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, @RequestParam Long userId) {
        UserAddress addr = addressMapper.selectById(id);
        if (addr == null || !addr.getUserId().equals(userId)) {
            return Result.error("地址不存在");
        }
        addressMapper.deleteById(id);
        return Result.success("已删除");
    }

    @PutMapping("/{id}/default")
    public Result<String> setDefault(@PathVariable Long id, @RequestParam Long userId) {
        UserAddress addr = addressMapper.selectById(id);
        if (addr == null || !addr.getUserId().equals(userId)) {
            return Result.error("地址不存在");
        }
        clearDefault(userId);
        addr.setIsDefault(true);
        addr.setUpdatedAt(LocalDateTime.now());
        addressMapper.updateById(addr);
        return Result.success("已设为默认");
    }

    private void clearDefault(Long userId) {
        LambdaQueryWrapper<UserAddress> qw = new LambdaQueryWrapper<>();
        qw.eq(UserAddress::getUserId, userId).eq(UserAddress::getIsDefault, true);
        List<UserAddress> defaults = addressMapper.selectList(qw);
        for (UserAddress a : defaults) {
            a.setIsDefault(false);
            addressMapper.updateById(a);
        }
    }
}
