package org.example.diplom.integration;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.diplom.model.User;
import org.example.diplom.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FileControllerIntegrationTest extends BaseIntegrationTest {


    @Autowired
    private FileRepository fileRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String authToken;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @BeforeEach
    void setUp() throws Exception {
        fileRepository.deleteAll();
        userRepository.deleteAll();

        Path dir = Paths.get(uploadDir);
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); }
                        catch (
                                IOException ignored) {}
                    });
        }

        User user = new User();
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode("password"));
        userRepository.save(user);

        authToken = login("admin", "password");
    }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("login", username, "password", password)
        );

        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("auth-token").asText();
    }

    @Test
    @DisplayName("POST /file - успешная загрузка файла")
    void uploadFile_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, cloud!".getBytes()
        );

        mockMvc.perform(multipart("/file")
                        .file(file)
                        .param("hash", "testhash")
                        .header("auth-token", authToken)
                        .param("filename", "test.txt"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /file - 401 без токена")
    void uploadFile_unauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "content".getBytes()
        );

        mockMvc.perform(multipart("/file")
                        .file(file)
                        .param("filename", "test.txt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /list - возвращает загруженный файл")
    void listFiles_returnsUploadedFile() throws Exception {
        // Сначала загружаем
        uploadTestFile("report.txt", "some content");

        // Проверяем список
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filename", is("report.txt")))
                .andExpect(jsonPath("$[0].size", greaterThan(0)));
    }

    @Test
    @DisplayName("GET /list - пустой список если файлов нет")
    void listFiles_empty() throws Exception {
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /list - 401 без токена")
    void listFiles_unauthorized() throws Exception {
        mockMvc.perform(get("/list")
                        .param("limit", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /file - скачивание существующего файла")
    void downloadFile_success() throws Exception {
        uploadTestFile("download-me.txt", "file content here");

        mockMvc.perform(get("/file")
                        .header("auth-token", authToken)
                        .param("filename", "download-me.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("download-me.txt")))
                .andExpect(content().contentType(
                        MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    @DisplayName("GET /file - 400 если файл не существует")
    void downloadFile_notFound() throws Exception {
        mockMvc.perform(get("/file")
                        .header("auth-token", authToken)
                        .param("filename", "nonexistent.txt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /file - 401 без токена")
    void downloadFile_unauthorized() throws Exception {
        mockMvc.perform(get("/file")
                        .param("filename", "some.txt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /file - успешное удаление")
    void deleteFile_success() throws Exception {
        uploadTestFile("to-delete.txt", "bye bye");

        mockMvc.perform(delete("/file")
                        .header("auth-token", authToken)
                        .param("filename", "to-delete.txt"))
                .andExpect(status().isOk());

        // Убеждаемся что файл пропал из списка
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken)
                        .param("limit", "10"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("DELETE /file - 400 если файл не найден")
    void deleteFile_notFound() throws Exception {
        mockMvc.perform(delete("/file")
                        .header("auth-token", authToken)
                        .param("filename", "ghost.txt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /file — 401 без токена")
    void deleteFile_unauthorized() throws Exception {
        mockMvc.perform(delete("/file")
                        .param("filename", "some.txt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /file - успешное переименование")
    void renameFile_success() throws Exception {
        uploadTestFile("old-name.txt", "content");

        String body = objectMapper.writeValueAsString(Map.of("name", "new-name.txt"));

        mockMvc.perform(put("/file")
                        .header("auth-token", authToken)
                        .param("filename", "old-name.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Проверяем что новое имя есть в списке
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken)
                        .param("limit", "10"))
                .andExpect(jsonPath("$[0].filename", is("new-name.txt")));
    }

    @Test
    @DisplayName("PUT /file - 400 если файл не найден")
    void renameFile_notFound() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "new-name.txt"));

        mockMvc.perform(put("/file")
                        .header("auth-token", authToken)
                        .param("filename", "nonexistent.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /file - 401 без токена")
    void renameFile_unauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "new.txt"));

        mockMvc.perform(put("/file")
                        .param("filename", "old.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ─── Изоляция пользователей ──────────────────────────────────────────────

    @Test
    @DisplayName("Пользователь не видит файлы другого пользователя")
    void userIsolation_cannotSeeOthersFiles() throws Exception {
        // Второй пользователь загружает файл
        User other = new User();
        other.setUsername("user");
        other.setPassword(passwordEncoder.encode("user"));
        userRepository.save(other);

        String otherToken = login("user", "user");
        uploadTestFileWithToken(otherToken, "secret.txt", "private data");

        // Первый пользователь не видит его файл
        mockMvc.perform(get("/list")
                        .header("auth-token", authToken)
                        .param("limit", "10"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private void uploadTestFile(String filename, String content) throws Exception {
        uploadTestFileWithToken(authToken, filename, content);
    }

    private void uploadTestFileWithToken(String token, String filename,
                                         String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", filename,
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes()
        );


        mockMvc.perform(multipart("/file")
                        .file(file)
                        .param("hash", "testhash")
                        .header("auth-token", token)
                        .param("filename", filename))
                .andExpect(status().isOk());
    }
}
