package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.model.Dispute;
import Green_trade.green_trade_platform.model.Evidence;
import Green_trade.green_trade_platform.repository.EvidenceRepository;
import Green_trade.green_trade_platform.service.implement.CloudinaryService;
import Green_trade.green_trade_platform.service.implement.EvidenceServiceImpl;
import Green_trade.green_trade_platform.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EvidenceServiceTest {

    @Mock
    private FileUtils fileUtils;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private EvidenceServiceImpl evidenceService;

    private Dispute dispute;

    @BeforeEach
    void setup() {
        dispute = Dispute.builder().id(123L).build();
    }

    @Test
    void shouldSaveAndReturnAllEvidencesForMultipleFiles() throws IOException {
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(file1, file2);

        Map<String, String> uploadRes1 = Map.of("fileUrl", "https://cdn/img1.jpg", "publicId", "pid1");
        Map<String, String> uploadRes2 = Map.of("fileUrl", "https://cdn/img2.jpg", "publicId", "pid2");

        when(cloudinaryService.upload(eq(file1), anyString())).thenReturn(uploadRes1);
        when(cloudinaryService.upload(eq(file2), anyString())).thenReturn(uploadRes2);

        // repository.save returns argument
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(inv -> inv.getArgument(0));
        List<Evidence> expected = new ArrayList<>();
        expected.add(Evidence.builder().imageUrl("https://cdn/img1.jpg").orderImage(1L).dispute(dispute).build());
        expected.add(Evidence.builder().imageUrl("https://cdn/img2.jpg").orderImage(2L).dispute(dispute).build());
        when(evidenceRepository.findAllByDispute(dispute)).thenReturn(expected);

        List<Evidence> result = evidenceService.saveEvidence(files, dispute);

        // validations were called
        verify(fileUtils, times(2)).validateFile(any(MultipartFile.class));
        // uploads were called
        verify(cloudinaryService, times(1)).upload(eq(file1), anyString());
        verify(cloudinaryService, times(1)).upload(eq(file2), anyString());
        // saves were called twice
        ArgumentCaptor<Evidence> evidenceCaptor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository, times(2)).save(evidenceCaptor.capture());
        List<Evidence> saved = evidenceCaptor.getAllValues();
        assertEquals("https://cdn/img1.jpg", saved.get(0).getImageUrl());
        assertEquals("https://cdn/img2.jpg", saved.get(1).getImageUrl());

        // returns repository list
        assertEquals(2, result.size());
        assertEquals(expected, result);
        verify(evidenceRepository).findAllByDispute(dispute);
    }

    @Test
    void shouldAssignSequentialOrderImageStartingFromOne() throws IOException {
        MultipartFile f0 = mock(MultipartFile.class);
        MultipartFile f1 = mock(MultipartFile.class);
        MultipartFile f2 = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(f0, f1, f2);

        when(cloudinaryService.upload(any(MultipartFile.class), anyString()))
                .thenReturn(Map.of("fileUrl", "u0"))
                .thenReturn(Map.of("fileUrl", "u1"))
                .thenReturn(Map.of("fileUrl", "u2"));
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(inv -> inv.getArgument(0));
        when(evidenceRepository.findAllByDispute(dispute)).thenReturn(Collections.emptyList());

        evidenceService.saveEvidence(files, dispute);

        ArgumentCaptor<Evidence> captor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository, times(3)).save(captor.capture());
        List<Evidence> saved = captor.getAllValues();

        assertEquals(1L, saved.get(0).getOrderImage());
        assertEquals(2L, saved.get(1).getOrderImage());
        assertEquals(3L, saved.get(2).getOrderImage());
    }

    @Test
    void shouldCallCloudinaryWithExpectedPathPerFile() throws IOException {
        MultipartFile f0 = mock(MultipartFile.class);
        MultipartFile f1 = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(f0, f1);

        when(cloudinaryService.upload(any(MultipartFile.class), anyString()))
                .thenReturn(Map.of("fileUrl", "url0"))
                .thenReturn(Map.of("fileUrl", "url1"));
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(inv -> inv.getArgument(0));
        when(evidenceRepository.findAllByDispute(dispute)).thenReturn(Collections.emptyList());

        evidenceService.saveEvidence(files, dispute);

        String base = "Evidences/DisputeId:" + dispute.getId();
        verify(cloudinaryService).upload(eq(f0), eq(base + "/evidences_image_0"));
        verify(cloudinaryService).upload(eq(f1), eq(base + "/evidences_image_1"));
    }

    @Test
    void shouldNotUploadOrSaveWhenValidationFails() throws IOException {
        MultipartFile invalid = mock(MultipartFile.class);
        MultipartFile another = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(invalid, another);

        doThrow(new IllegalArgumentException("invalid")).when(fileUtils).validateFile(invalid);

        assertThrows(IllegalArgumentException.class, () -> evidenceService.saveEvidence(files, dispute));

        verify(cloudinaryService, never()).upload(any(), anyString());
        verify(evidenceRepository, never()).save(any());
        verify(evidenceRepository, never()).findAllByDispute(any());
    }

    @Test
    void shouldPropagateIOExceptionAndAvoidPartialSaves() throws IOException {
        MultipartFile f0 = mock(MultipartFile.class);
        MultipartFile f1 = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(f0, f1);

        when(cloudinaryService.upload(eq(f0), anyString()))
                .thenThrow(new IOException("network"));
        // ensure validate passes
        // ensure repository not called
        assertThrows(IOException.class, () -> evidenceService.saveEvidence(files, dispute));

        verify(evidenceRepository, never()).save(any());
        verify(evidenceRepository, never()).findAllByDispute(any());
        // after exception on first upload, no second upload attempt
        verify(cloudinaryService, times(1)).upload(any(MultipartFile.class), anyString());
    }

    @Test
    void shouldReturnEmptyListWhenNoFilesProvided() throws IOException {
        List<MultipartFile> files = Collections.emptyList();
        List<Evidence> empty = Collections.emptyList();
        when(evidenceRepository.findAllByDispute(dispute)).thenReturn(empty);

        List<Evidence> result = evidenceService.saveEvidence(files, dispute);

        verify(fileUtils, never()).validateFile(any());
        verify(cloudinaryService, never()).upload(any(), anyString());
        verify(evidenceRepository, never()).save(any());
        verify(evidenceRepository, times(1)).findAllByDispute(dispute);
        assertTrue(result.isEmpty());
    }
}
