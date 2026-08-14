package com.homework.web.app.controller;


import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.model.enums.GroupType;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.EditProfileDTO;
import com.homework.web.app.dto.UserImageUpdateDTO;
import com.homework.web.app.service.UserCenterService;
import com.homework.model.enums.UserImageType;
import com.homework.web.app.service.impl.UserImageService;
import com.homework.web.app.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/app/user-center")
@RequiredArgsConstructor
public class UserCenterController {

    private final UserCenterService userCenterService;
    private final UserImageService imageService;

    @GetMapping
    public Result<UserCenterPageVO> centerPageInfo() {
        Long userId = LoginUserHolder.getUserId();
        UserCenterPageVO pageVO = userCenterService.getCenterPageInfo(userId);
        return Result.success(pageVO);
    }

    @Operation(summary = "上传头像或banner图片")
    @PostMapping("/images/{imageType}")
    public Result<UserImageVO> uploadImage(@PathVariable UserImageType imageType,@RequestParam("file") MultipartFile file) {
        UserImageVO userImageVO = imageService.upload(imageType,file,LoginUserHolder.getUserId());
        return Result.success(userImageVO);
    }


    @Operation(summary = "确认修改头像或banner图片")
    @PutMapping("/images/update")
    //注意：imageType可以选择通过 @PathVariable 或 @RequestParam，然后通过路径传参{imageType}，走你自己设计的转换器完成从数字到枚举常量的转换
    //也可以通过 @RequestBody，通过Jackson 的 HttpMessageConverter（反序列化），把Json 转换成 枚举常量，但也需要在你的枚举类中 自定义一个 @JsonCreator方法，遍历枚举常量，看是否匹配其中的数字字段
    public Result<Void> updateImage(@Valid @RequestBody UserImageUpdateDTO dto) {
        imageService.updateImage(dto.getUserImageType(),dto.getImageObjectKey(),LoginUserHolder.getUserId());
        return Result.success();
    }

    //用户点击“修改资料”，后端返回给前端数据
    @GetMapping("/profile-info")
    public Result<ProfileVO> getProfile(){
        return Result.success(userCenterService.getProfile(LoginUserHolder.getUserId()));
    }

    @GetMapping("/profile-info/options")
    public Result<ProfileOptionsVO> getProfileOptions(){
        return Result.success(userCenterService.getProfileOptions());
    }

    //用户填好需要修改的数据，点提交之后给到后端的数据
    @PutMapping("/edit-profile")
    public Result<EditedProfileVO> editProfile(@Valid @RequestBody EditProfileDTO dto) {
        return Result.success(userCenterService.editProfile(LoginUserHolder.getUserId(),dto));
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
