package com.mysite.sbb.question;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;


import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.user.SiteUser;
import com.mysite.sbb.user.profileImage;

import jakarta.transaction.Transactional;

import com.mysite.sbb.comment.Comment;
import com.mysite.sbb.comment.CommentRepository;

import lombok.RequiredArgsConstructor;
import net.bytebuddy.asm.Advice.This;

/*
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
*/

@RequiredArgsConstructor
@Service
public class QuestionService {
	
	private final QuestionRepository questionRepository;
	private final questionImageRepository questionImageRepository;
	private final CommentRepository commentRepository; 
	
    @Value("${file.path}")
    private String uploadFolder;

/*
	private Specification<Question> search(String kw){
		return new Specification<>() {
			private static final long serialVersionUID = 1L;
			@Override
			public Predicate toPredicate(Root<Question> q, CriteriaQuery<?> query, CriteriaBuilder cb) {
				query.distinct(true);
				Join<Question, SiteUser> u1 = q.join("author",JoinType.LEFT);
				Join<Question, Answer> a = q.join("answerList",JoinType.LEFT);
				Join<Answer, SiteUser> u2 = a.join("author",JoinType.LEFT);
				
				return cb.or(cb.like(q.get("subject"),"%"+kw+"%"),
						cb.like(q.get("content"), "%"+kw+"%"),
						cb.like(u1.get("username"), "%"+kw+"%"),
						cb.like(u2.get("username"), "%"+kw+"%")
						);
			}
		};
	}
*/
	
/*public List<Question> getList(){
		return questionRepository.findAll();
	}
	*/
	public Page<Question> getList(int Page, int category, String kw){
		List<Sort.Order> sorts = new ArrayList<>();
		sorts.add(Sort.Order.desc("createDate"));
		Pageable pageable = PageRequest.of(Page, 10, Sort.by(sorts));
		//Specification<Question> spec = search(kw);
		return this.questionRepository.findAllByKeyword(kw, category, pageable);
	}
	//get할때마다 조회수+1
	public Question getQuestion(Integer id) {
		Optional<Question> question = this.questionRepository.findById(id);
		if (question.isPresent()) {
			Question questionView = question.get();
			questionView.setViews(question.get().getViews()+1);
			this.questionRepository.save(questionView);
			return questionView;
		} else {
			throw new DataNotFoundException("question is not found");
		}
	}
	
	public void create(String subject, String content, SiteUser user, int category, List<MultipartFile> getFile) {
		Question q = new Question();
		q.setSubject(subject);
		q.setContent(content);
		q.setAuthor(user);
		q.setCreateDate(LocalDateTime.now());
		q.setCategory(category);
		q.setCommentParentGrp(0);
		
		this.questionRepository.save(q);	
		
		//이미지 업로드
        if(getFile != null && !getFile.isEmpty()) {
        	for(MultipartFile file:getFile) {
        UUID uuid = UUID.randomUUID();
        String imageFileName = uuid + "_" + file.getOriginalFilename();
        File destinationFile = new File(uploadFolder + imageFileName);

        try {
            file.transferTo(destinationFile);
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        questionImage image = questionImageRepository.findByQuestion(q);      
        
        image = questionImage.builder()
                        .question(q)
                        .url("/questionImages/" + imageFileName)
                        .build();            
        

        questionImageRepository.save(image);
        
        	}
        

    }

	}
	
    public questionImageResponseDTO findImage(Question question) {

            return questionImageResponseDTO.builder().question(question).build();
	

    }

    @Transactional
	public void modify(Question question, String subject, String content, List<MultipartFile> files) {
		question.setSubject(subject);
		question.setContent(content);
		question.setModifyDate(LocalDateTime.now());
		
		//form으로 받아온 file이 존재하면 원래 image를 삭제한다.
			if(files != null && !files.isEmpty()) {
				this.questionImageRepository.deleteAllByQuestion(question);

        	for(MultipartFile file:files) {
        		
            UUID uuid = UUID.randomUUID();
            String imageFileName = uuid + "_" + file.getOriginalFilename();
            File destinationFile = new File(uploadFolder + imageFileName);
            
            try {
                file.transferTo(destinationFile);
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
            questionImage image = this.questionImageRepository.findByQuestion(question);
	        
	        image = questionImage.builder()
	                        .question(question)
	                        .url("/questionImages/" + imageFileName)
	                        .build();
            
           
            this.questionImageRepository.save(image);


	}
			}
			
			this.questionRepository.save(question);
	}
	
	public void delete(Question question) {
		this.questionRepository.delete(question);
	}
	
	//추천인을 voter에 저장하고 size를 부르면 개수가 나온다.
	public void vote(Question question, SiteUser siteUser) {
		question.getVoter().add(siteUser);
		this.questionRepository.save(question);
	}
	
	public List<Question> findQuestionList(int num, String username) {

		Pageable pageable = PageRequest.of(0, num);
		return this.questionRepository.findQuestionList(username, pageable);
	}
	
	public List<Comment> findByQuestionOrderByGrpAscSeqAsc(Question question){
		return this.commentRepository.findByQuestionOrderByGrpAscSeqAsc(question);
	}

}
