package org.example.diplom.service;

import lombok.RequiredArgsConstructor;
import org.example.diplom.dto.FileInfo;
import org.example.diplom.model.CloudFile;
import org.example.diplom.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public void save(String username, String filename,
                     String hash, MultipartFile file) throws IOException {

        Path dir = Paths.get(uploadDir, username);
        Files.createDirectories(dir);
        Path filePath = dir.resolve(filename);
        Files.write(filePath, file.getBytes());

        CloudFile cloudFile = new CloudFile();
        cloudFile.setUsername(username);
        cloudFile.setFilename(filename);
        cloudFile.setSize(file.getSize());
        cloudFile.setHash(hash);
        cloudFile.setStoragePath(filePath.toString());

        fileRepository.save(cloudFile);
    }

    public Resource download(String username, String filename) throws IOException {
        CloudFile cloudFile = fileRepository
                .findByUsernameAndFilename(username, filename)
                .orElseThrow(() -> new FileNotFoundException("Файл не найден: " + filename));

        Path filePath = Paths.get(cloudFile.getStoragePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new FileNotFoundException("Файл недоступен: " + filename);
        }

        return resource;
    }

    @Transactional
    public void delete(String username, String filename) throws IOException {
        CloudFile cloudFile = fileRepository
                .findByUsernameAndFilename(username, filename)
                .orElseThrow(() -> new FileNotFoundException("Файл не найден: " + filename));

        Files.deleteIfExists(Paths.get(cloudFile.getStoragePath()));
        fileRepository.deleteByUsernameAndFilename(username, filename);
    }

    @Transactional
    public void rename(String username, String filename, String newName) throws IOException {
        CloudFile cloudFile = fileRepository
                .findByUsernameAndFilename(username, filename)
                .orElseThrow(() -> new FileNotFoundException("Файл не найден: " + filename));

        Path oldPath = Paths.get(cloudFile.getStoragePath());
        Path newPath = oldPath.getParent().resolve(newName);
        Files.move(oldPath, newPath);

        cloudFile.setFilename(newName);
        cloudFile.setStoragePath(newPath.toString());
        fileRepository.save(cloudFile);
    }


    public List<FileInfo> listFiles(String username, int limit) {
        return fileRepository.findAllByUsername(username)
                .stream()
                .limit(limit)
                .map(f -> new FileInfo(f.getFilename(), f.getSize()))
                .collect(Collectors.toList());
    }

}
