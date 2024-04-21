package com.mysite.sbb.question;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.comment.Comment;
import com.mysite.sbb.user.SiteUser;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Question {
	//strategy : 기본키 생성 전략 AUTO, IDENTITY, SEQUENCE, TABLE, UUID가 있음
	//IDENTITY : 기본키 생성을 하이버네이트가 아닌 DB에 위임. AUTO_INCREMENT
	//@GeneratedValue로 속성을 지정해서 번호를 차례대로 늘어나도록 할때.
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 200)
	private String subject;

	@Column(columnDefinition = "TEXT")
	private String content;

	private LocalDateTime createDate;

	//CascadeType : 영속성 전이. 부모 엔티티가 영속화될 때 자식 엔티티도 같이 영속화되고, 부모 엔티티가 삭제될 때 자식 엔티티도 삭제됨.
	@OneToMany(mappedBy = "question", cascade = CascadeType.REMOVE)
	private List<Answer> answerList;
	
	@OneToMany(mappedBy = "question", cascade = CascadeType.REMOVE)
	private List<Comment> commentList;
	
	@ManyToOne
	private SiteUser author;
	
	private LocalDateTime modifyDate;
	
	@ManyToMany
	Set<SiteUser> voter;
	
	private int category;

	public QuestionEnum getCategoryAsEnum() {
		switch (this.category) {
			case 0:
				return QuestionEnum.QNA;
			case 1:
				return QuestionEnum.FREE;
			default:
				throw new RuntimeException("올바르지 않은 접근입니다.");
		}
	}

	public String getCategoryAsString() {
		switch (this.category) {
			case 0:
				return "질문과답변";
			case 1:
				return "자유게시판";
			default:
				throw new RuntimeException("올바르지 않은 접근입니다.");
		}
	}
}