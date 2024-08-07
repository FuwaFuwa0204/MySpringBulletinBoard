package com.mysite.sbb.user;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.calendar.CalendarDTO;
import com.mysite.sbb.comment.Comment;
import com.mysite.sbb.comment.CommentService;
import com.mysite.sbb.question.Question;
import com.mysite.sbb.question.QuestionEnum;
import com.mysite.sbb.question.QuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
@Controller
@RequestMapping("/user")
public class UserController {
   
   private final UserService userService;
   private final QuestionService questionService;
   private final CommentService commentService;
   private final PasswordEncoder passwordEncoder;
   
   @GetMapping("/signup")
   public String signup(UserCreateForm userCreateForm) {
      return "signup_form";
   }
   
   @PostMapping("/signup")
   public String signup(@Valid UserCreateForm userCreateForm, BindingResult bindingResult) {
      if(bindingResult.hasErrors()) {
         return "signup_form";
      }
      if(!userCreateForm.getPassword1().equals(userCreateForm.getPassword2())) {
         //필드명,오류코드,오류메세지 rejectValue는 하나의 필드에 대한
         bindingResult.rejectValue("password2", "passwordInCoreect","2개의 비밀번호가 일치하지 않습니다.");
         return "signup_form";
      }
      try {
         userService.create(userCreateForm.getUsername(), userCreateForm.getEmail(), userCreateForm.getPassword1());
      }catch(DataIntegrityViolationException e) {
         e.printStackTrace();
         //오류코드,오류메세지 reject는 전체에 대한 복합적인 필드값 => form 검증에 의한 오류 외에 일반적인 오류를 발생시킬때 사용한다.
         bindingResult.reject("signupFailed","이미 등록된 사용자입니다.");
         return "signup_form";
      }catch(Exception e) {
         e.printStackTrace();
         bindingResult.reject("signupFailed",e.getMessage());
         return "signup_form";
      }
      
      
      return "redirect:/";
   }
   
   @GetMapping("/login")
   public String login() {
      return "login_form";
   }
   
   @GetMapping("/findid")
   public String findid() {
      return "find_id";
   }
   
   @PostMapping("/findid")
   @ResponseBody
   public String getusername(@RequestBody findusernameDTO findusernameDTO) {
      
      try {
      String username = this.userService.findByEmail(findusernameDTO.getEmail());         
      return username;      
      }catch(DataNotFoundException e) {
         return "해당 이메일의 유저를 찾을 수 없습니다.";
      }

   }
   
   @GetMapping("/findpassword")
   public String sendTmpPassword(tmpPasswordForm tmpPasswordForm) {
      return "findpasswordForm";
   }
   
   @PostMapping("/findpassword")
   public String sendTmpPassword(@Valid tmpPasswordForm tmpPasswordForm, BindingResult bindingResult){
      if(bindingResult.hasErrors()) {
         return "findpasswordForm";
      }
        try {
           userService.modifyPassword(tmpPasswordForm.getEmail());
        }catch(DataNotFoundException e) {
           e.printStackTrace();
           bindingResult.rejectValue("email","emailNotFound","해당 이메일을 찾을 수 없습니다.");
           return "findpasswordForm";
        } catch(EmailException e) {
            bindingResult.rejectValue("email","emailSendError","이메일 전송에 실패하였습니다.");
            return "findpasswordForm";
        }
        return "redirect:/";
   }
   
   
   @PreAuthorize("isAuthenticated()")   
   @GetMapping("/profile")
   public String profile(Model model, Principal principal) {
      String user = principal.getName(); //현재 로그인한 사람
      String userEmail = userService.getUser(user).getEmail();
      Long userid = userService.getUser(user).getId();
      //원래는 profileImageService
      profileImageResponseDTO image = this.userService.getProfileImage(user);
      profileImage checkimage= this.userService.findProfileImageByUser(user);
      
      model.addAttribute("username",user);
      model.addAttribute("userEmail",userEmail);
      model.addAttribute("userid",userid);
      model.addAttribute("userQuestion",questionService.findQuestionList(5, user));
      model.addAttribute("userComment",commentService.findCommentList(5, user));
      model.addAttribute("image",image);
      model.addAttribute("checkimage",checkimage);
      return "profile";
   }
   
   
   @GetMapping("/profile/myQuestion")
   //request param 값을 폼으로 받음
   public String profileMyQuestion(Model model,@RequestParam(value="page", defaultValue="0") int page, @RequestParam(value="kw", defaultValue="") String kw, Principal principal) {
      
      String user = principal.getName();
      Page<Question> paging = this.questionService.getListByUser(page, user, kw);
      model.addAttribute("pagingQuestion",paging);
      model.addAttribute("kw",kw);
        
      return "myQuestion";
      
   }
   
   
   @GetMapping("/profile/myComment")
   public String profileMyComment(Model model, @RequestParam(value="page", defaultValue="0") int page, @RequestParam(value="kw", defaultValue="") String kw, Principal principal) {
      String user = principal.getName();
      Page<Comment> paging = this.commentService.getListByUser(page, user, kw);
      model.addAttribute("pagingComment",paging);
      model.addAttribute("kw",kw);
        
      return "myComment";
      
   }
   
   //이 권한 가진 사람만
   @Secured("ROLE_ADMIN")   
   @GetMapping("/userManage")

   public String userManage(Model model, Principal principal, @RequestParam(value="page", defaultValue="0") int page, @RequestParam(value="kw", defaultValue="") String kw) {
      
      Page<SiteUser> userList = this.userService.getUserList(page,kw);
      
      System.out.println("userList 사이즈 : "+userList.getTotalElements());
      
      model.addAttribute("userList",userList);
      model.addAttribute("kw",kw);
      
      return "userManage";
   }
   
   @Secured("ROLE_ADMIN")   
   @GetMapping("/userManage/resign/{username}")
   @ResponseBody
   public ResponseEntity<?> userManageResign(@PathVariable String username) {
      
      this.userService.userManageResign(username);
      
      return ResponseEntity.ok("삭제성공");
   }
   
   @PreAuthorize("isAuthenticated()")   
   @GetMapping("/updatepassword")
   public String updatePassword(updatePasswordForm updatePasswordForm) {
      return "updatepassword";
   }
   
   @PreAuthorize("isAuthenticated()")   
   @PostMapping("/updatepassword")
   public String updatePassword(@Valid updatePasswordForm updatePasswordForm, BindingResult bindingResult, Principal principal) {
      if(bindingResult.hasErrors()) {
         return "updatepassword";
      }
      
      SiteUser user = this.userService.getUser(principal.getName());
      String password1 = updatePasswordForm.getPassword1();
      String password2 = updatePasswordForm.getPassword2();
      String email = updatePasswordForm.getEmail();
      
      
      if(!user.getUsername().equals(principal.getName())) {
         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경 권한이 없습니다.");
      }else if(!email.equals(user.getEmail())) {
         bindingResult.rejectValue("email","emailNotFound","해당 이메일의 유저가 존재하지 않습니다.");
         return "updatepassword";
      }else if(!passwordEncoder.matches(password1,user.getPassword())) {
            bindingResult.rejectValue("password1","passwordInCorrect","비밀번호가 맞지않습니다.");
            return "updatepassword";
      }else {
          try{
         this.userService.updatePassword(password1,password2,email);      
         return "redirect:/user/profile";
      }
      catch(DataNotFoundException e){
         bindingResult.rejectValue("email","emailNotFound","해당 이메일의 유저가 존재하지 않습니다.");
         return "updatepassword";
      }            
         }
      


      
   }
   
   @PreAuthorize("isAuthenticated()")   
   @GetMapping("/resign")
   public String resign(resignForm resignForm) {
      return "resign_form";
   }
   
   @PreAuthorize("isAuthenticated()")   
   @PostMapping("/resign")
   public String resign(@Valid resignForm resignForm, BindingResult bindingResult, Principal principal) {
      
      SiteUser user = this.userService.getUser(principal.getName());
      String email = user.getEmail();
      //입력으로 받아들이는 건 폼 클래스에서 가져오기
      String password = resignForm.getPassword();
      
      
      if(bindingResult.hasErrors()) {
         return "resign_form";
      }else{
      try {
         this.userService.resign(email,password);
         return "redirect:/user/logout";
      }catch(DataNotFoundException e) {
         bindingResult.rejectValue("password","passwordInCorrect","비밀번호가 일치하지 않습니다. 구글 소셜 로그인 사용자라면 구글 계정에서 연결을 해제하세요.");
         return "resign_form";
      }
      }
      
      
   }
   
   
   @PostMapping("/profileImage")
   public String imageUpload(@ModelAttribute profileImageUploadDTO profileImageUploadDTO, Principal principal) throws IOException{
      
	   /*
      String email = this.userService.getUser(principal.getName()).getEmail();
      
      profileImageService.upload(profileImageUploadDTO, email);
      return "redirect:/user/profile";
      */
	   
	   this.userService.upload(profileImageUploadDTO, principal.getName());
	   
	   return "redirect:/user/profile";
	   
   }
   
   @GetMapping("/profile/imagedelete")
   public String imageDelete(Principal principal) {
	   
	   /*
      
      SiteUser user = this.userService.getUser(principal.getName());
      
      this.profileImageService.profileImageDelete(user);
        
      return "redirect:/user/profile";
      */
	   
	   SiteUser user = this.userService.getUser(principal.getName());
	   
	   this.userService.deleteImage(user);

	  return "redirect:/user/profile";
   }
   

}
