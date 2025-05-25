package com.project.expense_tracker_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.ErrorResponseDto;
import com.project.expense_tracker_backend.exception.EmailNotFoundException;
import com.project.expense_tracker_backend.model.User;
import com.project.expense_tracker_backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie; // Ensure Cookie import
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class JwtValidatorFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String JWT_SECRET;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader(ApplicationConstants.JWT_AUTH_HEADER);
        String source = "header"; // For logging or debugging

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            // Token not in header or not a Bearer token, try finding it in cookies
            token = null; // Reset token
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (ApplicationConstants.JWT_COOKIE_NAME.equals(cookie.getName())) {
                        token = cookie.getValue();
                        source = "cookie";
                        break;
                    }
                }
            }
        }

        // Proceed only if a token was found (either from header or cookie)
        // AND authentication is not already set
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
            Claims claims;

            try {
                claims = Jwts.parser().decryptWith(key)
                        .build()
                        .parseEncryptedClaims(token)
                        .getPayload();
            } catch (ExpiredJwtException expiredJwtException) {
                log.error("Expired Token received from {} | {}", source, expiredJwtException.getLocalizedMessage());
                ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                        ApplicationConstants.EXPIRED_TOKEN, // Using a more specific constant if available
                        HttpStatus.BAD_REQUEST
                );
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.getWriter().write(objectMapper.writeValueAsString(errorResponseDto));
                return;
            } catch (JwtException exception) {
                log.error("Invalid Token received from {} | {}", source, exception.getLocalizedMessage());
                ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                        ApplicationConstants.INVALID_TOKEN, // Using a more specific constant if available
                        HttpStatus.FORBIDDEN
                );
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.getWriter().write(objectMapper.writeValueAsString(errorResponseDto));
                return;
            }

            String email = claims.get("email").toString();
            // The original code had extractUserIdFromEmail which could throw EmailNotFoundException.
            // This should be wrapped or handled if we want to avoid early exit on user not found for API calls.
            // For UI, if a cookie token is for a deleted user, this exception is okay.
            long userId;
            try {
                Optional<User> optionalUser = userRepository.findByEmail(email);
                if (optionalUser.isEmpty()) {
                     log.warn("User not found for email {} from token source {}", email, source);
                     // Decide how to handle: clear cookie and proceed, or send error
                     // For now, let it proceed to filterChain without authentication if user not found
                     // or re-throw specific exception if API requires strict user presence for valid token.
                     // For UI flow, this might mean redirecting to login.
                     // Let's re-throw for now, as it's existing behavior from extractUserIdFromEmail.
                     throw new EmailNotFoundException(String.format(ApplicationConstants.USER_NOT_FOUND_MSG_TEMPLATE, email));
                }
                userId = optionalUser.get().getId(); // Assuming User model has getId() for Long
            } catch (EmailNotFoundException e) {
                // Handle EmailNotFoundException specifically if needed, e.g., clear cookie
                log.warn("EmailNotFoundException for email {} from token source {}: {}", email, source, e.getMessage());
                 ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                        e.getMessage(), // Or a generic "User not found" message
                        HttpStatus.UNAUTHORIZED // Or FORBIDDEN
                );
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write(objectMapper.writeValueAsString(errorResponseDto));
                return;
            }


            log.info("Token successfully validated from {} for email: {}", source, email);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(email, null, List.of()); // No authorities for now

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            request.setAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE, String.valueOf(userId)); // Ensure userId is string
        }
        filterChain.doFilter(request, response);
    }

    // extractUserIdFromEmail is no longer directly called from doFilterInternal
    // but its logic is integrated above. If it's used elsewhere, it can be kept.
    // For this refactoring, we'll assume its logic is now self-contained within doFilterInternal's try-catch for user lookup.
    // If we want to keep it, it should not throw exception but return Optional<Long> perhaps.
    // For now, commenting it out to avoid unused private method warning, assuming its logic is now inline.
    /*
    private long extractUserIdFromEmail(String email) {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) throw new EmailNotFoundException(ApplicationConstants.EMAIL_NOT_FOUND, email);

        return optionalUser.get().getUserId();
    }
    */

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) throw new EmailNotFoundException(ApplicationConstants.EMAIL_NOT_FOUND, email);

        return optionalUser.get().getUserId();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        List<String> whitelisted = List.of(ApplicationConstants.LOGIN_USER_API_PATH,
                ApplicationConstants.REGISTER_USER_API_PATH);

        return whitelisted.contains(request.getServletPath());

//        return request.getServletPath().equals("/public/login");
    }
}
