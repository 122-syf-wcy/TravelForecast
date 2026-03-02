package com.travel.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.dto.admin.AdminScenicDetailVO;
import com.travel.dto.admin.StatusUpdateRequest;
import com.travel.entity.scenic.ScenicImage;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.entity.scenic.ScenicVideo;
import com.travel.service.common.OssService;
import com.travel.service.scenic.ScenicImageService;
import com.travel.service.scenic.ScenicSpotService;
import com.travel.service.scenic.ScenicVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理员端景区管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/admin/scenic")
@RequiredArgsConstructor
public class AdminScenicController {

    private final ScenicSpotService scenicSpotService;
    private final ScenicImageService scenicImageService;
    private final ScenicVideoService scenicVideoService;
    private final OssService ossService;

    /**
     * 分页查询景区列表
     */
    @GetMapping("/list")
    public Result<Page<ScenicSpot>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        log.info("分页查询景区列表: page={}, size={}, status={}, keyword={}", page, size, status, keyword);
        Page<ScenicSpot> result = scenicSpotService.getPage(page, size, status, keyword);
        return Result.success(result);
    }

    /**
     * 获取景区详情（支持ID或景区编码）
     */
    @GetMapping("/{idOrCode}")
    public Result<AdminScenicDetailVO> detail(@PathVariable String idOrCode) {
        log.info("获取景区详情: idOrCode={}", idOrCode);

        ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
        if (spot == null) {
            return Result.error("景区不存在");
        }

        List<ScenicImage> images = scenicImageService.getByScenicId(spot.getId());
        List<ScenicVideo> videos = scenicVideoService.getByScenicId(spot.getId());

        AdminScenicDetailVO vo = new AdminScenicDetailVO();
        vo.setScenic(spot);
        vo.setImages(images);
        vo.setVideos(videos);

        return Result.success(vo);
    }

    /**
     * 创建景区
     */
    @PostMapping
    public Result<ScenicSpot> create(@RequestBody ScenicSpot scenicSpot) {
        log.info("创建景区: name={}", scenicSpot.getName());

        try {
            ScenicSpot created = scenicSpotService.create(scenicSpot);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建景区失败", e);
            return Result.error("创建景区失败: " + e.getMessage());
        }
    }

    /**
     * 更新景区信息
     */
    @PutMapping("/{id}")
    public Result<ScenicSpot> update(@PathVariable Long id, @RequestBody ScenicSpot scenicSpot) {
        log.info("更新景区: id={}, name={}", id, scenicSpot.getName());

        scenicSpot.setId(id);
        try {
            ScenicSpot updated = scenicSpotService.update(scenicSpot);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新景区失败", e);
            return Result.error("更新景区失败: " + e.getMessage());
        }
    }

    /**
     * 删除景区
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除景区: id={}", id);

        boolean success = scenicSpotService.delete(id);
        if (success) {
            scenicImageService.deleteByScenicId(id);
            scenicVideoService.deleteByScenicId(id);
            return Result.success(null);
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * 更新景区状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        log.info("更新景区状态: id={}, status={}", id, request.getStatus());

        boolean success = scenicSpotService.updateStatus(id, request.getStatus());
        return success ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 上传景区图片（支持ID或景区编码）
     */
    @PostMapping(value = "/{idOrCode}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ScenicImage> uploadImage(
            @PathVariable String idOrCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "GALLERY") String imageType,
            @RequestParam(defaultValue = "0") Integer sortOrder) {

        log.info("上传景区图片: scenicIdOrCode={}, imageType={}, sortOrder={}", idOrCode, imageType, sortOrder);

        try {
            ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
            if (spot == null) {
                return Result.error("景区不存在");
            }
            Long scenicId = spot.getId();

            Map<String, String> uploadResult = ossService.uploadScenicImage(file, scenicId, imageType);

            ScenicImage image = new ScenicImage();
            image.setScenicId(scenicId);
            image.setImageUrl(uploadResult.get("url"));
            image.setImageType(imageType);
            image.setSortOrder(sortOrder);

            ScenicImage created = scenicImageService.add(image);
            return Result.success(created);

        } catch (Exception e) {
            log.error("上传景区图片失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传景区图片（支持ID或景区编码）
     */
    @PostMapping(value = "/{idOrCode}/images/batch-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<ScenicImage>> batchUploadImages(
            @PathVariable String idOrCode,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(defaultValue = "GALLERY") String imageType) {

        log.info("批量上传景区图片: scenicIdOrCode={}, imageType={}, count={}", idOrCode, imageType, files.length);

        try {
            ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
            if (spot == null) {
                return Result.error("景区不存在");
            }
            Long scenicId = spot.getId();

            List<ScenicImage> images = new ArrayList<>();

            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];

                Map<String, String> uploadResult = ossService.uploadScenicImage(file, scenicId, imageType);

                ScenicImage image = new ScenicImage();
                image.setScenicId(scenicId);
                image.setImageUrl(uploadResult.get("url"));
                image.setImageType(imageType);
                image.setSortOrder(i);

                images.add(image);
            }

            scenicImageService.batchAdd(images);
            return Result.success(images);

        } catch (Exception e) {
            log.error("批量上传景区图片失败", e);
            return Result.error("批量上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除景区图片
     */
    @DeleteMapping("/images/{imageId}")
    public Result<Void> deleteImage(@PathVariable Long imageId) {
        log.info("删除景区图片: imageId={}", imageId);

        try {
            ScenicImage image = scenicImageService.getById(imageId);
            if (image == null) {
                return Result.error("图片不存在");
            }

            String imageUrl = image.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String objectName = ossService.extractObjectNameFromUrl(imageUrl);
                if (objectName != null) {
                    try {
                        ossService.deleteFile(objectName);
                        log.info("已删除OSS文件: {}", objectName);
                    } catch (Exception e) {
                        log.warn("删除OSS文件失败: {}, 继续删除数据库记录", objectName, e);
                    }
                }
            }

            boolean success = scenicImageService.delete(imageId);
            if (success) {
                log.info("删除景区图片成功: imageId={}", imageId);
                return Result.success(null);
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除景区图片失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 上传景区视频（支持ID或景区编码）
     */
    @PostMapping(value = "/{idOrCode}/videos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ScenicVideo> uploadVideo(
            @PathVariable String idOrCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            @RequestParam(defaultValue = "0") Integer duration) {

        log.info("上传景区视频: scenicIdOrCode={}, title={}", idOrCode, title);

        try {
            ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
            if (spot == null) {
                return Result.error("景区不存在");
            }
            Long scenicId = spot.getId();

            Map<String, String> videoResult = ossService.uploadScenicVideo(file, scenicId, title);

            String coverUrl = null;
            if (cover != null && !cover.isEmpty()) {
                Map<String, String> coverResult = ossService.uploadVideoCover(cover, scenicId);
                coverUrl = coverResult.get("url");
            }

            ScenicVideo video = new ScenicVideo();
            video.setScenicId(scenicId);
            video.setTitle(title);
            video.setVideoUrl(videoResult.get("url"));
            video.setCoverUrl(coverUrl);
            video.setDuration(duration);

            ScenicVideo created = scenicVideoService.add(video);
            return Result.success(created);

        } catch (Exception e) {
            log.error("上传景区视频失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除景区视频
     */
    @DeleteMapping("/videos/{videoId}")
    public Result<Void> deleteVideo(@PathVariable Long videoId) {
        log.info("删除景区视频: videoId={}", videoId);

        List<ScenicVideo> videos = scenicVideoService.getByScenicId(videoId);
        if (!videos.isEmpty()) {
            ScenicVideo video = videos.get(0);

            String videoObjectName = ossService.extractObjectNameFromUrl(video.getVideoUrl());
            if (videoObjectName != null) {
                ossService.deleteFile(videoObjectName);
            }

            if (video.getCoverUrl() != null) {
                String coverObjectName = ossService.extractObjectNameFromUrl(video.getCoverUrl());
                if (coverObjectName != null) {
                    ossService.deleteFile(coverObjectName);
                }
            }
        }

        boolean success = scenicVideoService.delete(videoId);
        return success ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 获取景区的所有图片（支持ID或景区编码）
     */
    @GetMapping("/{idOrCode}/images")
    public Result<List<ScenicImage>> getImages(@PathVariable String idOrCode) {
        log.info("获取景区图片列表: scenicIdOrCode={}", idOrCode);

        ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
        if (spot == null) {
            return Result.error("景区不存在");
        }

        List<ScenicImage> images = scenicImageService.getByScenicId(spot.getId());
        return Result.success(images);
    }

    /**
     * 获取景区的所有视频（支持ID或景区编码）
     */
    @GetMapping("/{idOrCode}/videos")
    public Result<List<ScenicVideo>> getVideos(@PathVariable String idOrCode) {
        log.info("获取景区视频列表: scenicIdOrCode={}", idOrCode);

        ScenicSpot spot = scenicSpotService.getByIdOrCode(idOrCode);
        if (spot == null) {
            return Result.error("景区不存在");
        }

        List<ScenicVideo> videos = scenicVideoService.getByScenicId(spot.getId());
        return Result.success(videos);
    }
}
