package org.example.diplom.service;

import org.example.diplom.model.CloudFile;
import org.example.diplom.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    FileRepository fileRepositoryMock;

    FileService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        FileRepository fileRepository;
        fileService = new FileService(fileRepositoryMock);
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
    }
    @Nested
    class TestSave {
        @Test
        void save_shouldSaveFileToUsernameDir() throws IOException {
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getBytes()).thenReturn("fileContent".getBytes());
            when(mockFile.getSize()).thenReturn(7L);
            String username = "ivan";
            String filename = "document.pdf";
            String hashString = "abc123";


            fileService.save(username, filename, hashString, mockFile);

            assertTrue(Files.exists(tempDir.resolve(username + "/" + filename)));
        }

        @Test
        void save_shouldCallRepositorySaveOnceWithCorrectArguments() throws IOException {
            Long size = 7L;
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getBytes()).thenReturn("fileContent".getBytes());
            when(mockFile.getSize()).thenReturn(size);
            String username = "ivan";
            String filename = "document.pdf";
            String hashString = "abc123";


            fileService.save(username, filename, hashString, mockFile);

            ArgumentCaptor<CloudFile> captor = ArgumentCaptor.forClass(CloudFile.class);
            verify(fileRepositoryMock, times(1)).save(captor.capture());
            CloudFile cloudFile = captor.getValue();
            assertEquals(username, cloudFile.getUsername());
            assertEquals(filename, cloudFile.getFilename());
            assertEquals(hashString, cloudFile.getHash());
            assertEquals(size, cloudFile.getSize());
        }
    }

    @Nested
    class TestDownload {
        @Test
        void download_shouldReturnResourceWithCorrectInfo() throws IOException {
            String username = "ivan";
            String filename = "document.pdf";
            String hashString = "abc123";
            byte[] bytes = {10, 12, 11, 15};
            Path dir = Paths.get(String.valueOf(tempDir), username);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(filename);
            Files.write(filePath, bytes);

            CloudFile cloudFile = new CloudFile();
            cloudFile.setFilename(filename);
            cloudFile.setUsername(username);
            cloudFile.setHash(hashString);
            cloudFile.setSize(0L);
            cloudFile.setStoragePath(filePath.toString());

            when(fileRepositoryMock.findByUsernameAndFilename(any(String.class), any(String.class)))
                    .thenReturn(Optional.of(cloudFile));
            Resource resource = fileService.download(username, filename);

            assertEquals(filename, resource.getFilename());
            assertEquals(bytes.length, resource.getFile().length());
            assertEquals(filePath, resource.getFilePath());
        }

        @Test
        void download_shouldThrowFileNotFoundForNonExistentFile() throws IOException {
            when(fileRepositoryMock.findByUsernameAndFilename(any(String.class), any(String.class)))
                    .thenReturn(Optional.empty());
            assertThrows(FileNotFoundException.class, () -> fileService.download("nonexist", "nonexist"));
        }

        @Test
        void download_shouldThrowWhenFileDeletedAfterSave() throws IOException {
            String username = "ivan";
            String filename = "document.pdf";

            Path dir = tempDir.resolve(username);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(filename);
            Files.write(filePath, new byte[]{1, 2, 3});

            CloudFile cloudFile = new CloudFile();
            cloudFile.setFilename(filename);
            cloudFile.setUsername(username);
            cloudFile.setHash("abc123");
            cloudFile.setSize(3L);
            cloudFile.setStoragePath(filePath.toString());

            Files.delete(filePath);

            when(fileRepositoryMock.findByUsernameAndFilename(any(), any()))
                    .thenReturn(Optional.of(cloudFile));

            assertThrows(FileNotFoundException.class, () ->
                    fileService.download(username, filename)
            );
        }
    }

    @Nested
    class TestDelete {
        @Test
        void delete_shouldDeleteFileFromDiskAndRepository() throws IOException {
            String username = "ivan";
            String filename = "document.pdf";

            Path dir = tempDir.resolve(username);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(filename);
            Files.write(filePath, new byte[]{1, 2, 3});

            CloudFile cloudFile = new CloudFile();
            cloudFile.setFilename(filename);
            cloudFile.setUsername(username);
            cloudFile.setHash("abc123");
            cloudFile.setSize(3L);
            cloudFile.setStoragePath(filePath.toString());

            when(fileRepositoryMock.findByUsernameAndFilename(any(), any()))
                    .thenReturn(Optional.of(cloudFile));

            fileService.delete(username, filename);

            assertFalse(Files.exists(filePath));
            verify(fileRepositoryMock, times(1))
                    .deleteByUsernameAndFilename(username, filename);
        }

        @Test
        void delete_shouldThrowWhenFileNotFoundInDb() {
            when(fileRepositoryMock.findByUsernameAndFilename(any(), any()))
                    .thenReturn(Optional.empty());

            assertThrows(FileNotFoundException.class, () ->
                    fileService.delete("ivan", "document.pdf")
            );

            verify(fileRepositoryMock, times(0))
                    .deleteByUsernameAndFilename(any(), any());
        }
    }

    @Nested
    class TestRename {
        @Test
        void rename_shouldRenameFileOnDiskAndUpdateRepository() throws IOException {
            String username = "ivan";
            String filename = "document.pdf";
            String newName = "renamed.pdf";

            Path dir = tempDir.resolve(username);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(filename);
            Files.write(filePath, new byte[]{1, 2, 3});

            CloudFile cloudFile = new CloudFile();
            cloudFile.setFilename(filename);
            cloudFile.setUsername(username);
            cloudFile.setHash("abc123");
            cloudFile.setSize(3L);
            cloudFile.setStoragePath(filePath.toString());

            when(fileRepositoryMock.findByUsernameAndFilename(any(), any()))
                    .thenReturn(Optional.of(cloudFile));

            fileService.rename(username, filename, newName);

            assertFalse(Files.exists(filePath));
            assertTrue(Files.exists(dir.resolve(newName)));

            ArgumentCaptor<CloudFile> captor = ArgumentCaptor.forClass(CloudFile.class);
            verify(fileRepositoryMock, times(1)).save(captor.capture());
            CloudFile saved = captor.getValue();
            assertEquals(newName, saved.getFilename());
            assertEquals(dir.resolve(newName).toString(), saved.getStoragePath());
        }

        @Test
        void rename_shouldThrowWhenFileNotFoundInDb() {
            when(fileRepositoryMock.findByUsernameAndFilename(any(), any()))
                    .thenReturn(Optional.empty());

            assertThrows(FileNotFoundException.class, () ->
                    fileService.rename("ivan", "document.pdf", "renamed.pdf")
            );

            verify(fileRepositoryMock, times(0)).save(any());
        }
    }
    @Nested
    class TestListFiles {
        @Test
        void listFiles_shouldReturnAllFilesWithinLimit() {
            String username = "ivan";
            int limit = 2;

            CloudFile file1 = new CloudFile();
            file1.setUsername(username);
            file1.setFilename("file1.pdf");

            CloudFile file2 = new CloudFile();
            file2.setUsername(username);
            file2.setFilename("file2.pdf");

            CloudFile file3 = new CloudFile();
            file3.setUsername(username);
            file3.setFilename("file3.pdf");

            when(fileRepositoryMock.findAllByUsername(username))
                    .thenReturn(List.of(file1, file2, file3));

            List<CloudFile> result = fileService.listFiles(username, limit);

            assertEquals(limit, result.size());
            assertEquals("file1.pdf", result.get(0).getFilename());
            assertEquals("file2.pdf", result.get(1).getFilename());
        }

        @Test
        void listFiles_shouldReturnEmptyListWhenNoFiles() {
            when(fileRepositoryMock.findAllByUsername(any()))
                    .thenReturn(List.of());

            List<CloudFile> result = fileService.listFiles("ivan", 10);

            assertTrue(result.isEmpty());
        }

        @Test
        void listFiles_shouldReturnAllWhenLimitExceedsSize() {
            String username = "ivan";

            CloudFile file1 = new CloudFile();
            file1.setFilename("file1.pdf");
            CloudFile file2 = new CloudFile();
            file2.setFilename("file2.pdf");

            when(fileRepositoryMock.findAllByUsername(username))
                    .thenReturn(List.of(file1, file2));

            List<CloudFile> result = fileService.listFiles(username, 100);

            assertEquals(2, result.size());
        }
    }
}