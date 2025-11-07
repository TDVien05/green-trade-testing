package Green_trade.green_trade_platform.service;


import Green_trade.green_trade_platform.enumerate.AccountStatus;
import Green_trade.green_trade_platform.mapper.AdminMapper;
import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.repository.AdminRepository;
import Green_trade.green_trade_platform.request.CreateAdminRequest;
import Green_trade.green_trade_platform.request.MailRequest;
import Green_trade.green_trade_platform.service.implement.AdminServiceImpl;
import Green_trade.green_trade_platform.service.implement.CloudinaryService;
import Green_trade.green_trade_platform.service.implement.MailServiceImpl;
import Green_trade.green_trade_platform.util.StringUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private StringUtils stringUtils;
    @Mock
    private AdminMapper adminMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private MailServiceImpl mailService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private CreateAdminRequest createRequest;
    private Admin mappedAdmin;
    private MultipartFile avatarFile;

    @BeforeEach
    void setUp() {
        createRequest = CreateAdminRequest.builder()
                .employeeNumber("1234567890")
                .password("Passw0rd!")
                .fullName("john doe")
                .phoneNumber("0123456789")
                .email("john.doe@example.com")
                .build();

        mappedAdmin = Admin.builder()
                .id(1L)
                .employeeNumber(createRequest.getEmployeeNumber())
                .password(createRequest.getPassword())
                .fullName(createRequest.getFullName())
                .phoneNumber(createRequest.getPhoneNumber())
                .email(createRequest.getEmail())
                .status(AccountStatus.ACTIVE)
                .build();

        avatarFile = mock(MultipartFile.class);
    }

    @Test
    void shouldCreateAdminAndSendEmailWhenInputsAreValid() throws IOException {
        when(adminRepository.existsByEmployeeNumber(createRequest.getEmployeeNumber())).thenReturn(false);
        when(adminRepository.existsByPhoneNumber(createRequest.getPhoneNumber())).thenReturn(false);
        when(adminRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);

        when(adminMapper.toEntity(createRequest)).thenReturn(mappedAdmin);
        when(stringUtils.formatFullName("john doe")).thenReturn("John Doe");
        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPwd");
        Map<String, String> uploadRes = Map.of("publicId", "pub-1", "fileUrl", "http://cdn/avatar.jpg");
        when(cloudinaryService.upload(eq(avatarFile), anyString())).thenReturn(uploadRes);

        Admin saved = Admin.builder()
                .id(10L)
                .employeeNumber(mappedAdmin.getEmployeeNumber())
                .password("encodedPwd")
                .fullName("John Doe")
                .phoneNumber(mappedAdmin.getPhoneNumber())
                .email(mappedAdmin.getEmail())
                .avatarPublicId("pub-1")
                .avatarUrl("http://cdn/avatar.jpg")
                .status(AccountStatus.ACTIVE)
                .build();
        when(adminRepository.save(any(Admin.class))).thenReturn(saved);

        Admin result = adminService.handleCreateAdminAccount(avatarFile, createRequest);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("John Doe", result.getFullName());
        assertEquals("encodedPwd", result.getPassword());
        assertEquals("http://cdn/avatar.jpg", result.getAvatarUrl());
        assertEquals("pub-1", result.getAvatarPublicId());

        verify(adminRepository).existsByEmployeeNumber(createRequest.getEmployeeNumber());
        verify(adminRepository).existsByPhoneNumber(createRequest.getPhoneNumber());
        verify(adminRepository).existsByEmail(createRequest.getEmail());
        verify(adminMapper).toEntity(createRequest);
        verify(stringUtils).formatFullName("john doe");
        verify(passwordEncoder).encode("Passw0rd!");
        verify(cloudinaryService).upload(eq(avatarFile), eq("admin/John Doe/avatar"));
        verify(adminRepository).save(any(Admin.class));

        ArgumentCaptor<MailRequest> mailCaptor = ArgumentCaptor.forClass(MailRequest.class);
        verify(mailService, times(1)).sendBeautifulMail(mailCaptor.capture());
        MailRequest sentMail = mailCaptor.getValue();
        assertEquals("green.trade.platform.391@gmail.com", sentMail.getFrom());
        assertEquals("john.doe@example.com", sentMail.getTo());
        assertTrue(sentMail.getSubject().contains("Tài khoản quản trị viên mới"));
        assertNotNull(sentMail.getMessage());
        assertFalse(sentMail.getMessage().isBlank());
    }

    @Test
    void shouldBlockAdminAndSendNotification() {
        Admin existing = Admin.builder()
                .id(5L)
                .fullName("Jane Admin")
                .email("jane.admin@example.com")
                .status(AccountStatus.ACTIVE)
                .build();

        when(adminRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(adminRepository.save(any(Admin.class))).thenAnswer(inv -> inv.getArgument(0));

        adminService.blockAccount(5L, "Violation of policy", "block");

        assertEquals(AccountStatus.INACTIVE, existing.getStatus());
        verify(adminRepository).save(existing);

        ArgumentCaptor<MailRequest> mailCaptor = ArgumentCaptor.forClass(MailRequest.class);
        verify(mailService, times(1)).sendBeautifulMail(mailCaptor.capture());
        MailRequest mail = mailCaptor.getValue();
        assertEquals("jane.admin@example.com", mail.getTo());
        assertTrue(mail.getSubject().contains("bị khóa"));
        assertTrue(mail.getMessage().contains("Violation of policy"));
        assertTrue(mail.getMessage().contains("Jane Admin"));
    }

    @Test
    void shouldReturnPagedAdminsSortedByIdDesc() {
        int page = 1, size = 3;
        List<Admin> content = Arrays.asList(
                Admin.builder().id(9L).build(),
                Admin.builder().id(8L).build()
        );
        Page<Admin> pageResult = new PageImpl<>(content, PageRequest.of(page, size, Sort.by("id").descending()), 10);
        when(adminRepository.findAll(any(Pageable.class))).thenReturn(pageResult);

        Page<Admin> result = adminService.getAdminList(page, size);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(9L, result.getContent().get(0).getId());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminRepository).findAll(pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertEquals(page, used.getPageNumber());
        assertEquals(size, used.getPageSize());
        assertEquals(Sort.by("id").descending(), used.getSort());
    }

    @Test
    void shouldFailCreateWhenEmployeeNumberExists() {
        when(adminRepository.existsByEmployeeNumber(createRequest.getEmployeeNumber())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adminService.handleCreateAdminAccount(avatarFile, createRequest));

        assertEquals("Duplicate employee number.", ex.getMessage());
        verify(adminRepository, never()).existsByPhoneNumber(anyString());
        verify(adminRepository, never()).existsByEmail(anyString());
        verifyNoInteractions(adminMapper, stringUtils, passwordEncoder, cloudinaryService, mailService);
    }

    @Test
    void shouldThrowWhenActivityIsInvalid() {
        Admin existing = Admin.builder()
                .id(7L)
                .fullName("John Admin")
                .email("john.admin@example.com")
                .status(AccountStatus.ACTIVE)
                .build();

        when(adminRepository.findById(7L)).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adminService.blockAccount(7L, "N/A", "freeze"));

        assertEquals("Activity must be 'block' or 'unblock'", ex.getMessage());
        verify(adminRepository, never()).save(any());
        verifyNoInteractions(mailService);
    }

    @Test
    void shouldThrowWhenAdminProfileNotFound() {
        when(adminRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminService.getAdminProfile(100L));
    }
}
