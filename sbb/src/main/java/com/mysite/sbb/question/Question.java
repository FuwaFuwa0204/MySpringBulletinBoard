package com.mysite.sbb.question;

import java.time.LocalDateTime;


import java.util.List;
import java.util.Set;

import com.mysite.sbb.comment.Comment;
import com.mysite.sbb.user.SiteUser;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;

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
    
	/* 답변 부분 삭제
	//CascadeType : 영속성 전이. 부모 엔티티가 영속화될 때 자식 엔티티도 같이 영속화되고, 부모 엔티티가 삭제될 때 자식 엔티티도 삭제됨.
	@OneToMany(mappedBy = "question", cascade = CascadeType.REMOVE)
	private List<Answer> answerList;
	*/
	
	@OneToMany(mappedBy = "question", cascade = CascadeType.REMOVE)
	private List<Comment> commentList;
	
	@ManyToOne
	private SiteUser author;
	
	private LocalDateTime modifyDate;
	
	@ManyToMany
	Set<SiteUser> voter;
	
	int category;
	
	//기본값 설정, null 불가능.
	@Column(columnDefinition = "integer default 0",nullable=false)
	//조회수 추가
	private int views;
	
	//question 지워지면 이미지도 지워짐
    @OneToMany(mappedBy = "question", fetch=FetchType.LAZY,cascade=CascadeType.REMOVE)
    private List<questionImage> questionImage;
    
    //question의 부모 댓글 번호. comment에 달면 차례대로 증가시키기 어려워서 그때그때 가져와서 쓸 수 있도록 question에 추가.
    private Integer commentParentGrp;
}

