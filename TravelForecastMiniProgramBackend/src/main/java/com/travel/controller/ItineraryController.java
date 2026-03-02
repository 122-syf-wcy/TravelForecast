package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.Itinerary;
import com.travel.entity.ItineraryItem;
import com.travel.mapper.ItineraryMapper;
import com.travel.mapper.ItineraryItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryMapper itineraryMapper;
    private final ItineraryItemMapper itemMapper;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam Long userId) {
        LambdaQueryWrapper<Itinerary> qw = new LambdaQueryWrapper<>();
        qw.eq(Itinerary::getUserId, userId).orderByDesc(Itinerary::getCreatedAt);
        List<Itinerary> itineraries = itineraryMapper.selectList(qw);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Itinerary it : itineraries) {
            Map<String, Object> map = new HashMap<>();
            map.put("itinerary", it);
            LambdaQueryWrapper<ItineraryItem> iqw = new LambdaQueryWrapper<>();
            iqw.eq(ItineraryItem::getItineraryId, it.getId()).orderByAsc(ItineraryItem::getDayNum).orderByAsc(ItineraryItem::getSortOrder);
            map.put("items", itemMapper.selectList(iqw));
            result.add(map);
        }
        return Result.success(result);
    }

    @PostMapping("/create")
    public Result<Itinerary> create(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String title = body.getOrDefault("title", "我的行程").toString();
        int days = body.containsKey("days") ? Integer.parseInt(body.get("days").toString()) : 1;

        Itinerary it = new Itinerary();
        it.setUserId(userId);
        it.setTitle(title);
        it.setDays(days);
        it.setStatus("planning");
        itineraryMapper.insert(it);

        if (body.containsKey("items")) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            int order = 0;
            for (Map<String, Object> item : items) {
                ItineraryItem ii = new ItineraryItem();
                ii.setItineraryId(it.getId());
                ii.setDayNum(item.containsKey("dayNum") ? Integer.parseInt(item.get("dayNum").toString()) : 1);
                ii.setTimeSlot(item.getOrDefault("timeSlot", "").toString());
                ii.setTitle(item.getOrDefault("title", "").toString());
                ii.setDescription(item.containsKey("description") ? item.get("description").toString() : null);
                ii.setScenicId(item.containsKey("scenicId") ? Long.valueOf(item.get("scenicId").toString()) : null);
                ii.setImageUrl(item.containsKey("imageUrl") ? item.get("imageUrl").toString() : null);
                ii.setSortOrder(order++);
                itemMapper.insert(ii);
            }
        }

        return Result.success(it);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        itineraryMapper.deleteById(id);
        LambdaQueryWrapper<ItineraryItem> qw = new LambdaQueryWrapper<>();
        qw.eq(ItineraryItem::getItineraryId, id);
        itemMapper.delete(qw);
        return Result.success("已删除");
    }
}