package com.homework.web.app.controller;


import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.model.enums.GroupType;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.service.UserCenterService;
import com.homework.web.app.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/app/user-center")
@RequiredArgsConstructor
public class UserCenterController {

    private final UserCenterService userCenterService;

    @GetMapping
    public Result<UserCenterPageVO> centerPageInfo() {
        Long userId = LoginUserHolder.getUserId();
        UserCenterPageVO pageVO = userCenterService.getCenterPageInfo(userId);
        return Result.success(pageVO);
    }


    @GetMapping("/wrong-question-banks")
    public Result<PageResult<WrongQuestionBankVO>> wrongQuestionBanks(@RequestParam GroupType groupType,
                                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                                      @RequestParam(defaultValue = "20") Integer pageSize) {

        return Result.success(userCenterService.getWrongQuestionBanks(LoginUserHolder.getUserId(), groupType, pageNum, pageSize));
    }

    @GetMapping("/wrong-question-list")
    public Result<PageResult<WrongQuestionVO>> wrongQuestionList(@RequestParam Long bankId,
                                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                                              @RequestParam(defaultValue = "20") Integer pageSize){
        return Result.success(userCenterService.getWrongQuestions(LoginUserHolder.getUserId(),bankId,pageNum, pageSize));
    }

    @GetMapping("/wrong-question")
    public Result<WrongQuestionReviewVO> wrongQuestion(@RequestParam Long bankId ,@RequestParam Long questionId){
        return Result.success(userCenterService.getWrongQuestion(LoginUserHolder.getUserId(),bankId,questionId));
    }

    @GetMapping("/favorite-question-banks")
    public Result<PageResult<FavoriteQuestionBankVO>> favoriteQuestionBanks(@RequestParam GroupType groupType,
                                                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(userCenterService.getFavoriteQuestionBanks(LoginUserHolder.getUserId(), groupType, pageNum, pageSize));
    }

    @GetMapping("/favorite-question-list")
    public Result<PageResult<FavoriteQuestionVO>> favoriteQuestionList(@RequestParam Long bankId,
                                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                                 @RequestParam(defaultValue = "20") Integer pageSize){
        return Result.success(userCenterService.getFavoriteQuestions(LoginUserHolder.getUserId(),bankId,pageNum, pageSize));
    }

    @GetMapping("/favorite-question")
    public Result<FavoriteQuestionReviewVO> favoriteQuestion(@RequestParam Long bankId ,@RequestParam Long questionId){
        return Result.success(userCenterService.getFavoriteQuestion(LoginUserHolder.getUserId(),bankId,questionId));
    }

    @GetMapping("/note-banks")
    public Result<PageResult<NoteBankVO>> noteBanks(@RequestParam GroupType groupType,
                                                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(userCenterService.getNoteBanks(LoginUserHolder.getUserId(), groupType, pageNum, pageSize));
    }

    @GetMapping("/note-list")
    public Result<PageResult<NoteQuestionVO>> noteQuestionList(@RequestParam Long bankId,
                                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                                       @RequestParam(defaultValue = "20") Integer pageSize){
        return Result.success(userCenterService.getNoteQuestions(LoginUserHolder.getUserId(),bankId,pageNum, pageSize));
    }

    @GetMapping("/note-question")
    public Result<NoteVO> note(@RequestParam Long bankId ,@RequestParam Long questionId){
        return Result.success(userCenterService.getNote(LoginUserHolder.getUserId(),bankId,questionId));
    }

    @GetMapping("/membership-info")
    public Result<MembershipInfoVO> membershipInfo(){
        Long userId = LoginUserHolder.getUserId();
        return Result.success(userCenterService.getMembershipInfo(userId));
    }
}
