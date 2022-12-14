package com.robosoft.virtualLearn.chapter_module_test.dao;


import com.robosoft.virtualLearn.chapter_module_test.dto.ModuleTestRequest;
import com.robosoft.virtualLearn.chapter_module_test.dto.ResultAnswerRequest;
import com.robosoft.virtualLearn.chapter_module_test.dto.ResultHeaderRequest;
import com.robosoft.virtualLearn.chapter_module_test.model.Answers;
import com.robosoft.virtualLearn.chapter_module_test.model.ModuleTest;
import com.robosoft.virtualLearn.chapter_module_test.model.Questions;
import com.robosoft.virtualLearn.chapter_module_test.model.UserAnswers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModuleTestDataAccess {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public ModuleTest moduleTestQuestions(ModuleTestRequest request) {
        List<Questions> questions;
        ModuleTest moduleTest;
        String query = "select questionId,questionName,option_1,option_2,option_3,option_4 from question where testId=?";
        try {
            questions = jdbcTemplate.query(query, new BeanPropertyRowMapper<>(Questions.class), request.getTestId());
            moduleTest = jdbcTemplate.queryForObject("select testId,testName,testDuration,questionsCount from test where testId=" + request.getTestId(), new BeanPropertyRowMapper<>(ModuleTest.class));
        } catch (Exception e) {
            return null;
        }
        moduleTest.setQuestions(questions);
        return moduleTest;
    }

    public float userAnswers(UserAnswers userAnswers) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        jdbcTemplate.update("update chapterProgress set testCompletedStatus=true where testId=" + userAnswers.getTestId());
        float chapterTestPercentage =  updateUserAnswerTable(userAnswers);
        jdbcTemplate.update("update chapterProgress set chapterTestPercentage=" + chapterTestPercentage+ "where testId=" + userAnswers.getTestId());
        String coursePhoto = jdbcTemplate.queryForObject("select coursePhoto from course where courseId=(select courseId from chapterProgress where testId=" + userAnswers.getTestId() + ")", String.class);
        String description = "Completed Chapter " + jdbcTemplate.queryForObject("select chapterNumber from chapter where chapterId=(select chapterId from chapterProgress where testId=" + userAnswers.getTestId() + ")", String.class) + " - Setting up a new project, of course - " +jdbcTemplate.queryForObject("select courseName from course where courseId=(select courseId from chapter where chapterId=(select chapterId from chapterProgress where testId=" + userAnswers.getTestId() + "))", String.class);
        String description1 = "You Scored " + jdbcTemplate.queryForObject("select chapterTestPercentage from chapterProgress where chapterId=(select chapterId from chapterProgress where testId=" + userAnswers.getTestId() + ")", String.class) + "% in Chapter" + jdbcTemplate.queryForObject("select chapterNumber from chapter where chapterId=(select chapterId from chapterProgress where testId=" + userAnswers.getTestId() + ")", String.class) + " - Setting up a new project, of course - " +jdbcTemplate.queryForObject("select courseName from course where courseId=(select courseId from chapter where chapterId=(select chapterId from chapterProgress where testId=" + userAnswers.getTestId() + "))", String.class);
        jdbcTemplate.update("insert into notification(userName,description,notificationUrl) values(?,?,?)",userName,description,coursePhoto);
        jdbcTemplate.update("insert into notification(userName,description,notificationUrl) values(?,?,?)",userName,description1,coursePhoto);
        return chapterTestPercentage;
    }

    public float updateUserAnswerTable(UserAnswers userAnswers) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        String query = "select chapterId from test where testId=" + userAnswers.getTestId();
        int chapterId = jdbcTemplate.queryForObject(query, Integer.class);
        query = "select courseId from chapter where chapterId=" + chapterId;
        int courseId = jdbcTemplate.queryForObject(query, Integer.class);
        for (Answers uAnswers : userAnswers.getUserAnswers()) {
            query = "insert into userAnswer values('" + userName + "'" + "," + courseId + "," + chapterId + "," + userAnswers.getTestId() + "," + uAnswers.getQuestionId() + "," + "'" + uAnswers.getCorrectAnswer() + "'" + "," + "(select if((select correctAnswer from question where questionId=" + uAnswers.getQuestionId() + ") ='" + uAnswers.getCorrectAnswer() + "'" + ",true,false)))";
            jdbcTemplate.update(query);
        }
        int correctAnswerCount = jdbcTemplate.queryForObject("select count(*) from userAnswer where userAnswerStatus=true and testId=" + userAnswers.getTestId(), Integer.class);
        int questionCount = jdbcTemplate.queryForObject("select questionsCount from test where testId=" + userAnswers.getTestId(), Integer.class);
        float chapterTestPercentage = (correctAnswerCount / (float) questionCount) * 100;
        return chapterTestPercentage;

    }

    public ResultHeaderRequest getResultHeader(ModuleTestRequest testRequest) {
        float chapterTestPercentage = jdbcTemplate.queryForObject("select chapterTestPercentage from chapterProgress where testId=" + testRequest.getTestId(), Float.class);
        String chapterName = jdbcTemplate.queryForObject("select chapterName from chapter where chapterId=(select chapterId from test where testId=" + testRequest.getTestId() + ")", String.class);
        String courseName = jdbcTemplate.queryForObject("select courseName from course where courseId=(select courseId from chapter where chapterId=(select chapterId from test where testId=" + testRequest.getTestId() + "))", String.class);
        int totalNumberOfQuestions = jdbcTemplate.queryForObject("select questionsCount from test where testId=" + testRequest.getTestId(), Integer.class);
        int correctAnswers = jdbcTemplate.queryForObject("select count(*) from userAnswer where userAnswerStatus=true and testId=" + testRequest.getTestId(), Integer.class);
        int wrongAnswers = totalNumberOfQuestions - correctAnswers;
        return new ResultHeaderRequest(chapterName,chapterTestPercentage,courseName,correctAnswers,wrongAnswers,totalNumberOfQuestions);
    }

    public List<ResultAnswerRequest> getResultAnswers(ModuleTest request) {
        return jdbcTemplate.query("select question.questionId,questionName,option_1,option_2,option_3,option_4,correctAnswer,userAnswer,userAnswerStatus from question inner join userAnswer on question.questionId=userAnswer.questionId where userAnswer.testId=" +request.getTestId(),new BeanPropertyRowMapper<>(ResultAnswerRequest.class));

    }



//    public int getCorrectAnswersCount(UserAnswers userAnswers){
//        int correctAnswerCount = 0;
//        List<Answers> correctAnswers = jdbcTemplate.query("select questionId,correctAnswer from question where testId=" + userAnswers.getTestId(),new BeanPropertyRowMapper<>(Answers.class));
//        List<Answers> usrAnswer = userAnswers.getUserAnswers();
//        for (Answers cAnswers: correctAnswers) {
//            for (Answers uAnswers: usrAnswer) {
//                if(cAnswers.getQuestionId() == uAnswers.getQuestionId()) {
//                    if(cAnswers.getCorrectAnswer().equals(uAnswers.getCorrectAnswer()))
//                        correctAnswerCount++;
//                }
//            }
//        }
//        return correctAnswerCount;
//    }
}
