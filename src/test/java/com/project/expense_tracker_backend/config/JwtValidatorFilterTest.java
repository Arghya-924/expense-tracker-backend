package com.project.expense_tracker_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.ErrorResponseDto;
import com.project.expense_tracker_backend.exception.EmailNotFoundException;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtValidatorFilterTest {

    // A 256-bit Base64 encoded secret key for testing (HS256 needs at least 256 bits)
    // This is equivalent to a 32-byte array.
    private static final String TEST_JWT_SECRET = "YWFyb25fYmFzZW1lbnRfbW9ybW9uX2Rvb3JfcGxhdGludW1fc2hpcmFzZTNyYXM=";
    private SecretKey testSecretKey;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;
    
    private ObjectMapper objectMapper;
    
    private JwtValidatorFilter jwtValidatorFilter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper(); // Real ObjectMapper
        jwtValidatorFilter = new JwtValidatorFilter();

        // Manually inject dependencies using ReflectionTestUtils
        ReflectionTestUtils.setField(jwtValidatorFilter, "userRepository", userRepository);
        ReflectionTestUtils.setField(jwtValidatorFilter, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(jwtValidatorFilter, "jwtSecret", TEST_JWT_SECRET);

        // Initialize the secret key after jwtSecret is set
        jwtValidatorFilter.init(); 
        testSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_JWT_SECRET));
        
        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }
    
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String generateTestToken(String email, Date expiration, SecretKey key) {
        return Jwts.builder()
                .claim("email", email)
                .issuer(ApplicationConstants.JWT_ISSUER)
                .issuedAt(new Date())
                .expiration(expiration)
                .encryptWith(key, Jwts.ENC.A128CBC_HS256) // Use JWE standard
                .compact();
    }
    
    private String generatePotentiallyMalformedToken(String email, Date expiration, SecretKey key) {
        // This token is signed, not encrypted, so filter should reject it as malformed JWE
        return Jwts.builder()
                .claim("email", email)
                .issuer(ApplicationConstants.JWT_ISSUER)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256) 
                .compact();
    }


    @Test
    void doFilterInternal_validToken_shouldAuthenticate() throws ServletException, IOException {
        String email = "test@example.com";
        Long userId = 1L;
        Date expiration = new Date(System.currentTimeMillis() + 600000); // 10 mins
        String token = generateTestToken(email, expiration, testSecretKey);

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail(email);

        when(request.getHeader(ApplicationConstants.JWT_AUTH_HEADER)).thenReturn("Bearer " + token);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        jwtValidatorFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(email, authentication.getName());
        verify(request).setAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE, userId.toString());
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        verify(response, never()).getWriter();
    }

    @Test
    void doFilterInternal_invalidToken_malformed_shouldReject() throws ServletException, IOException {
        String token = "this.is.not.a.valid.jwe.token"; // Malformed JWE
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getHeader(ApplicationConstants.JWT_AUTH_HEADER)).thenReturn("Bearer " + token);
        when(response.getWriter()).thenReturn(printWriter);

        jwtValidatorFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        verify(response.getWriter()).write(objectMapper.writeValueAsString(new ErrorResponseDto(ApplicationConstants.INVALID_TOKEN, HttpStatus.FORBIDDEN)));
        printWriter.flush(); // Ensure content is written to stringWriter
        // Example: assertTrue(stringWriter.toString().contains(ApplicationConstants.INVALID_TOKEN));
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    
    @Test
    void doFilterInternal_invalidToken_wrongSignatureType_shouldReject() throws ServletException, IOException {
        // Generate a JWT (signed) instead of JWE (encrypted)
        String email = "test@example.com";
        Date expiration = new Date(System.currentTimeMillis() + 600000); // 10 mins
        String token = generatePotentiallyMalformedToken(email, expiration, testSecretKey);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getHeader(ApplicationConstants.JWT_AUTH_HEADER)).thenReturn("Bearer " + token);
        when(response.getWriter()).thenReturn(printWriter);

        jwtValidatorFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        verify(response.getWriter()).write(objectMapper.writeValueAsString(new ErrorResponseDto(ApplicationConstants.INVALID_TOKEN, HttpStatus.FORBIDDEN)));
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }


    @Test
    void doFilterInternal_expiredToken_shouldReject() throws ServletException, IOException {
        String email = "test@example.com";
        Date expiration = new Date(System.currentTimeMillis() - 60000); // Expired 1 min ago
        String token = generateTestToken(email, expiration, testSecretKey);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getHeader(ApplicationConstants.JWT_AUTH_HEADER)).thenReturn("Bearer " + token);
        when(response.getWriter()).thenReturn(printWriter);

        jwtValidatorFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
        verify(response.getWriter()).write(objectMapper.writeValueAsString(new ErrorResponseDto(ApplicationConstants.EXPIRED_TOKEN, HttpStatus.BAD_REQUEST)));
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_noToken_shouldPassThrough() throws ServletException, IOException {
        when(request.getHeader(ApplicationConstants.JWT_AUTH_HEADER)).thenReturn(null);

        jwtValidatorFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response, never()).setStatus(anyInt());
    }
    
    @Test
    void doFilterInternal_tokenWithoutBearerPrefix_shouldPassThrough() throws ServletException, IOException {
        String email = "test@example.com";
        Date expiration = new Date(System.currentTimeMillis() + 600000);
        String token = generateTestToken(email, expiration, testSecretKey);

        when(request.getHeader(ApplicationConstants.JWT_AUTH_HEADER)).thenReturn(token); // No "Bearer " prefix

        jwtValidatorFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response, never()).setStatus(anyInt());
    }


    @Test
    void doFilterInternal_tokenForNonExistentUser_shouldThrowEmailNotFound() throws ServletException, IOException {
        String email = "unknown@example.com";
        Date expiration = new Date(System.currentTimeMillis() + 600000);
        String token = generateTestToken(email, expiration, testSecretKey);

        when(request.getHeader(ApplicationConstants.JWT_AUTH_HEADER)).thenReturn("Bearer " + token);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Exception exception = assertThrows(EmailNotFoundException.class, () -> {
            jwtValidatorFilter.doFilterInternal(request, response, filterChain);
        });
        
        assertEquals(String.format(ApplicationConstants.USER_NOT_FOUND_MSG_TEMPLATE, email), exception.getMessage());

        verify(filterChain, never()).doFilter(request, response); // Assuming exception stops further processing in filter
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotFilter_publicPath_shouldReturnTrue() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn(ApplicationConstants.LOGIN_USER_API_PATH);
        assertTrue(jwtValidatorFilter.shouldNotFilter(request));
        
        // Also test the main doFilter method for this path
        jwtValidatorFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response); // Directly passed through
        verify(request, never()).getHeader(ApplicationConstants.JWT_AUTH_HEADER); // doFilterInternal not called
    }

    @Test
    void shouldNotFilter_nonPublicPath_shouldReturnFalse() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/user/someAction");
        assertFalse(jwtValidatorFilter.shouldNotFilter(request));
    }
    
    @Test
    void doFilter_publicPath_shouldPassThrough() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn(ApplicationConstants.REGISTER_USER_API_PATH);
        
        jwtValidatorFilter.doFilter(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        verify(request, never()).getHeader(ApplicationConstants.JWT_AUTH_HEADER); // Verifies doFilterInternal was not called by checking one of its first actions
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_protectedPath_shouldInvokeDoFilterInternal() throws ServletException, IOException {
        // This test is a bit more complex as it calls the public doFilter, which then calls doFilterInternal.
        // We'll set up for a valid token scenario to see if doFilterInternal's logic is hit.
        String email = "test@example.com";
        Long userId = 1L;
        Date expiration = new Date(System.currentTimeMillis() + 600000);
        String token = generateTestToken(email, expiration, testSecretKey);
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail(email);

        when(request.getServletPath()).thenReturn("/user/profile"); // A protected path
        when(request.getHeader(ApplicationConstants.JWT_AUTH_HEADER)).thenReturn("Bearer " + token);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        jwtValidatorFilter.doFilter(request, response, filterChain);

        // Verify that doFilterInternal was effectively called by checking its side effects
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getName());
        verify(request).setAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE, userId.toString());
        verify(filterChain).doFilter(request, response);
    }
}
