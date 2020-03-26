package com.hesiyue.miaosha.controller;


import com.hesiyue.miaosha.domain.User;
import com.hesiyue.miaosha.rabbitmq.MQSender;
import com.hesiyue.miaosha.redis.RedisService;
import com.hesiyue.miaosha.redis.UserKey;
import com.hesiyue.miaosha.result.Result;
import com.hesiyue.miaosha.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RequestMapping("/demo")
public class MsController {

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender mqSender;

    @RequestMapping("/themeleaf")
    public String Themeleaf(Model model){
        model.addAttribute("name", "hesiyue");
        return "hello";
    }

    @RequestMapping("/db/get")
    @ResponseBody
    public Result<User> dbGet(){
        User byId = userService.getById(1);
        return Result.success(byId);
    }

    @RequestMapping("/db/tx")
    @ResponseBody
    public Boolean dbtx(){
        return userService.tx();
    }

    @RequestMapping("/redis/get")
    @ResponseBody
    public Result<User> redisGet(){
        User user = redisService.get(UserKey.getById,""+1,User.class);
        return Result.success(user);
    }


    @RequestMapping("/redis/set")
    @ResponseBody
    public Result<Boolean> redisSet(){
        User user = new User();
        user.setId(1);
        user.setName("1111");
        redisService.set(UserKey.getById,""+1, user);
        return Result.success(true);
    }


}
