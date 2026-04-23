package com.roadinspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadinspection.common.Result;
import com.roadinspection.entity.Road;
import com.roadinspection.entity.RoadSection;
import com.roadinspection.mapper.RoadMapper;
import com.roadinspection.mapper.RoadSectionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "道路管理")
@RestController
@RequestMapping("/road")
@RequiredArgsConstructor
public class RoadController {

    private final RoadMapper roadMapper;
    private final RoadSectionMapper roadSectionMapper;
    
    @Value("${app.storage.base-path:./storage}")
    private String basePath;

    @Operation(summary = "分页查询道路列表")
    @GetMapping("/list")
    public Result<Page<Road>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String roadName,
            @RequestParam(required = false) String region) {
        
        Page<Road> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Road> wrapper = new LambdaQueryWrapper<>();
        
        if (roadName != null && !roadName.isEmpty()) {
            wrapper.like(Road::getRoadName, roadName);
        }
        if (region != null && !region.isEmpty()) {
            wrapper.eq(Road::getRegion, region);
        }
        wrapper.eq(Road::getDeleted, 0);
        wrapper.orderByDesc(Road::getCreateTime);
        
        return Result.success(roadMapper.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "获取所有路段（用于监控中心）")
    @GetMapping("/all-sections")
    public Result<List<Map<String, Object>>> getAllSections() {
        try {
            LambdaQueryWrapper<RoadSection> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RoadSection::getDeleted, 0);
            wrapper.orderByAsc(RoadSection::getRoadId);
            
            List<RoadSection> sections = roadSectionMapper.selectList(wrapper);
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (RoadSection section : sections) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", section.getId());
                item.put("roadId", section.getRoadId());
                item.put("sectionName", section.getSectionName());
                item.put("sectionCode", section.getSectionCode());
                
                Road road = roadMapper.selectById(section.getRoadId());
                if (road != null) {
                    item.put("roadName", road.getRoadName());
                    item.put("roadCode", road.getRoadCode());
                    
                    String roadFolderName = sanitizeFolderName(
                        (road.getRoadCode() != null ? road.getRoadCode() : road.getId()) + "_" + road.getRoadName()
                    );
                    String sectionFolderName = sanitizeFolderName(
                        (section.getSectionCode() != null ? section.getSectionCode() : section.getId()) + "_" + section.getSectionName()
                    );
                    String videoFolderPath = basePath + "/" + roadFolderName + "/" + sectionFolderName;
                    item.put("videoFolderPath", videoFolderPath);
                } else {
                    item.put("videoFolderPath", null);
                }
                
                result.add(item);
            }
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.success(new ArrayList<>());
        }
    }

    @Operation(summary = "获取道路详情")
    @GetMapping("/{id}")
    public Result<Road> getById(@PathVariable Long id) {
        return Result.success(roadMapper.selectById(id));
    }

    @Operation(summary = "新增道路")
    @PostMapping
    public Result<Void> save(@RequestBody Road road) {
        try {
            roadMapper.insert(road);
            return Result.success();
        } catch (Exception e) {
            log.error("新增道路失败: ", e);
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                return Result.error(400, "道路编码已存在，请使用其他编码");
            }
            return Result.error(500, "新增道路失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新道路")
    @PutMapping
    public Result<Void> update(@RequestBody Road road) {
        try {
            roadMapper.updateById(road);
            return Result.success();
        } catch (Exception e) {
            log.error("更新道路失败: ", e);
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                return Result.error(400, "道路编码已存在，请使用其他编码");
            }
            return Result.error(500, "更新道路失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除道路")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roadMapper.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "获取道路下的路段列表")
    @GetMapping("/{roadId}/sections")
    public Result<List<RoadSection>> getSections(@PathVariable Long roadId) {
        LambdaQueryWrapper<RoadSection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoadSection::getRoadId, roadId);
        wrapper.eq(RoadSection::getDeleted, 0);
        return Result.success(roadSectionMapper.selectList(wrapper));
    }

    @Operation(summary = "分页查询路段列表")
    @GetMapping("/section/list")
    public Result<Page<RoadSection>> sectionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long roadId) {
        
        Page<RoadSection> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RoadSection> wrapper = new LambdaQueryWrapper<>();
        
        if (roadId != null) {
            wrapper.eq(RoadSection::getRoadId, roadId);
        }
        wrapper.eq(RoadSection::getDeleted, 0);
        wrapper.orderByDesc(RoadSection::getCreateTime);
        
        return Result.success(roadSectionMapper.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "新增路段")
    @PostMapping("/section")
    public Result<Void> saveSection(@RequestBody RoadSection section) {
        roadSectionMapper.insert(section);
        return Result.success();
    }

    @Operation(summary = "更新路段")
    @PutMapping("/section")
    public Result<Void> updateSection(@RequestBody RoadSection section) {
        roadSectionMapper.updateById(section);
        return Result.success();
    }

    @Operation(summary = "删除路段")
    @DeleteMapping("/section/{id}")
    public Result<Void> deleteSection(@PathVariable Long id) {
        roadSectionMapper.deleteById(id);
        return Result.success();
    }
    
    private String sanitizeFolderName(String name) {
        if (name == null) return "unnamed";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
