package com.project.expense_tracker_backend;

import com.project.expense_tracker_backend.config.JwtGenerator;
import com.project.expense_tracker_backend.constants.ApplicationConstants;
import com.project.expense_tracker_backend.dto.*;
import com.project.expense_tracker_backend.model.Category;
import com.project.expense_tracker_backend.model.Expense;
import com.project.expense_tracker_backend.repository.CategoryRepository;
import com.project.expense_tracker_backend.repository.ExpenseRepository;
import com.project.expense_tracker_backend.util.DateUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.time.Month;
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

                Authentication mockAuthentication = new UsernamePasswordAuthenticationToken("abcd", null);

                String token = jwtGenerator.generateToken(mockAuthentication).token();

                Assertions.assertFalse(token.isEmpty());

                SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));

                Claims claims = Jwts.parser().decryptWith(key)
                                .build().parseEncryptedClaims(token).getPayload();

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

                assertSame(HttpStatus.BAD_REQUEST, errorResponseDto.getStatusCode());
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

                assertSame(HttpStatus.FORBIDDEN, errorResponseDto.getStatusCode());
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
                                // .param("userId", String.valueOf(userId))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                // Deserialize response
                String responseContent = apiResponse.andReturn().getResponse().getContentAsString();

                UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse = objectMapper
                                .readValue(responseContent, new TypeReference<>() {
                                });

                List<ExpenseResponseDto> expenseResponseDtoList = userExpensesResponse.getUserExpenses();

                assertFalse(expenseResponseDtoList.isEmpty());

                assertEquals("iPhone 15 Pro", expenseResponseDtoList.getFirst().getDescription());

                assertEquals("Shopping", expenseResponseDtoList.getFirst().getCategory());
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

                LoginResponseDto mockLogin = loginUser("test2@gmail.com", "123456");

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

                UserRegistrationDto newUserDetails = new UserRegistrationDto("Resham", "test@gmail.com", "12345",
                                "0123456789");

                var apiResponse = mockMvc
                                .perform(MockMvcRequestBuilders.post(ApplicationConstants.REGISTER_USER_API_PATH)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(newUserDetails)))
                                .andExpect(MockMvcResultMatchers.status().isCreated());
        }

        @Test
        @Order(9)
        void testRegisterNewUser_Duplicate_Email_ID() throws Exception {

                UserRegistrationDto newUserDetails = new UserRegistrationDto("Resham", "test@gmail.com", "12345",
                                "0123456789");

                var apiResponse = mockMvc
                                .perform(MockMvcRequestBuilders.post(ApplicationConstants.REGISTER_USER_API_PATH)
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

                LoginResponseDto mockLogin = loginUser("test2@gmail.com", "123456");

                var userExpenses = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                                .header("Authorization", "Bearer " + mockLogin.getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                // Deserialize response
                String responseContent = userExpenses.andReturn().getResponse().getContentAsString();

                UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse = objectMapper
                                .readValue(responseContent, new TypeReference<>() {
                                });

                ExpenseResponseDto expenseResponse = userExpensesResponse.getUserExpenses().getFirst();
                Long expenseId = expenseResponse.getId();

                ExpenseRequestDto updateExpense = new ExpenseRequestDto();
                updateExpense.setAmount(100.0);
                updateExpense.setDescription("aloo, pyaaj");

                mockMvc.perform(MockMvcRequestBuilders.put("/api/expenses/{expenseId}", expenseId)
                                .header("Authorization", "Bearer " + mockLogin.getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateExpense)))
                                .andExpect(MockMvcResultMatchers.status().isAccepted())
                                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(expenseId))
                                .andExpect(MockMvcResultMatchers.jsonPath("$.description")
                                                .value(updateExpense.getDescription()))
                                .andExpect(MockMvcResultMatchers.jsonPath("$.amount").value(updateExpense.getAmount()))
                                .andExpect(MockMvcResultMatchers.jsonPath("$.category")
                                                .value(expenseResponse.getCategory()));
        }

        @Test
        @Order(11)
        void testDeleteUserExpense_Successful() throws Exception {

                LoginResponseDto mockLogin = loginUser("test2@gmail.com", "123456");

                // Get user's expenses to find a valid expense ID to delete
                var userExpenses = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                                .header("Authorization", "Bearer " + mockLogin.getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

                String responseContent = userExpenses.getResponse().getContentAsString();
                UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse = objectMapper
                                .readValue(responseContent, new TypeReference<>() {
                                });

                assertFalse(userExpensesResponse.getUserExpenses().isEmpty());
                Long expenseId = userExpensesResponse.getUserExpenses().getFirst().getId();

                Optional<Expense> expense = expenseRepository.findById(expenseId);
                assertTrue(expense.isPresent());

                mockMvc.perform(MockMvcRequestBuilders.delete("/api/expenses/{expenseId}", expenseId)
                                .header("Authorization", "Bearer " + mockLogin.getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(MockMvcResultMatchers.status().isNoContent());

                Optional<Category> category = categoryRepository.findByCategoryName("Groceries");
                expense = expenseRepository.findById(expenseId);

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

                UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse = objectMapper
                                .readValue(responseContent, new TypeReference<>() {
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

                UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse = objectMapper
                                .readValue(responseContent, new TypeReference<>() {
                                });

                List<ExpenseResponseDto> expenseResponseDtoList = userExpensesResponse.getUserExpenses();

                assertEquals(3, expenseResponseDtoList.size());
                assertEquals("Food items", expenseResponseDtoList.getFirst().getDescription());
                assertEquals("Self help", expenseResponseDtoList.getLast().getCategory());

                assertEquals(7300.0, userExpensesResponse.getTotalMonthlyExpense());

        }

        @Order(14)
        @Test
        void testUpdateUserExpenses_AggregateAmount() throws Exception {

                LoginResponseDto loginUser = loginUser("test@gmail.com", "12345");

                // Get expenses for July 2024 to find a valid expense ID
                var julyExpenses = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                                .header("Authorization", "Bearer " + loginUser.getAuthToken())
                                .param("yearMonth", "2024-07")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

                String julyContent = julyExpenses.getResponse().getContentAsString();
                UserExpensesResponse<List<ExpenseResponseDto>> julyResponse = objectMapper.readValue(julyContent,
                                new TypeReference<>() {
                                });

                assertFalse(julyResponse.getUserExpenses().isEmpty());
                Long expenseId = julyResponse.getUserExpenses().getFirst().getId();

                ExpenseRequestDto mockUpdate = new ExpenseRequestDto();
                mockUpdate.setDate(LocalDate.of(2024, 7, 15));
                mockUpdate.setAmount(800.0);

                var updateResponse = mockMvc.perform(MockMvcRequestBuilders.put("/api/expenses/{expenseId}", expenseId)
                                .header("Authorization", "Bearer " + loginUser.getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mockUpdate)))
                                .andExpect(MockMvcResultMatchers.status().isAccepted()).andReturn();

                ExpenseResponseDto expenseResponseDto = objectMapper
                                .readValue(updateResponse.getResponse().getContentAsString(), ExpenseResponseDto.class);

                assertEquals(800.0, expenseResponseDto.getAmount());
                assertEquals(LocalDate.of(2024, 7, 15), expenseResponseDto.getDate());

                LocalDate previousMonthDate = LocalDate.of(2024, 7, 15);

                var mockGetCurrentMonth = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                                .header("Authorization", "Bearer " + loginUser.getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

                String responseContent1 = mockGetCurrentMonth.getResponse().getContentAsString();
                log.info(responseContent1);

                UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse1 = objectMapper
                                .readValue(responseContent1, new TypeReference<>() {
                                });

                // Store initial aggregate amount before update
                double initialJuly2024Total = julyResponse.getTotalMonthlyExpense();
                double expenseAmountBeforeUpdate = julyResponse.getUserExpenses().getFirst().getAmount();

                var mockGetPreviousMonth = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                                .header("Authorization", "Bearer " + loginUser.getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("yearMonth", "2024-07"))
                                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

                String responseContent2 = mockGetPreviousMonth.getResponse().getContentAsString();

                UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse2 = objectMapper
                                .readValue(responseContent2, new TypeReference<>() {
                                });

                // Verify that the current month expense total doesn't contain the moved expense
                assertNotNull(userExpensesResponse1.getTotalMonthlyExpense());
                // Verify that the July 2024 total changed by the expected amount (initial - old
                // + new = initial - old + 800)
                double expectedJulyTotal = initialJuly2024Total - expenseAmountBeforeUpdate + 800.0;
                assertEquals(expectedJulyTotal, userExpensesResponse2.getTotalMonthlyExpense(), 0.01);
                // Verify the July 2024 expenses still contain data
                assertFalse(userExpensesResponse2.getUserExpenses().isEmpty());
        }

        @Test
        @Order(15)
        void testUpdateUserExpenses_AggregateAmount_Not_ExistingMonth() throws Exception {

                LoginResponseDto loginUser = loginUser("test@gmail.com", "12345");

                // Get expenses for July 2024 to find a valid expense ID
                var julyExpenses = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                                .header("Authorization", "Bearer " + loginUser.getAuthToken())
                                .param("yearMonth", "2024-07")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

                String julyContent = julyExpenses.getResponse().getContentAsString();
                UserExpensesResponse<List<ExpenseResponseDto>> julyResponse = objectMapper.readValue(julyContent,
                                new TypeReference<>() {
                                });

                assertFalse(julyResponse.getUserExpenses().isEmpty());
                // Use the last expense (Dress) for this test
                Long expenseId = julyResponse.getUserExpenses().getLast().getId();

                ExpenseRequestDto mockUpdate = new ExpenseRequestDto();
                mockUpdate.setDate(LocalDate.now().plusMonths(2));
                mockUpdate.setAmount(6000.0);

                var updateResponse = mockMvc.perform(MockMvcRequestBuilders.put("/api/expenses/{expenseId}", expenseId)
                                .header("Authorization", "Bearer " + loginUser.getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mockUpdate)))
                                .andExpect(MockMvcResultMatchers.status().isAccepted()).andReturn();

                ExpenseResponseDto expenseResponseDto = objectMapper
                                .readValue(updateResponse.getResponse().getContentAsString(), ExpenseResponseDto.class);

                assertEquals(6000.0, expenseResponseDto.getAmount());
                assertEquals(LocalDate.now().plusMonths(2), expenseResponseDto.getDate());

                var mockGetCurrentMonth = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                                .header("Authorization", "Bearer " + loginUser.getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("yearMonth", "2024-07"))
                                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

                String futureYearMonth = DateUtil.getYearMonth(LocalDate.now().plusMonths(2)).toString();

                var mockGetFutureMonth = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                                .header("Authorization", "Bearer " + loginUser.getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("yearMonth", futureYearMonth))
                                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

                // Store the initial July 2024 total before the expense was moved
                double initialJulyTotal = julyResponse.getTotalMonthlyExpense();
                double movedExpenseOriginalAmount = julyResponse.getUserExpenses().getLast().getAmount();

                String responseContent1 = mockGetCurrentMonth.getResponse().getContentAsString();

                UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse1 = objectMapper
                                .readValue(responseContent1, new TypeReference<>() {
                                });

                String responseContent2 = mockGetFutureMonth.getResponse().getContentAsString();
                log.info(responseContent2);
                UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse2 = objectMapper
                                .readValue(responseContent2, new TypeReference<>() {
                                });

                // Verify July 2024 no longer contains the moved expense
                double expectedJulyTotal = initialJulyTotal - movedExpenseOriginalAmount;
                assertEquals(expectedJulyTotal, userExpensesResponse1.getTotalMonthlyExpense(), 0.01);
                // Verify the expense count decreased by one in July 2024
                assertEquals(julyResponse.getUserExpenses().size() - 1, userExpensesResponse1.getUserExpenses().size());

                // Verify the future month contains the moved expense with new amount
                assertEquals(6000.0, userExpensesResponse2.getTotalMonthlyExpense());
                assertEquals(1, userExpensesResponse2.getUserExpenses().size());

        }

        @Test
        @Order(16)
        void testDeleteExpense_Aggregate_Expense() throws Exception {

                UserRegistrationDto newUserDetails = new UserRegistrationDto(
                                "test3", "test3@test.com", "1234567", "0987654321");

                // register new user
                mockMvc.perform(MockMvcRequestBuilders.post(ApplicationConstants.REGISTER_USER_API_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newUserDetails)))
                                .andExpect(MockMvcResultMatchers.status().isCreated());

                // add new expenses for this user.
                var loginResponse = loginUser("test3@test.com", "1234567");

                List<ExpenseRequestDto> newExpenses = List.of(
                                new ExpenseRequestDto("LOL", 1000.0, LocalDate.of(2024, Month.SEPTEMBER, 20), "LOL2"),
                                new ExpenseRequestDto("NEW_LOL", 2000.0, LocalDate.of(2024, Month.SEPTEMBER, 18),
                                                "Shopping"));

                var createResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/expenses")
                                .header("Authorization", "Bearer " + loginResponse.getAuthToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newExpenses)))
                                .andExpect(MockMvcResultMatchers.status().isCreated())
                                .andExpect(MockMvcResultMatchers.jsonPath("$[0].description").value("LOL"))
                                .andExpect(MockMvcResultMatchers.jsonPath("$[1].description").value("NEW_LOL"))
                                .andReturn();

                // Extract the created expense IDs from response
                String createContent = createResponse.getResponse().getContentAsString();
                List<ExpenseResponseDto> createdExpenses = objectMapper.readValue(createContent, new TypeReference<>() {
                });
                Long firstExpenseId = createdExpenses.get(0).getId();

                // delete user expense with the first expense id
                mockMvc.perform(MockMvcRequestBuilders.delete("/api/expenses/{expenseId}", firstExpenseId)
                                .header("Authorization", "Bearer " + loginResponse.getAuthToken()))
                                .andExpect(MockMvcResultMatchers.status().isNoContent());

                // fetch all the expenses for the month and check the aggregate amount is
                // updated or not
                var getApiResponse = mockMvc.perform(MockMvcRequestBuilders.get("/api/expenses")
                                .header("Authorization", "Bearer " + loginResponse.getAuthToken())
                                .param("yearMonth", "2024-09"))
                                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

                String responseContent = getApiResponse.getResponse().getContentAsString();

                UserExpensesResponse<List<ExpenseResponseDto>> userExpensesResponse = objectMapper
                                .readValue(responseContent, new TypeReference<>() {
                                });

                assertEquals(2000, userExpensesResponse.getTotalMonthlyExpense());
        }

        @Test
        @Order(17)
        void testChangePassword() throws Exception {

                var loginResponse = loginUser("test1@gmail.com", "12345");

                mockMvc.perform(MockMvcRequestBuilders.post("/user/changePass")
                                .header("Authorization", "Bearer " + loginResponse.getAuthToken())
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("newPassword"))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                // try login in with old password
                LoginRequestDto loginRequestDto = new LoginRequestDto("test1@gmail.com", "12345");

                mockMvc.perform(MockMvcRequestBuilders.post("/pubic/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequestDto)))
                                .andExpect(MockMvcResultMatchers.status().isForbidden());

                // try login with new password
                loginUser("test1@gmail.com", "newPassword");

        }
}
