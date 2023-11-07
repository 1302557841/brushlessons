package com.cbq.brushlessons.core.action.canghui;

import com.cbq.brushlessons.core.action.canghui.entity.coursedetail.Chapter;
import com.cbq.brushlessons.core.action.canghui.entity.coursedetail.CourseDetailData;
import com.cbq.brushlessons.core.action.canghui.entity.coursedetail.Process;
import com.cbq.brushlessons.core.action.canghui.entity.coursedetail.Section;
import com.cbq.brushlessons.core.action.canghui.entity.mycourselistresponse.*;
import com.cbq.brushlessons.core.action.canghui.entity.submitstudy.ConverterSubmitStudyTime;
import com.cbq.brushlessons.core.action.canghui.entity.submitstudy.SubmitStudyTimeRequest;
import com.cbq.brushlessons.core.entity.AccountCacheCangHui;
import com.cbq.brushlessons.core.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class CourseStudyAction implements Runnable {
    private User user;

    private MyCourse myCourse;

    //视屏
//    private VideoRouter videoRouter;
    private Map<Long, RouterDatum> map = new HashMap<>();


    private boolean newThread;

    private long studyInterval = 5; //单次提交学习时长

    private long accoVideo = 0;

    public void toStudy() {
        user.setModel(user.getModel() == null ? 0 : user.getModel());
        switch (user.getModel()) {
            //普通模式
            case 0 -> {
                if (newThread) {
                    new Thread(this).start();
                } else {
                    log.info("{}:正在学习课程>>>{}", user.getAccount(), myCourse.getCourse().getTitle());
                    study1();
                    log.info("{}:{}学习完毕！", user.getAccount(), myCourse.getCourse().getTitle());
                }
            }
            //暴力模式
            case 1 -> {
                log.info("{}:正在学习课程>>>{}", user.getAccount(), myCourse.getCourse().getTitle());
                study2();
            }
        }

    }

    /**
     * 普通一个一个刷版本
     */
    public void study1() {
        AccountCacheCangHui cache = (AccountCacheCangHui) user.getCache();
        Iterator<Long> iterator = map.keySet().iterator();

        while (iterator.hasNext()) {
            Long videoId = iterator.next();
            RouterDatum routerDatum = map.get(videoId);

            long studyTime = routerDatum.getProgress();//当前学习时间
            long videoDuration = routerDatum.getVideoDuration();//视屏总时长
            String title = routerDatum.getName();//视屏名称
            //循环开始学习
            while (studyTime < videoDuration) {
                //这里根据账号账号登录状态进行策划行为
                switch (cache.getStatus()) {//未登录则跳出
                    case 0 -> {
                        log.info("账号未登录，禁止刷课！");
                        return;
                    }
                    case 2 -> {//如果登录超时，则堵塞等待
                        studyTime -= studyInterval;
                        continue;
                    }
                }

                SubmitStudyTimeRequest submitStudyTimeRequest = CourseAction.submitLearnTime(user, myCourse, videoId, studyTime);
                try {
                    if (submitStudyTimeRequest != null) {
                        if (submitStudyTimeRequest.getMsg().contains("登录超时")) {
                            cache.setStatus(2);
                            studyTime -= studyInterval;
                            continue;
                        }
                        //成功提交

                        log.info("\n服务器端信息：>>>{}\n学习账号>>>{}\n学习平台>>>{}\n视屏名称>>>{}\n视屏总长度>>>{}\n当前学时>>>{}",
                                ConverterSubmitStudyTime.toJsonString(submitStudyTimeRequest),
                                user.getAccount(),
                                user.getAccountType().name(),
                                title,
                                videoDuration,
                                studyTime);


                    }

                    if (studyTime < videoDuration) {
                        Thread.sleep(1000 * studyInterval);
                    }
                } catch (JsonProcessingException e) {
                    log.error("");
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }


                //添加学时
                studyTime += studyInterval;

                //更新数据
                if (studyTime >= videoDuration) {
                    if (submitStudyTimeRequest == null)
                        studyTime -= studyInterval;
                    else
                        update();
                }
            }
        }
    }

    /**
     * 暴力模式
     */
    public void study2() {

        AccountCacheCangHui cache = (AccountCacheCangHui) user.getCache();
        Long arr[] = map.keySet().toArray(new Long[0]);

        for (int i = 0; i < arr.length; ++i) {
            Long videoId = arr[i];
            new Thread(() -> {
                RouterDatum routerDatum = map.get(videoId);
                long studyTime = routerDatum.getProgress() == 0 ? studyInterval : routerDatum.getProgress();//当前学习时间
                long videoDuration = routerDatum.getVideoDuration();//视屏总时长
                String title = routerDatum.getName();//视屏名称
                //循环开始学习
                while (studyTime < videoDuration) {
                    //这里根据账号账号登录状态进行策划行为
                    switch (cache.getStatus()) {//未登录则跳出
                        case 0 -> {
                            log.info("账号未登录，禁止刷课！");
                            return;
                        }
                        case 2 -> {//如果登录超时，则堵塞等待
                            studyTime -= studyInterval;
                            continue;
                        }
                    }

                    SubmitStudyTimeRequest submitStudyTimeRequest = CourseAction.submitLearnTime(user, myCourse, videoId, studyTime);
                    try {
                        if (submitStudyTimeRequest != null) {
                            if (submitStudyTimeRequest.getMsg().contains("登录超时")) {
                                cache.setStatus(2);
                                studyTime -= studyInterval;
                                continue;
                            }
                            //成功提交

                            log.info("\n服务器端信息：>>>{}\n学习账号>>>{}\n学习平台>>>{}\n视屏名称>>>{}\n视屏总长度>>>{}\n当前学时>>>{}",
                                    ConverterSubmitStudyTime.toJsonString(submitStudyTimeRequest),
                                    user.getAccount(),
                                    user.getAccountType().name(),
                                    title,
                                    videoDuration,
                                    studyTime);


                        }

                        if (studyTime < videoDuration) {
                            Thread.sleep(1000 * studyInterval);
                        }
                    } catch (JsonProcessingException e) {
                        log.error("");
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }


                    //添加学时
                    studyTime += studyInterval;

                    //更新数据
                    if (studyTime >= videoDuration) {
                        if (submitStudyTimeRequest == null)
                            studyTime -= studyInterval;
                        else
                            addAcco();
                        //判断是否刷课完成
                        if (getAcco() == arr.length)
                            log.info("{}:{}学习完毕！", user.getAccount(), myCourse.getCourse().getTitle());
                    }
                }
            }).start();
        }
    }


    private void update() {
//详细页面获取进度并赋值--------------------------
        CourseDetailData courseDetail = null;
        while ((courseDetail = CourseAction.getCourseDetail(user, myCourse.getCourse().getId())) == null) ;

        //章节
        if (courseDetail.getChapters() != null)
            for (Chapter chapter : courseDetail.getChapters()) {
                //视屏
                if (chapter == null)
                    continue;
                for (Section section : chapter.getSections()) {
                    Process process = section.getProcess();
                    if (process == null)
                        continue;
                    long id = process.getId();//视屏id
                    long progress = process.getProgress();//获取已经学习了的时长
                    RouterDatum orDefault = map.getOrDefault(id, null);
                    if (orDefault == null) continue;
                    orDefault.setProgress(progress > orDefault.getProgress() ? progress : orDefault.getProgress());//设置时长
                }
            }
    }


    public synchronized void addAcco() {
        ++accoVideo;
    }

    public synchronized long getAcco() {
        return this.accoVideo;
    }

    @Override
    public void run() {
        log.info("{}:正在学习课程>>>{}", user.getAccount(), myCourse.getCourse().getTitle());
        study1();
        log.info("{}:{}学习完毕！", user.getAccount(), myCourse.getCourse().getTitle());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CourseStudyAction courseStudyAction = new CourseStudyAction();

        public Builder user(User user) {
            courseStudyAction.user = user;
            return this;
        }

        public Builder courseInform(MyCourse myCourse) {
            courseStudyAction.myCourse = myCourse;
            return this;
        }

        public Builder newThread(Boolean newThread) {
            courseStudyAction.newThread = newThread;
            return this;
        }

        public CourseStudyAction build() {
            Course course = courseStudyAction.myCourse.getCourse();//获取课程
            long id1 = course.getId(); //课程id
            VideoRouter router = course.getRouter();//视屏列表

            //将需要学习的视屏加进去
            for (RouterDatum datum : router.getData()) {
                long id = datum.getId();//视屏id
                long videoDuration = datum.getVideoDuration();//获取视屏总时长
                courseStudyAction.map.put(id, datum);
            }

            //将进度缓存设置进去
            ProgressDetail progressDetail = courseStudyAction.myCourse.getProgressDetail();
            if (progressDetail.getData() != null)
                for (ProgressDetailDatum datum : progressDetail.getData()) {
                    RouterDatum routerDatum = courseStudyAction.map.getOrDefault(datum.getId(), null);
                    if (routerDatum == null)
                        continue;
                    courseStudyAction.map.get(datum.getId()).setProgress(datum.getProgress());//设置观看进度
                }


            //详细页面获取进度并赋值--------------------------
            CourseDetailData courseDetail = null;
            while ((courseDetail = CourseAction.getCourseDetail(courseStudyAction.user, id1)) == null) ;

            //章节
            if (courseDetail.getChapters() != null)
                for (Chapter chapter : courseDetail.getChapters()) {
                    //视屏
                    if (chapter == null)
                        continue;
                    for (Section section : chapter.getSections()) {
                        Process process = section.getProcess();
                        if (process == null)
                            continue;
                        long id = process.getId();//视屏id
                        long progress = process.getProgress();//获取已经学习了的时长
                        RouterDatum orDefault = courseStudyAction.map.getOrDefault(id, null);
                        if (orDefault == null) continue;
                        orDefault.setProgress(progress > orDefault.getProgress() ? progress : orDefault.getProgress());//设置时长
                    }
                }

            return courseStudyAction;
        }
    }
}
