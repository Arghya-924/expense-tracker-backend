package com.project.expense_tracker_backend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.expense_tracker_backend.config.JwtGenerator;
import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.*;
import com.project.expense_tracker_backend.model.Category;
import com.project.expense_tracker_backend.model.Expense;
import com.project.expense_tracker_backend.repository.CategoryRepository;
import com.project.expense_tracker_backend.repository.ExpenseRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExpenseTrackerBackendApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(ExpenseTrackerBackendApplicationTests.class);
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtGenerator jwtGenerator;

    @Value("${jwt.secret}")
    private String JWT_SECRET;

    @Test
    @Order(1)
    void testSuccessfulTokenGeneration() {

        Authentication mockAuthentication =
                new UsernamePasswordAuthenticationToken("abcd", null);

        String token = jwtGenerator.generateToken(mockAuthentication);

        Assertions.assertFalse(token.isEmpty());

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));

        Claims claims = Jwts.parser().verifyWith(key)
                .build().parseSignedClaims(token).getPayload();

        Date expireDate = claims.getExpiration();

        String subject = claims.getSubject();

        assertEquals(ApplicationConstants.JWT_SUBJECT, subject);
        assertTrue(expireDate.after(new Date()));
    }

    @Test
    @Order(2)
    void testLoggingController_invalid_email() throws Exception {

        LoginRequestDto loginRequestDto = new LoginRequestDto("invalid_user@gmail.com", "12345");

        var loginResponse = mockMvc.perform(MockMvcRequestBuilders.post("/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andReturn();

        String authResponse = loginResponse.getResponse().getContentAsString();

        ErrorResponseDto errorResponseDto = objectMapper.readValue(authResponse, ErrorResponseDto.class);

        assertNotNull(errorResponseDto);
        assertEquals(ApplicationConstants.STATUS_FAILURE, errorResponseDto.getStatus());
        assertEquals(String.format(ApplicationConstants.EMAIL_NOT_FOUND, "invalid_user@gmail.com"),
                errorResponseDto.getErrorMessage().getFirst());

        assertSame(errorResponseDto.getStatusCode(), HttpStatus.BAD_REQUEST);
        assertTrue(errorResponseDto.getApiPath().contains("/public/login"));
    }

    @Test
    @Order(3)
    void testLoggingController_invalid_password() throws Exception {

        LoginRequestDto loginRequestDto = new LoginRequestDto("test1@gmail.com", "invalid_password");

        var loginResponse = mockMvc.perform(MockMvcRequestBuilders.post("/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andReturn();

        String authResponse = loginResponse.getResponse().getContentAsString();

        ErrorResponseDto errorResponseDto = objectMapper.readValue(authResponse, ErrorResponseDto.class);

        assertNotNull(errorResponseDto);
        assertEquals(ApplicationConstants.STATUS_FAILURE, errorResponseDto.getStatus());
        assertEquals(ApplicationConstants.BAD_CREDENTIALS,
                errorResponseDto.getErrorMessage().getFirst());

        assertSame(errorResponseDto.getStatusCode(), HttpStatus.FORBIDDEN);
        assertTrue(errorResponseDto.getApiPath().contains("/public/login"));

    }

    @Test
    @Order(4)
    void testLoggingControllerSuccess() throws Exception {

        LoginResponseDto loginResponseDto = loginUser("test1@gmail.com", "12345");

        assertNotNull(loginResponseDto);
        assertNotNull(loginResponseDto.getAuthToken());
        assertFalse(loginResponseDto.getAuthToken().isEmpty());
    }

    @Test
    @Order(5)
    void testGetUserExpensesFromExpenseController() throws Exception {

        long userId = 1L;

        LoginResponseDto mockLogin = loginUser("test1@gmail.com", "12345");

        var apiResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                        .header("Authorization", "Bearer " + mockLogin.getAuthToken())
//                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Deserialize response
        String responseContent = apiResponse.andReturn().getResponse().getContentAsString();

        UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse =
                objectMapper.readValue(responseContent, new TypeReference<>() {
                });

        List<ExpenseResponseDto> expenseResponseDtoList = userExpensesResponse.getUserExpenses();

        assertEquals(2, expenseResponseDtoList.size());

        assertEquals("iPhone", expenseResponseDtoList.getFirst().getDescription());

        assertEquals(25000.0, expenseResponseDtoList.getLast().getAmount());
    }

    @Test
    @Order(6)
    void testGetUserExpensesFromExpenseController_Year_Month_Format() throws Exception {

        LoginResponseDto mockLogin = loginUser("test1@gmail.com", "12345");

        var apiResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                        .header("Authorization", "Bearer " + mockLogin.getAuthToken())
                        .param("yearMonth", "slfgsa")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        log.info(apiResponse.andReturn().getResponse().getContentAsString());
    }

    @Test
    @Order(7)
    void testAddNewUserExpenseFromExpenseController() throws Exception {

        long userId = 2L;

        LoginResponseDto mockLogin = loginUser("test2@gmail.com", "12345");


        List<ExpenseRequestDto> userExpenses = List.of(
                new ExpenseRequestDto("Durga Puja dress", 5000.0, LocalDate.now(), "Shopping"),
                new ExpenseRequestDto("Movie", 1000.0, LocalDate.now(), "Entertainment"));


        var apiResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/expenses")
                        .header("Authorization", "Bearer " + mockLogin.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userExpenses)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].description").value("Durga Puja dress"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].category").value("Entertainment"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].description").value("Movie"));
    }

    @Test
    @Order(8)
    void testRegisterNewUser_Success() throws Exception {

        UserRegistrationDto newUserDetails = new
                UserRegistrationDto("Resham", "test@gmail.com", "12345", "0123456789");

        var apiResponse = mockMvc.perform(MockMvcRequestBuilders.post(ApplicationConstants.REGISTER_USER_API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDetails)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    @Order(9)
    void testRegisterNewUser_Duplicate_Email_ID() throws Exception {

        UserRegistrationDto newUserDetails = new
                UserRegistrationDto("Resham", "test@gmail.com", "12345", "0123456789");

        var apiResponse = mockMvc.perform(MockMvcRequestBuilders.post(ApplicationConstants.REGISTER_USER_API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDetails)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andReturn();

        String response = apiResponse.getResponse().getContentAsString();

        ErrorResponseDto errorResponseDto = objectMapper.readValue(response, ErrorResponseDto.class);

        assertEquals(String.format(ApplicationConstants.EMAIL_ALREADY_EXISTS, newUserDetails.getEmail()),
                errorResponseDto.getErrorMessage().getFirst());

        assertEquals(ApplicationConstants.STATUS_FAILURE, errorResponseDto.getStatus());

        assertSame(HttpStatus.BAD_REQUEST, errorResponseDto.getStatusCode());

    }

    @Test
    @Order(10)
    void testUpdateExpense_AmountAndDescription() throws Exception {

        LoginResponseDto mockLogin = loginUser("test2@gmail.com", "12345");

        var userExpenses = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                        .header("Authorization", "Bearer " + mockLogin.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Deserialize response
        String responseContent = userExpenses.andReturn().getResponse().getContentAsString();

        UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse =
                objectMapper.readValue(responseContent, new TypeReference<>() {
                });

        ExpenseResponseDto expenseResponse = userExpensesResponse.getUserExpenses().getFirst();

        ExpenseRequestDto updateExpense = new ExpenseRequestDto();
        updateExpense.setAmount(100.0);
        updateExpense.setDescription("aloo, pyaaj");

        var mockPatch = mockMvc.perform(MockMvcRequestBuilders.put("/api/expenses/{expenseId}", 2)
                        .header("Authorization", "Bearer " + mockLogin.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateExpense)))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(expenseResponse.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value(updateExpense.getDescription()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.amount").value(updateExpense.getAmount()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.category").value(expenseResponse.getCategory()));
    }

    @Test
    @Order(11)
    void testDeleteUserExpense_Successful() throws Exception {

        LoginResponseDto mockLogin = loginUser("test2@gmail.com", "12345");

        Optional<Expense> expense = expenseRepository.findById(2L);

        assertTrue(expense.isPresent());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/expenses/{expenseId}", 2L)
                        .header("Authorization", "Bearer " + mockLogin.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        Optional<Category> category = categoryRepository.findByCategoryName("Groceries");
        expense = expenseRepository.findById(2L);

        assertTrue(category.isPresent());
        assertTrue(expense.isEmpty());
    }

    private LoginResponseDto loginUser(String username, String password) throws Exception {
        LoginRequestDto loginRequestDto = new LoginRequestDto(username, password);

        var loginUser = mockMvc.perform(MockMvcRequestBuilders.post(ApplicationConstants.LOGIN_USER_API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String loginResponse = loginUser.getResponse().getContentAsString();

        return objectMapper.readValue(loginResponse, LoginResponseDto.class);
    }

    @Order(12)
    @Test
    void testAddNewExpensesAndCheckTotalExpenseAmount() throws Exception {

        LoginResponseDto loginUser = loginUser("test@gmail.com", "12345");

        List<ExpenseRequestDto> userExpenses = List.of(
                new ExpenseRequestDto("Pizza", 5000.0, LocalDate.now(), "Food"),
                new ExpenseRequestDto("Movie", 1000.0, LocalDate.now(), "Entertainment"),
                new ExpenseRequestDto("Food items", 2000.0, LocalDate.of(2024, 7, 4), "Groceries"),
                new ExpenseRequestDto("Dress", 5000.0, LocalDate.of(2024, 7, 15), "Shopping"),
                new ExpenseRequestDto("Book", 300.0, LocalDate.of(2024, 7, 19), "Self help"));


        mockMvc.perform(MockMvcRequestBuilders.post("/api/expenses")
                        .header("Authorization", "Bearer " + loginUser.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userExpenses)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].description").value("Pizza"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].category").value("Entertainment"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].description").value("Movie"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].amount").value(5000.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].category").value("Food"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].category").value("Groceries"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].description").value("Food items"));

        var getResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                        .header("Authorization", "Bearer " + loginUser.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String responseContent = getResponse.getResponse().getContentAsString();

        UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse =
                objectMapper.readValue(responseContent, new TypeReference<>() {
                });

        assertEquals(6000.0, userExpensesResponse.getTotalMonthlyExpense());

        List<ExpenseResponseDto> expenseResponseDtoList = userExpensesResponse.getUserExpenses();

        assertEquals(2, expenseResponseDtoList.size());

        assertEquals("Pizza", expenseResponseDtoList.getFirst().getDescription());

        assertEquals(1000.0, expenseResponseDtoList.getLast().getAmount());

    }

    @Test
    @Order(13)
    void testGetUserExpensesForPreviousMonth() throws Exception {
        LoginResponseDto loginUser = loginUser("test@gmail.com", "12345");

        String yearMonth = "2024-07";

        var response = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                        .header("Authorization", "Bearer " + loginUser.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("yearMonth", yearMonth))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String responseContent = response.getResponse().getContentAsString();

        UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse = objectMapper.readValue(responseContent, new TypeReference<>() {});

        List<ExpenseResponseDto> expenseResponseDtoList = userExpensesResponse.getUserExpenses();

        assertEquals(3, expenseResponseDtoList.size());
        assertEquals("Food items", expenseResponseDtoList.getFirst().getDescription());
        assertEquals("Self help", expenseResponseDtoList.getLast().getCategory());

        assertEquals(7300.0, userExpensesResponse.getTotalMonthlyExpense());

    }
}
