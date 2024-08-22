/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctu.demo.controller;

import ctu.demo.utils.JwtUtil;
import com.example.respone.AuthenticanResponse;
import com.example.respone.ResponseHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import ctu.demo.dto.AccountRequest;
import ctu.demo.exception.AppException;
import ctu.demo.exception.ErrorCode;
import ctu.demo.model.Account;
import ctu.demo.model.User;
import ctu.demo.repository.AccountRepository;
import ctu.demo.request.LoginRequest;
import ctu.demo.service.AccountService;
import ctu.demo.service.EmailService;
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
import org.springframework.http.HttpStatus;
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
    
@PostMapping("/verify")
public ResponseEntity<?> verifyCode(@RequestBody EmailRequest emailRequest) {
    String email = emailRequest.getEmail().trim();  // Trích xuất và xử lý email

    // Kiểm tra nếu tài khoản đã tồn tại
    if (accountService.getAccountByEmail(email) != null) {
        return ResponseHandler.resBuilder("NGƯỜI DÙNG ĐÃ TỒN TẠI!!!", HttpStatus.BAD_REQUEST, null);
    }

    // Tạo mã xác thực và gửi email
    String verificationCode = accountService.generateVerificationCode();
    emailService.sendVerificationEmail(email, verificationCode);

    return ResponseHandler.resBuilder("Bạn cần nhập mã xác thực", HttpStatus.OK, null);
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