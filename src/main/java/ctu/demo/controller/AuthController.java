/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctu.demo.controller;

import ctu.demo.utils.JwtUtil;
import ctu.demo.respone.AuthenticanResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import ctu.demo.dto.AccountRequest;
import ctu.demo.exception.AppException;
import ctu.demo.exception.ErrorCode;
import ctu.demo.model.Account;
import ctu.demo.model.User;
import ctu.demo.repository.AccountRepository;
import ctu.demo.request.LoginRequest;
import ctu.demo.request.MailRequest;
import ctu.demo.request.VerificationRequest;
import ctu.demo.respone.ResponseHandler;
import ctu.demo.service.AccountService;
import ctu.demo.service.EmailService;
import ctu.demo.service.OtpService;
import ctu.demo.service.RedisService;
import ctu.demo.service.UserDetailsServiceImpl;
import ctu.demo.service.UserService;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author ADMIN
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3001")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    
   @Autowired
    private JwtUtil jwtUtil;
   
   @Autowired
   private EmailService emailService;
    
   @Autowired
   private OtpService otpService;
   
   @Autowired
   private RedisService redisService;
   
    @Autowired
    private PasswordEncoder passwordEncoder;
    
     @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private UserService userService;
    @Autowired
    private AccountService accountService;

//    @PostMapping("/login")
//    public String login(@RequestBody Account account) {
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(
//                       account.getEmail(),
//                        account.getPassword()
//                )
//        );
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//        return jwtUtil.generateToken(account.getEmail());
//    }

     @PostMapping("/login")
    public ResponseEntity<Map<String, String>> createToken(@RequestBody AuthRequest authRequest) {
       try {
            // Thực hiện xác thực người dùng
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );

            // Tạo JWT Token nếu xác thực thành công
           final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmail());
           final String jwt = jwtUtil.generateToken(userDetails);

           Map<String, String> response = new HashMap<>();
           response.put("token", jwt);
          return ResponseEntity.ok(response);
       } catch (Exception e) {
           Map<String, String> errorResponse = new HashMap<>();
           errorResponse.put("error", "Invalid credentials");
           return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
   }
    
@PostMapping("/generate")
public ResponseEntity<?> generateVerificationCode(@RequestBody EmailRequest emailRequest) {
    String email = emailRequest.getEmail().trim();  // Trích xuất và xử lý email

    // Kiểm tra nếu tài khoản đã tồn tại
    if (accountService.getAccountByEmail(email) != null) {
        return ResponseHandler.resBuilder("NGƯỜI DÙNG ĐÃ TỒN TẠI!!!", HttpStatus.BAD_REQUEST, null);
    }
    
    String otp = otpService.generateOtp();
    
    RestTemplate restTemplate = new RestTemplate();
    String url = "http://localhost:8081/api/otp/generate";
    try {
        ResponseEntity<String> response = restTemplate.postForEntity(url, new MailRequest(email, otp), String.class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            return ResponseHandler.resBuilder("Lỗi khi tạo mã!!!", HttpStatus.BAD_REQUEST, null);
        }
    } catch (Exception e) {
        return ResponseHandler.resBuilder("Lỗi khi kết nối đến dịch vụ lưu mã OTP", HttpStatus.INTERNAL_SERVER_ERROR, null);
    }
    // Tạo mã xác thực và gửi email
    
//    emailService.sendVerificationEmail(email, otp);

    return ResponseHandler.resBuilder("Mã OTP đã được gửi đến email của bạn", HttpStatus.OK, null);
}
    
    
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody VerificationRequest verificationReq) {
          String email = verificationReq.getEmail();
          String otp = verificationReq.getCode();

     RestTemplate restTemplate = new RestTemplate();
    String url = "http://localhost:8081/api/otp/verify";
    
    // Tạo HttpHeaders và HttpEntity
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<VerificationRequest> request = new HttpEntity<>(verificationReq, headers);
    
    ResponseEntity<String> response;
      try {
        response = restTemplate.postForEntity(url, request, String.class);
        if (response.getStatusCode() == HttpStatus.OK && "Xác thực OTP thành công".equals(response.getBody())) {
            return ResponseHandler.resBuilder("Xác thực OTP thành công", HttpStatus.OK, null);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mã OTP không hợp lệ hoặc đã hết hạn");
        }
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Có lỗi xảy ra khi xác thực mã OTP.");
    }

          
}
    
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestPart("account") AccountRequest accountRequest,
                                      @RequestPart("image") MultipartFile image) {
        
        System.out.println(accountRequest.getEmail());
         try {
            Account savedAccount = accountService.registerNewAccount(accountRequest, image);
            return ResponseHandler.resBuilder("User registered successfully", HttpStatus.CREATED, savedAccount);
//        } catch (UserAlreadyExistsException e) {
//            return ResponseHandler.resBuilder(e.getMessage(), HttpStatus.BAD_REQUEST, null);
        } catch (ParseException e) {
            return ResponseHandler.resBuilder("Invalid birth date format", HttpStatus.BAD_REQUEST, null);
        } catch (IOException e) {
            return ResponseHandler.resBuilder("Error saving image", HttpStatus.BAD_REQUEST, null);
        } catch (Exception e) {
            return ResponseHandler.resBuilder("SOME ERROR WHEN REGISTERED", HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
         }  
//    
//    public AuthenticanResponse authenticate(LoginRequest req){
//        Account user =accountService.getAccountByEmail(req.getEmail());
//         if (user == null) {
//             throw new UsernameNotFoundException("USER NOT FOUND");
//        }
//         boolean authenticate = passwordEncoder.matches(req.getPassword(), user.getPassword());
//         if(!authenticate) throw new AppException(ErrorCode.UNAUTHENTICATED);
//    }
}