package com.sky.controller.admin;

import com.aliyun.oss.model.MultipartUpload;
import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {
    private static final String FILE_UPLOAD_PATH = "D:/AppStore/资料/day03/图片资源";

    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        //log.info("上传文件：{}",file);
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        File dir = new File(FILE_UPLOAD_PATH);
        if (!dir.exists() || !dir.isDirectory()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("文件夹成功创建，{}", FILE_UPLOAD_PATH);
            } else {
                log.warn("文件夹创建失败或已存在，{}", FILE_UPLOAD_PATH);
            }
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return Result.error("文件名无效");
        }

        //获取后缀
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (!suffix.equalsIgnoreCase(".png") && !suffix.equalsIgnoreCase(".jpg") && !suffix.equalsIgnoreCase(".jpeg")) {
            return Result.error("文件格式错误！");
        }
        //拼接新的随机名称
        originalFilename = UUID.randomUUID().toString() + suffix;
        //确保文件路径安全，避免路径遍历攻击
        Path targetLocation = Paths.get(FILE_UPLOAD_PATH).resolve(originalFilename).normalize();
        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件上传成功：{}", originalFilename);
        } catch (Exception e) {
            log.error("文件上传失败：{}", originalFilename, e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
        //你可以根据实际情况调整返回的文件访问链接
        String fileAccessUrl = "http://localhost:8080/static/" + originalFilename;
        return Result.success(fileAccessUrl);
    }
}
