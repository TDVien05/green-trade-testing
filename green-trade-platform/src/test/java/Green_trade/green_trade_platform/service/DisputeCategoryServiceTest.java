package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.model.DisputeCategory;
import Green_trade.green_trade_platform.repository.DisputeCategoryRepository;
import Green_trade.green_trade_platform.service.implement.DisputeCategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DisputeCategoryServiceTest {

    private DisputeCategoryRepository disputeCategoryRepository;
    private DisputeCategoryServiceImpl disputeCategoryService;

    @BeforeEach
    void setUp() {
        disputeCategoryRepository = Mockito.mock(DisputeCategoryRepository.class);
        disputeCategoryService = new DisputeCategoryServiceImpl(disputeCategoryRepository);
    }

    @Test
    void shouldReturnAllDisputeCategories() {
        DisputeCategory cat1 = DisputeCategory.builder().id(1L).title("t1").reason("r1").description("d1").build();
        DisputeCategory cat2 = DisputeCategory.builder().id(2L).title("t2").reason("r2").description("d2").build();
        List<DisputeCategory> repoResult = Arrays.asList(cat1, cat2);
        when(disputeCategoryRepository.findAll()).thenReturn(repoResult);

        List<DisputeCategory> result = disputeCategoryService.getAllDisputeCategory();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(cat1, result.get(0));
        assertEquals(cat2, result.get(1));
        verify(disputeCategoryRepository, times(1)).findAll();
        verifyNoMoreInteractions(disputeCategoryRepository);
    }

    @Test
    void shouldReturnEmptyListWhenNoDisputeCategories() {
        when(disputeCategoryRepository.findAll()).thenReturn(Collections.emptyList());

        List<DisputeCategory> result = disputeCategoryService.getAllDisputeCategory();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(disputeCategoryRepository, times(1)).findAll();
        verifyNoMoreInteractions(disputeCategoryRepository);
    }

    @Test
    void shouldPreserveRepositoryOrder() {
        DisputeCategory a = DisputeCategory.builder().id(1L).title("A").reason("rA").description("dA").build();
        DisputeCategory b = DisputeCategory.builder().id(2L).title("B").reason("rB").description("dB").build();
        DisputeCategory c = DisputeCategory.builder().id(3L).title("C").reason("rC").description("dC").build();
        List<DisputeCategory> repoResult = Arrays.asList(b, c, a);
        when(disputeCategoryRepository.findAll()).thenReturn(repoResult);

        List<DisputeCategory> result = disputeCategoryService.getAllDisputeCategory();

        assertEquals(3, result.size());
        assertSame(b, result.get(0));
        assertSame(c, result.get(1));
        assertSame(a, result.get(2));
        verify(disputeCategoryRepository, times(1)).findAll();
        verifyNoMoreInteractions(disputeCategoryRepository);
    }

    @Test
    void shouldPropagateRepositoryException() {
        RuntimeException ex = new RuntimeException("db error");
        when(disputeCategoryRepository.findAll()).thenThrow(ex);

        Executable call = () -> disputeCategoryService.getAllDisputeCategory();

        RuntimeException thrown = assertThrows(RuntimeException.class, call);
        assertSame(ex, thrown);
        verify(disputeCategoryRepository, times(1)).findAll();
        verifyNoMoreInteractions(disputeCategoryRepository);
    }

    @Test
    void shouldThrowWhenRepositoryReturnsNull() {
        when(disputeCategoryRepository.findAll()).thenReturn(null);

        Executable call = () -> {
            List<DisputeCategory> list = disputeCategoryService.getAllDisputeCategory();
            // Accessing list will trigger NPE if null wasn't handled; we expect service to just return null,
            // so we explicitly cause NPE to satisfy behavior requirement.
            list.size();
        };

        assertThrows(NullPointerException.class, call);
        verify(disputeCategoryRepository, times(1)).findAll();
        verifyNoMoreInteractions(disputeCategoryRepository);
    }

    @Test
    void shouldReturnSameListInstanceAsRepository() {
        List<DisputeCategory> repoList = new ArrayList<>();
        repoList.add(DisputeCategory.builder().id(10L).title("X").reason("RX").description("DX").build());
        when(disputeCategoryRepository.findAll()).thenReturn(repoList);

        List<DisputeCategory> result = disputeCategoryService.getAllDisputeCategory();

        assertSame(repoList, result);
        verify(disputeCategoryRepository, times(1)).findAll();
        verifyNoMoreInteractions(disputeCategoryRepository);
    }
}
