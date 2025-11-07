package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.filter.BadWordFilter;
import Green_trade.green_trade_platform.mapper.ReviewMapper;
import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.model.Review;
import Green_trade.green_trade_platform.model.ReviewImage;
import Green_trade.green_trade_platform.repository.OrderRepository;
import Green_trade.green_trade_platform.repository.ReviewImagesRepository;
import Green_trade.green_trade_platform.repository.ReviewRepository;
import Green_trade.green_trade_platform.request.ReviewRequest;
import Green_trade.green_trade_platform.service.implement.CloudinaryService;
import Green_trade.green_trade_platform.service.implement.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReviewServiceTest {

    @Mock
    private BadWordFilter badWordFilter;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private ReviewImagesRepository reviewImagesRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private ReviewRequest buildRequest(Long orderId, double rating, String feedback) {
        return ReviewRequest.builder()
                .orderId(orderId)
                .rating(rating)
                .feedback(feedback)
                .build();
    }

    private MultipartFile mockFile(String name, byte[] bytes) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(name);
        when(file.getBytes()).thenReturn(bytes);
        return file;
    }

    @BeforeEach
    void setup() {
        // no-op
    }

    @Test
    void shouldCreateReviewWithImagesSuccessfully() throws IOException {
        Long orderId = 10L;
        Order order = Order.builder().id(orderId).build();
        ReviewRequest request = buildRequest(orderId, 4.5, "Great product!");

        // mocks
        when(orderRepository.findOrderById(orderId)).thenReturn(Optional.of(order));
        when(badWordFilter.containsBadWord(request.getFeedback())).thenReturn(false);

        MultipartFile file1 = mockFile("img1.jpg", new byte[]{1, 2});
        MultipartFile file2 = mockFile("img2.jpg", new byte[]{3, 4});

        Map<String, String> uploadRes1 = Map.of("fileUrl", "https://cdn/img1.jpg", "publicId", "pub1");
        Map<String, String> uploadRes2 = Map.of("fileUrl", "https://cdn/img2.jpg", "publicId", "pub2");

        when(cloudinaryService.upload(file1, "reviews")).thenReturn(uploadRes1);
        when(cloudinaryService.upload(file2, "reviews")).thenReturn(uploadRes2);

        // mapper -> entity
        ArgumentCaptor<List<ReviewImage>> imagesCaptor = ArgumentCaptor.forClass(List.class);
        Review mappedReview = Review.builder().rating(request.getRating()).feedback(request.getFeedback()).build();
        when(reviewMapper.toEntity(eq(request), imagesCaptor.capture())).thenReturn(mappedReview);

        Review savedReview = Review.builder().id(100L).build();
        when(reviewRepository.save(mappedReview)).thenReturn(savedReview);

        Review result = reviewService.createReview(request, Arrays.asList(file1, file2));

        assertNotNull(result);
        assertEquals(100L, result.getId());

        // verify images passed to mapper
        List<ReviewImage> imagesPassed = imagesCaptor.getValue();
        assertEquals(2, imagesPassed.size());
        assertEquals("https://cdn/img1.jpg", imagesPassed.get(0).getImageUrl());
        assertEquals("pub1", imagesPassed.get(0).getPublicImageId());
        assertEquals(1, imagesPassed.get(0).getOrderImage());
        assertEquals("https://cdn/img2.jpg", imagesPassed.get(1).getImageUrl());
        assertEquals("pub2", imagesPassed.get(1).getPublicImageId());
        assertEquals(2, imagesPassed.get(1).getOrderImage());

        // verify associations persisted
        verify(reviewRepository, times(1)).save(mappedReview);
        verify(reviewImagesRepository, times(2)).save(any(ReviewImage.class));
        verify(badWordFilter).containsBadWord("Great product!");
        verify(orderRepository).findOrderById(orderId);
    }

    @Test
    void shouldCreateReviewWithoutImages() throws IOException {
        Long orderId = 11L;
        Order order = Order.builder().id(orderId).build();
        ReviewRequest request = buildRequest(orderId, 5.0, "Excellent!");

        when(orderRepository.findOrderById(orderId)).thenReturn(Optional.of(order));
        when(badWordFilter.containsBadWord(request.getFeedback())).thenReturn(false);

        ArgumentCaptor<List<ReviewImage>> imagesCaptor = ArgumentCaptor.forClass(List.class);
        Review mappedReview = Review.builder().rating(request.getRating()).feedback(request.getFeedback()).build();
        when(reviewMapper.toEntity(eq(request), imagesCaptor.capture())).thenReturn(mappedReview);

        Review savedReview = Review.builder().id(200L).build();
        when(reviewRepository.save(mappedReview)).thenReturn(savedReview);

        Review result = reviewService.createReview(request, Collections.emptyList());

        assertNotNull(result);
        assertEquals(200L, result.getId());

        List<ReviewImage> imagesPassed = imagesCaptor.getValue();
        assertNotNull(imagesPassed);
        assertTrue(imagesPassed.isEmpty());

        verify(reviewImagesRepository, never()).save(any());
        verify(cloudinaryService, never()).upload(any(), anyString());
    }

    @Test
    void shouldGetReviewByOrderId() {
        Long orderId = 22L;
        Review review = Review.builder().id(300L).build();
        when(reviewRepository.findByOrder_Id(orderId)).thenReturn(Optional.of(review));

        Review result = reviewService.getReviewsByOrderId(orderId);

        assertNotNull(result);
        assertEquals(300L, result.getId());
        verify(reviewRepository).findByOrder_Id(orderId);
    }

    @Test
    void shouldThrowWhenFeedbackContainsBadWords() {
        Long orderId = 12L;
        Order order = Order.builder().id(orderId).build();
        ReviewRequest request = buildRequest(orderId, 3.0, "bad words here");

        when(orderRepository.findOrderById(orderId)).thenReturn(Optional.of(order));
        when(badWordFilter.containsBadWord(request.getFeedback())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.createReview(request, Collections.emptyList()));
        assertEquals("This feedback contains bad words. Please write again.", ex.getMessage());

        verify(reviewRepository, never()).save(any());
        verify(reviewImagesRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRatingOutOfRange() {
        Long orderId = 13L;
        Order order = Order.builder().id(orderId).build();
        ReviewRequest request = buildRequest(orderId, 6.5, "nice");

        when(orderRepository.findOrderById(orderId)).thenReturn(Optional.of(order));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reviewService.createReview(request, Collections.emptyList()));
        assertEquals("Rating must be in from 0 to 5.", ex.getMessage());

        verify(reviewRepository, never()).save(any());
        verify(reviewImagesRepository, never()).save(any());
        verify(badWordFilter, never()).containsBadWord(any());
    }

    @Test
    void shouldCreateReviewWhenSomeImageUploadsFail() throws IOException {
        Long orderId = 14L;
        Order order = Order.builder().id(orderId).build();
        ReviewRequest request = buildRequest(orderId, 4.0, "Works fine");

        when(orderRepository.findOrderById(orderId)).thenReturn(Optional.of(order));
        when(badWordFilter.containsBadWord(request.getFeedback())).thenReturn(false);

        MultipartFile file1 = mockFile("ok1.jpg", new byte[]{1});
        MultipartFile file2 = mockFile("bad.jpg", new byte[]{2});
        MultipartFile file3 = mockFile("ok2.jpg", new byte[]{3});

        Map<String, String> uploadRes1 = Map.of("fileUrl", "https://cdn/ok1.jpg", "publicId", "p1");
        Map<String, String> uploadRes3 = Map.of("fileUrl", "https://cdn/ok2.jpg", "publicId", "p3");

        when(cloudinaryService.upload(file1, "reviews")).thenReturn(uploadRes1);
        when(cloudinaryService.upload(file2, "reviews")).thenReturn(null); // simulate failure
        when(cloudinaryService.upload(file3, "reviews")).thenReturn(uploadRes3);

        ArgumentCaptor<List<ReviewImage>> imagesCaptor = ArgumentCaptor.forClass(List.class);
        Review mappedReview = Review.builder().rating(request.getRating()).feedback(request.getFeedback()).build();
        when(reviewMapper.toEntity(eq(request), imagesCaptor.capture())).thenReturn(mappedReview);

        Review savedReview = Review.builder().id(400L).build();
        when(reviewRepository.save(mappedReview)).thenReturn(savedReview);

        Review result = reviewService.createReview(request, Arrays.asList(file1, file2, file3));

        assertNotNull(result);
        assertEquals(400L, result.getId());

        List<ReviewImage> imagesPassed = imagesCaptor.getValue();
        assertEquals(2, imagesPassed.size());
        assertEquals("https://cdn/ok1.jpg", imagesPassed.get(0).getImageUrl());
        assertEquals("p1", imagesPassed.get(0).getPublicImageId());
        assertEquals(1, imagesPassed.get(0).getOrderImage());

        assertEquals("https://cdn/ok2.jpg", imagesPassed.get(1).getImageUrl());
        assertEquals("p3", imagesPassed.get(1).getPublicImageId());
        assertEquals(2, imagesPassed.get(1).getOrderImage());

        verify(reviewImagesRepository, times(2)).save(any(ReviewImage.class));
    }
}
