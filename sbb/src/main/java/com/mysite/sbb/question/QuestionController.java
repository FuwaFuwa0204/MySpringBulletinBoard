package com.mysite.sbb.question;


import org.springframework.stereotype.Controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

import org.springframework.validation.BindingResult;

import com.mysite.sbb.answer.AnswerForm;
import com.mysite.sbb.user.SiteUser;
import com.mysite.sbb.user.UserService;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mysite.sbb.answer.AnswerService;
import com.mysite.sbb.answer.Answer;

@RequestMapping("/question")
@RequiredArgsConstructor
@Controller
public class QuestionController {
	
	private final QuestionService questionService;
	private final UserService userService;
	private final AnswerService answerService;
	
		@GetMapping("/")
		public String root() {
			return "redirect:/question/list/qna";
		}


	@GetMapping("/list/{type}")
	public String list(Model model, @RequestParam(value="page", defaultValue="0") int page, @RequestParam(value="kw", defaultValue="") String kw, @PathVariable String type) {
		//List<Question> questionList = this.questionService.getList();
		int category = switch(type) {
		case "qna" -> QuestionEnum.QNA.getStatus();
		case "free" -> QuestionEnum.FREE.getStatus();
		default -> throw new RuntimeException("올바르지 않은 접근입니다.");
		};
		Page<Question> paging = this.questionService.getList(page,category, kw);
		//name,value
		model.addAttribute("paging",paging);
		model.addAttribute("kw",kw);
		model.addAttribute("category",category);
		//이제 이 model 객체를 템플릿에서 활용한다.
		//파일명
		return "question_list";
	}	


	//댓글 페이징 부분
	@GetMapping(value= "/detail/{id}")
	public String detail(Model model, @PathVariable("id") Integer id, AnswerForm answerForm, @RequestParam(value="answerPage", defaultValue="0") int answerPage) {
		//id로 조회한 question을 넣어준다.
		Question question = this.questionService.getQuestion(id);
		Page<Answer> answerPaging = this.answerService.getList(question,answerPage);
		model.addAttribute("question",question);
		model.addAttribute("answerPaging",answerPaging);
		return "question_detail";
	}
	
	@PreAuthorize("isAuthenticated()")
	//버튼용
	@GetMapping("/create")
	public String questionCreate(Model model, QuestionForm questionForm) {

		return "question_form";
	}
	
	
	@PreAuthorize("isAuthenticated()")
	//질문등록용
	@PostMapping("/create")
	public String questionCreate(@Valid QuestionForm questionForm, BindingResult bindingResult, Principal principal) {
		
		SiteUser siteUser = this.userService.getUser(principal.getName());
		
		if(bindingResult.hasErrors()) {
			return"question_form";
		}
	    this.questionService.create(questionForm.getSubject(),questionForm.getContent(), siteUser);
		return "redirect:/question/list";
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/modify/{id}")
	public String questionModify(@Valid QuestionForm questionForm, BindingResult bindingResult, Principal principal, @PathVariable("id") Integer id) {
		
		if(bindingResult.hasErrors()) {
			return "question_form";
		}
		Question question = this.questionService.getQuestion(id);
		
		if(!question.getAuthor().getUsername().equals(principal.getName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정 권한이 없습니다.");
		}
		this.questionService.modify(question, questionForm.getSubject(), questionForm.getContent());
		return String.format("redirect:/question/detail/%s",id);
	}

	
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/modify/{id}")
	public String questionModify(QuestionForm questionForm, Principal principal, @PathVariable("id") Integer id) {

		Question question = this.questionService.getQuestion(id);
		
		if(!question.getAuthor().getUsername().equals(principal.getName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정 권한이 없습니다.");
		}
		questionForm.setSubject(question.getSubject());
		questionForm.setContent(question.getContent());
		return "question_form";
	}
	
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/delete/{id}")
	public String questionDelete(Principal principal, @PathVariable("id") Integer id) {
		Question question = this.questionService.getQuestion(id);
		if(!question.getAuthor().getUsername().equals(principal.getName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"삭제 권한이 없습니다.");
		}
		this.questionService.delete(question);
		return "redirect:/";
	}
	
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/vote/{id}")
	public String questionVote(Principal principal, @PathVariable("id") Integer id) {
		Question question = this.questionService.getQuestion(id);
		SiteUser siteUser = this.userService.getUser(principal.getName());
		this.questionService.vote(question, siteUser);
		return String.format("redirect:/question/detail/%s",id);
	}

}
