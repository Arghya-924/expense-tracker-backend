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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
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

        if(token != null && token.startsWith("Bearer") && SecurityContextHolder.getContext().getAuthentication() == null) {

            token = token.substring(7);

            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));

            Claims claims;

            try{
                claims = Jwts.parser().verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            }
            catch(ExpiredJwtException expiredJwtException) {
                log.error("Invalid Token received | {}", expiredJwtException.getLocalizedMessage());

                ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                        ApplicationConstants.STATUS_FAILURE,
                        HttpStatus.BAD_REQUEST,
                        request.getRequestURI(),
                        List.of(expiredJwtException.getLocalizedMessage())
                );

                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpStatus.BAD_REQUEST.value());

                response.getWriter().write(objectMapper.writeValueAsString(errorResponseDto));

                return;
            }
            catch (JwtException exception) {

                log.error("Invalid Token received | {}", exception.getLocalizedMessage());

                ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                        ApplicationConstants.STATUS_FAILURE,
                        HttpStatus.FORBIDDEN,
                        request.getRequestURI(),
                        List.of(exception.getLocalizedMessage())
                );

                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpStatus.FORBIDDEN.value());

                response.getWriter().write(objectMapper.writeValueAsString(errorResponseDto));

                return;
            }

            String email = claims.get("email").toString();
            long userId = extractUserIdFromEmail(email);

            log.info("Email received from token: {}", email);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(email, null, List.of());

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            request.setAttribute(ApplicationConstants.REQUEST_USER_ID_ATTRIBUTE, userId);
        }
        filterChain.doFilter(request,response);
    }

    private long extractUserIdFromEmail(String email) {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if(optionalUser.isEmpty()) throw new EmailNotFoundException(ApplicationConstants.EMAIL_NOT_FOUND, email);

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
