package com.kingguanzhang.toptalk.controller.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingguanzhang.toptalk.dto.Msg;
import com.kingguanzhang.toptalk.entity.*;
import com.kingguanzhang.toptalk.service.CommentServiceImpl;
import com.kingguanzhang.toptalk.service.PraiseServiceImpl;
import com.kingguanzhang.toptalk.service.StoryServiceImpl;
import com.kingguanzhang.toptalk.service.UserFavoriteServiceImpl;
import com.kingguanzhang.toptalk.utils.ImgUtil;
import com.kingguanzhang.toptalk.utils.RequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import com.kingguanzhang.toptalk.utils.VerifyAuthorityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Controller
public class StoryController {

    @Autowired
    private StoryServiceImpl storyService;
    @Autowired
    private CommentServiceImpl commentService;
    @Autowired
    private UserFavoriteServiceImpl userFavoriteService;
    @Autowired
    private PraiseServiceImpl praiseService;
    /**
     * 获取所有故事,分页并排序;
     * @param model
     * @param pn
     * @return
     */
    @RequestMapping("/story")
    public String toStoryPage(Model model, @RequestParam(value = "pn",defaultValue = "1")Integer pn){
        /**
         * 获取所有故事,分页并排序;
         */
        Pageable pageable = new PageRequest(pn-1,10,new Sort(Sort.Direction.DESC,"creatTime"));
        Story allStory = new Story();
        allStory.setStatus(1);//查出通过审核的状态为展示的故事;
        ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id","collectNumber","commentNumber");//long类型的需要忽略;
        Example<Story> example = Example.of(allStory,exampleMatcher);
        Page<Story> storyPage = storyService.findAllByExample(example,pageable);
        model.addAttribute("storyPage",storyPage);

        /**
         * 获取5个最热故事,即收藏数最多的故事;
         */
        Pageable pageable2 = new PageRequest(0,5,new Sort(Sort.Direction.DESC,"collectNumber"));
        Story hotStory = new Story();
        hotStory.setStatus(1);//查出通过审核的状态为展示的故事;
        ExampleMatcher exampleMatcher2 = ExampleMatcher.matching().withIgnorePaths("id","collectNumber","commentNumber");//long类型的需要忽略;
        Example<Story> example2 = Example.of(hotStory,exampleMatcher2);
        Page<Story> hotStoryPage = storyService.findAllByExample(example2,pageable2);
        model.addAttribute("hotStoryPage",hotStoryPage);

        return "portal/story";
    }


    /**
     * 获取故事详情;
     * @param model
     * @param storyId
     * @return
     */
    @RequestMapping("/story/{storyId}")
    public String toStoryDetailsPage(HttpServletRequest request,Model model, @RequestParam(value = "commentSort",defaultValue = "new")String commentSort,@PathVariable("storyId")String storyId,@RequestParam(value = "pn",defaultValue = "1")Integer pn){

        /**
         * 判断收藏状态,返回一个名为favStatus 的布尔值给页面;
         */

        Boolean favStrtus = false;
        if (null != request.getSession().getAttribute("user")){
            User user = (User) request.getSession().getAttribute("user");
            UserFavorite userFavorite = new UserFavorite();
            userFavorite.setUserId(user.getId());
            userFavorite.setStoryId(Long.parseLong(storyId));
            ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id").withIgnorePaths("essayId").withIgnorePaths("topicId");//所有Long 类型的属性默认值都是0不是null,所以要忽略;
            Example<UserFavorite> example = Example.of(userFavorite,exampleMatcher);
            Pageable pageable = new PageRequest(0,2);
            if (userFavoriteService.findAllByExample(example, pageable).hasContent()){//不使用findOne,因为当数据库有重复时findOne会抛异常;
                favStrtus = true;
            }
        }
        model.addAttribute("favStatus",favStrtus);

        Story story = storyService.findById(Long.parseLong(storyId));
        if (null == story){
            model.addAttribute("errorMsg","很抱歉,没有找到此稿件...");
            return "error/promptMessage";
        }else {
            /**
             * 限制浏览者只能浏览状态为1的稿件,除非浏览者是作者或管理员
             */
            if(1 == story.getStatus() || VerifyAuthorityUtil.isAdmin(request) || VerifyAuthorityUtil.isAuthorForThisStory(request,story)){
                model.addAttribute("story",story);

            }else {
                model.addAttribute("errorMsg","很抱歉,您暂时没有权限浏览此稿件...");
                return "error/promptMessage";
            }
        }


        /**
         * 获取story关联的父Comment,sql语句中已经排除了评论表中supcomment_id 不等于0的情况(即排除掉此评论为子评论时的情况);
         * 请求参数中的commentSort对应的值代表评论排序规则,new代表按最新排序,hot代表按最热排序(点赞数);
         */
        Pageable pageable5 ;
        if ("new" == commentSort) {
            pageable5 = new PageRequest(pn - 1, 10, new Sort(Sort.Direction.DESC, "creat_time"));
        }else {
            pageable5 = new PageRequest(pn - 1, 10, new Sort(Sort.Direction.DESC, "praise_number"));
        } Page<Comment> commentPage = commentService.findByStoryId(Long.parseLong(storyId), pageable5);
        //同时将排序状态返回,方便页面渲染翻页链接:
        model.addAttribute("commentSort",commentSort);
        model.addAttribute("commentPage",commentPage);

        /**
         * 判断当前取出的评论是否被用户点赞,返回一个记录当前页被收藏的评论Id的拼接字符串
         */
        String praiseCommentIds = "";
        if (null != request.getSession().getAttribute("user")){
            User user = (User) request.getSession().getAttribute("user");
            Praise praise = new Praise();
            praise.setUserId(user.getId());
            for(Comment temp:commentPage.getContent()){
                praise.setCommentId(temp.getId());
                ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id").withIgnorePaths("topicId").withIgnorePaths("storyId").withIgnorePaths("essayId").withIgnorePaths("eventId");
                Example<Praise> example = Example.of(praise,exampleMatcher);
                Pageable pageable1 = new PageRequest(0,2);
                if (praiseService.findAllByExample(example,pageable1).hasContent()){
                    praiseCommentIds = praiseCommentIds+temp.getId() + ",";
                }
            }
        }
        if ("" != praiseCommentIds){
            praiseCommentIds = praiseCommentIds.substring(0,praiseCommentIds.lastIndexOf(","));
        }
        model.addAttribute("praiseCommentIds",praiseCommentIds);
        return "portal/storyDetails";

    }

    /**
     * 保存ue富文本编辑器上传的图片并回显;
     * @param upfile
     * @return
     */
    @RequestMapping(value = "/storyContribute/imgUpload")
    @ResponseBody
    public String imgUpload3(MultipartFile upfile) {
        if (upfile.isEmpty()) {
            return "error";
        }

        // TODO 此处user id 需要改成从session中获取security 保存的用户信息来从数据库中查出id:
        User author = new User();
        author.setId(1);
        //设置中间文件夹,方便整理图片
        String centreAddr = "/story/"+author.getId()+"/";
        try {
            //使用工具类保存图片并返回文件名给网页;
            String fileName = ImgUtil.generateThumbnail(upfile,centreAddr,1920,1080);
            //url为文件访问的完整路径,注意应该配合mvc中配置的虚拟路径"/upload"
            String config = "{\"state\": \"SUCCESS\"," +
                    "\"url\": \"" + fileName + "\"," +
                    "\"title\": \"" + fileName + "\"," +
                    "\"original\": \"" + fileName + "\"}";
            return config;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return "error";
    }


}
