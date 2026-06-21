package org.example.diplom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class FileService {
    private final FileRepository fileRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public void save(String username, String filename,
                     String hash, MultipartFile file) throws IOException {

        Path dir = Paths.get(uploadDir, username);
        log.info("Proceeding file save for username: {}, by path: {}. File hash: {}, file size: {}",
                username,
                dir,
                hash,
                file.getSize());
        Files.createDirectories(dir);
        Path filePath = dir.resolve(filename);
        Files.write(filePath, file.getBytes());
        log.info("Username {} file was saved", username);

        CloudFile cloudFile = new CloudFile();
        cloudFile.setUsername(username);
        cloudFile.setFilename(filename);
        cloudFile.setSize(file.getSize());
        cloudFile.setHash(hash);
        cloudFile.setStoragePath(filePath.toString());

        fileRepository.save(cloudFile);
    }

    public Resource download(String username, String filename) throws IOException {
        log.info("Proceeding file download for username: {}, filename: {}", username, filename);

        CloudFile cloudFile = fileRepository
                .findByUsernameAndFilename(username, filename)
                .orElseThrow(() -> new FileNotFoundException("Файл не найден: " + filename));

        Path filePath = Paths.get(cloudFile.getStoragePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new FileNotFoundException("Файл недоступен: " + filename);
        }
        log.debug("Returning URI for username: {}, filename: {}", username, filename);

        return resource;
    }

    @Transactional
    public void delete(String username, String filename) throws IOException {
        log.info("Proceeding file delete for username: {}, filename: {}", username, filename);
        CloudFile cloudFile = fileRepository
                .findByUsernameAndFilename(username, filename)
                .orElseThrow(() -> new FileNotFoundException("Файл не найден: " + filename));

        Files.deleteIfExists(Paths.get(cloudFile.getStoragePath()));

        log.info("File deleted for username: {}, filename: {}", username, filename);

        fileRepository.deleteByUsernameAndFilename(username, filename);
    }

    @Transactional
    public void rename(String username, String filename, String newName) throws IOException {
        log.info("Proceeding file rename for username: {}, filename: {}, new name: {}", username, filename, newName);
        CloudFile cloudFile = fileRepository
                .findByUsernameAndFilename(username, filename)
                .orElseThrow(() -> new FileNotFoundException("Файл не найден: " + filename));

        Path oldPath = Paths.get(cloudFile.getStoragePath());
        Path newPath = oldPath.getParent().resolve(newName);
        log.debug("Moving file, oldPath: {}, newPath: {}", oldPath, newPath);
        Files.move(oldPath, newPath);

        log.info("File for username: {} was renamed to newName: {}", username, newName);

        cloudFile.setFilename(newName);
        cloudFile.setStoragePath(newPath.toString());
        fileRepository.save(cloudFile);
    }


    public List<CloudFile> listFiles(String username, int limit) {
        log.info("Proceeding list of files for username: {}, limit: {}", username, limit);
        return fileRepository.findAllByUsername(username)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

}
