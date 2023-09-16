package com.agileboot.admin.controller.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.agileboot.common.constant.Constants.UploadSubDir;
import com.agileboot.common.core.dto.ResponseDTO;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode;
import com.agileboot.common.exception.error.ErrorCode.Business;
import com.agileboot.common.utils.ServletHolderUtil;
import com.agileboot.common.utils.file.FileUploadUtils;
import com.agileboot.common.utils.jackson.JacksonUtil;
import com.agileboot.domain.common.dto.UploadDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用请求处理
 * TODO 需要重构
 * @author valarchie
 */
@Tag(name = "上传API", description = "上传相关接口")
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {


    /**
     * 通用下载请求
     * download接口  其实不是很有必要
     * @param fileName 文件名称
     */
    @Operation(summary = "下载文件")
    @GetMapping("/download")
    public ResponseEntity<byte[]> fileDownload(String fileName, HttpServletResponse response) {
        try {
            if (!FileUploadUtils.isAllowDownload(fileName)) {
                // 返回类型是ResponseEntity 不能捕获异常， 需要手动将错误填到 ResponseEntity
                ResponseDTO<Object> fail = ResponseDTO.fail(
                    new ApiException(Business.COMMON_FILE_NOT_ALLOWED_TO_DOWNLOAD, fileName));
                return new ResponseEntity<>(JacksonUtil.to(fail).getBytes(), null, HttpStatus.OK);
            }

            String filePath = FileUploadUtils.getFileAbsolutePath(UploadSubDir.DOWNLOAD_PATH, fileName);

            HttpHeaders downloadHeader = FileUploadUtils.getDownloadHeader(fileName);
            // 下载文件时，要加上  application/octet-stream 响应头 comment by wangzhy 2023.9.16
            // why 为什么这里是在 HttpServletResponse 设置响应头呢？ comment by wangzhy 2023.9.16
            /**
             * ResponseEntity 是 HttpEntity 的子类。
             * 在 SpringMVC 中是由 HttpEntityMethodProcessor 来处理这个对象。
             * org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor#handleReturnValue(java.lang.Object, org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest)
             * 在这个方法中，会获取到 HttpServletResponse 对象。将 HttpServletResponse 的响应头复制到 HttpEntity 对象中
             * 因此，在这里给 HttpServletResponse 设置响应头和给 ResponseEntity 设置响应头是等效的。
             * 参考文档： https://zhuanlan.zhihu.com/p/626962131
             * 处理过程：
             * 首先，检查返回值是否为空，如果为空，就直接返回。
             * 然后，创建 ServletServerHttpRequest 和 ServletServerHttpResponse 对象，用于读取请求和写入响应。
             * 接着，断言返回值是 HttpEntity 类型的，并将其强制转换为 HttpEntity 或 ResponseEntity 对象。
             * 然后，获取输出消息的标头和实体标头，并将实体标头复制到输出标头中。
             * 接着，判断返回值是否是 ResponseEntity 类型的，并获取其状态码，并设置到输出消息中。
             * 最后，调用父类的 writeWithMessageConverters 方法，根据实体类型和请求内容协商选择合适的消息转换器，并将实体内容写入到输出消息中。
             */
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            // ResponseEntity 表示一个 HTTP 响应。 comment by wangzhy 2023.9.16
            return new ResponseEntity<>(FileUtil.readBytes(filePath), downloadHeader, HttpStatus.OK);
        } catch (Exception e) {
            log.error("下载文件失败", e);
            return null;
        }
    }

    /**
     * 通用上传请求（单个）
     */
    @Operation(summary = "单个上传文件")
    @PostMapping("/upload")
    public ResponseDTO<UploadDTO> uploadFile(MultipartFile file) {
        if (file == null) {
            throw new ApiException(ErrorCode.Business.UPLOAD_FILE_IS_EMPTY);
        }

        // 上传并返回新文件名称
        String fileName = FileUploadUtils.upload(UploadSubDir.UPLOAD_PATH, file);

        String url = ServletHolderUtil.getContextUrl() + fileName;

        UploadDTO uploadDTO = UploadDTO.builder()
            // 全路径
            .url(url)
            // 相对路径
            .fileName(fileName)
            // 新生成的文件名
            .newFileName(FileNameUtil.getName(fileName))
            // 原始的文件名
            .originalFilename(file.getOriginalFilename()).build();

        return ResponseDTO.ok(uploadDTO);
    }

    /**
     * 通用上传请求（多个）
     */
    @Operation(summary = "多个上传文件")
    @PostMapping("/uploads")
    public ResponseDTO<List<UploadDTO>> uploadFiles(List<MultipartFile> files) {
        if (CollUtil.isEmpty(files)) {
            throw new ApiException(ErrorCode.Business.UPLOAD_FILE_IS_EMPTY);
        }

        List<UploadDTO> uploads = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file != null) {
                // 上传并返回新文件名称
                String fileName = FileUploadUtils.upload(UploadSubDir.UPLOAD_PATH, file);
                String url = ServletHolderUtil.getContextUrl() + fileName;
                UploadDTO uploadDTO = UploadDTO.builder()
                    .url(url)
                    .fileName(fileName)
                    .newFileName(FileNameUtil.getName(fileName))
                    .originalFilename(file.getOriginalFilename()).build();

                uploads.add(uploadDTO);

            }
        }
        return ResponseDTO.ok(uploads);
    }

}
