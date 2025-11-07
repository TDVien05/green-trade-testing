package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.service.implement.CloudinaryService;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setup() {
        when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    void shouldUploadMultipartFileAndReturnSecureUrlAndPublicId() throws Exception {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getBytes()).thenReturn("content".getBytes());

        String fixedUuid = "fixed-public-id";
        try (MockedStatic<UUID> mockedUUID = mockStatic(UUID.class)) {
            UUID uuidMock = mock(UUID.class);
            mockedUUID.when(UUID::randomUUID).thenReturn(uuidMock);
            when(uuidMock.toString()).thenReturn(fixedUuid);

            Map<String, Object> uploadRes = new HashMap<>();
            uploadRes.put("secure_url", "https://secure.cdn/file.png");
            when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadRes);

            Map<String, String> result = cloudinaryService.upload(multipartFile, "sellers/123");

            assertNotNull(result);
            assertEquals("https://secure.cdn/file.png", result.get("fileUrl"));
            assertEquals(fixedUuid, result.get("publicId"));

            ArgumentCaptor<Map> optionsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(uploader).upload(eq("content".getBytes()), optionsCaptor.capture());
            Map captured = optionsCaptor.getValue();
            assertEquals("sellers/123", captured.get("folder"));
            assertEquals(fixedUuid, captured.get("public_id"));
            assertEquals("auto", captured.get("resource_type"));
        }
    }

    @Test
    void shouldDeleteAndReturnTrueWhenResultOk() throws Exception {
        Map<String, Object> destroyRes = new HashMap<>();
        destroyRes.put("result", "ok");
        when(uploader.destroy(eq("folderA/publicId123"), anyMap())).thenReturn(destroyRes);

        boolean deleted = cloudinaryService.delete("publicId123", "folderA");
        assertTrue(deleted);

        ArgumentCaptor<Map> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).destroy(eq("folderA/publicId123"), optionsCaptor.capture());
        assertEquals("image", optionsCaptor.getValue().get("resource_type"));
    }

    @Test
    void shouldUploadFileAndReturnSecureUrlAndPublicId() throws Exception {
        File file = mock(File.class);

        Map<String, Object> uploadRes = new HashMap<>();
        uploadRes.put("secure_url", "https://secure.cdn/doc.pdf");
        uploadRes.put("public_id", "cloudinary-public-id-789");
        when(uploader.upload(eq(file), anyMap())).thenReturn(uploadRes);

        Map<String, String> result = cloudinaryService.uploadFile(file, "docs/2025");

        assertNotNull(result);
        assertEquals("https://secure.cdn/doc.pdf", result.get("fileUrl"));
        assertEquals("cloudinary-public-id-789", result.get("publicId"));

        ArgumentCaptor<Map> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(file), optionsCaptor.capture());
        Map opts = optionsCaptor.getValue();
        assertEquals("docs/2025", opts.get("folder"));
        assertEquals("auto", opts.get("resource_type"));
        assertEquals("upload", opts.get("type"));
        assertEquals("public", opts.get("access_mode"));
        assertEquals(true, opts.get("use_filename"));
        assertEquals(false, opts.get("unique_filename"));
    }

    @Test
    void shouldUseUrlWhenSecureUrlMissingInUpload() throws Exception {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getBytes()).thenReturn("bytes".getBytes());

        String fixedUuid = "uuid-fallback";
        try (MockedStatic<UUID> mockedUUID = mockStatic(UUID.class)) {
            UUID uuidMock = mock(UUID.class);
            mockedUUID.when(UUID::randomUUID).thenReturn(uuidMock);
            when(uuidMock.toString()).thenReturn(fixedUuid);

            Map<String, Object> uploadRes = new HashMap<>();
            uploadRes.put("url", "http://non-secure.cdn/file.png");
            when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadRes);

            Map<String, String> result = cloudinaryService.upload(multipartFile, "sellers/x");

            assertNotNull(result);
            assertEquals("http://non-secure.cdn/file.png", result.get("fileUrl"));
            assertEquals(fixedUuid, result.get("publicId"));
        }
    }

    @Test
    void shouldReturnNullWhenNoUrlFieldsPresentInUpload() throws Exception {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getBytes()).thenReturn("data".getBytes());

        String fixedUuid = "fetretretret";
        try (MockedStatic<UUID> mockedUUID = mockStatic(UUID.class)) {
            UUID uuidMock = mock(UUID.class);
            mockedUUID.when(UUID::randomUUID).thenReturn(uuidMock);
            when(uuidMock.toString()).thenReturn(fixedUuid);

            Map<String, Object> uploadRes = new HashMap<>();
            when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadRes);

            Map<String, String> result = cloudinaryService.upload(multipartFile, "any");
            assertNull(result);
        }
    }

    @Test
    void shouldReturnFalseWhenDeleteFailsOrResultNotOk() throws Exception {
        // Case 1: result not ok
        Map<String, Object> destroyRes = new HashMap<>();
        destroyRes.put("result", "not_found");
        when(uploader.destroy(eq("pidOnly"), anyMap())).thenReturn(destroyRes);

        boolean res1 = cloudinaryService.delete("pidOnly", null);
        assertFalse(res1);

        // Case 2: exception thrown by Cloudinary
        when(uploader.destroy(eq("folderZ/pidErr"), anyMap())).thenThrow(new RuntimeException("boom"));

        boolean res2 = cloudinaryService.delete("pidErr", "folderZ");
        assertFalse(res2);
    }
}
