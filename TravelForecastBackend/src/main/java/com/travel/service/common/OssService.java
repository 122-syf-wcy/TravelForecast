package com.travel.service.common;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * OSS文件上传服务接口
 */
public interface OssService {

    /**
     * 上传景区图片
     * @param file 文件
     * @param scenicId 景区ID
     * @param imageType 图片类型（COVER/GALLERY/MAP）
     * @return 上传结果（包含url、signedUrl、objectName）
     */
    Map<String, String> uploadScenicImage(MultipartFile file, Long scenicId, String imageType) throws Exception;

    /**
     * 上传景区视频
     * @param file 文件
     * @param scenicId 景区ID
     * @param title 视频标题
     * @return 上传结果
     */
    Map<String, String> uploadScenicVideo(MultipartFile file, Long scenicId, String title) throws Exception;

    /**
     * 上传景区视频封面
     * @param file 文件
     * @param scenicId 景区ID
     * @return 上传结果
     */
    Map<String, String> uploadVideoCover(MultipartFile file, Long scenicId) throws Exception;

    /**
     * 删除OSS文件
     * @param objectName OSS对象名称
     * @return 是否成功
     */
    boolean deleteFile(String objectName);

    /**
     * 生成签名URL
     * @param objectName OSS对象名称
     * @param expireHours 过期小时数
     * @return 签名URL
     */
    String generateSignedUrl(String objectName, int expireHours);

    /**
     * 从URL提取objectName
     */
    String extractObjectNameFromUrl(String url);
}

