package org.example.diplom.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.diplom.dto.FileInfo;
import org.example.diplom.dto.RenameRequest;
import org.example.diplom.model.CloudFile;
import org.example.diplom.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;

    private String currentUser() {
        return Objects.requireNonNull(SecurityContextHolder.getContext()
                        .getAuthentication())
                .getName();
    }

    @PostMapping("/file")
    public ResponseEntity<Void> upload(
            @RequestParam("filename") String filename,
            @RequestParam(value = "hash", required = false) String hash,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("POST /file — filename: {}, user: {}", filename, currentUser());

        fileService.save(currentUser(), filename, hash, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> download(
            @RequestParam("filename") String filename) throws IOException {
        log.info("GET /file — filename: {}, user: {}", filename, currentUser());

        Resource resource = fileService.download(currentUser(), filename);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/file")
    public ResponseEntity<Void> delete(
            @RequestParam("filename") String filename) throws IOException {
        log.info("DELETE /file — filename: {}, user: {}", filename, currentUser());

        fileService.delete(currentUser(), filename);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/file")
    public ResponseEntity<Void> rename(
            @RequestParam("filename") String filename,
            @RequestBody RenameRequest request) throws IOException {
        log.info("PUT /file — filename: {}, user: {}, newName: {}", filename, currentUser(), request.getName());

        fileService.rename(currentUser(), filename, request.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileInfo>> list(
            @RequestParam("limit") int limit) {
        log.info("GET /list — limit: {}, user: {}", limit, currentUser());

        List<CloudFile> files = fileService.listFiles(currentUser(), limit);

        List<FileInfo> fileInfoDTO = files.stream()
                .map(file -> new FileInfo(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(fileInfoDTO);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<?> handleFileNotFound(FileNotFoundException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "file not found", "id", 0));
    }
}
