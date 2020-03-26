package com.hesiyue.miaosha.controller;


import com.hesiyue.miaosha.access.AccessLimit;
import com.hesiyue.miaosha.domain.MiaoshaOrder;
import com.hesiyue.miaosha.domain.MiaoshaUser;
import com.hesiyue.miaosha.domain.OrderInfo;
import com.hesiyue.miaosha.rabbitmq.MQSender;
import com.hesiyue.miaosha.rabbitmq.MiaoshaMessage;
import com.hesiyue.miaosha.redis.GoodsKey;
import com.hesiyue.miaosha.redis.RedisService;
import com.hesiyue.miaosha.result.CodeMsg;
import com.hesiyue.miaosha.result.Result;
import com.hesiyue.miaosha.service.GoodsService;
import com.hesiyue.miaosha.service.MiaoshaService;
import com.hesiyue.miaosha.service.MiaoshaUserService;
import com.hesiyue.miaosha.service.OrderService;
import com.hesiyue.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

	@Autowired
	MiaoshaUserService userService;
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	GoodsService goodsService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	MiaoshaService miaoshaService;

	@Autowired
	MQSender mqSender;

	private Map<Long,Boolean> localOverMap = new HashMap<>();
	
	/**
	 * QPS:1306
	 * 5000 * 10
	 * */
    @RequestMapping(value="/{path}/do_miaosha", method=RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
								   @RequestParam("goodsId")long goodsId,
								   @PathVariable("path")String path) {
    	model.addAttribute("user", user);
    	if(user == null) {
    		return Result.error(CodeMsg.SESSION_ERROR);
    	}

    	boolean check = miaoshaService.checkPath(user, goodsId, path);
    	if(!check){
    		return Result.error(CodeMsg.REQUEST_ILLEGAL);
		}
    	//内存标记，减少redis访问
    	boolean over = localOverMap.get(goodsId);
    	if(over){
    		return Result.error(CodeMsg.MIAO_SHA_OVER);
		}

    	//判断库存是否还足够
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock,""+goodsId);
    	if(stock < 0){
    		localOverMap.put(goodsId,true);
    		return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
    	//判断是否已经秒杀到了
		MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
    	if(order != null) {
    		return Result.error(CodeMsg.REPEATE_MIAOSHA);
    	}
		MiaoshaMessage mm = new MiaoshaMessage();
    	mm.setUser(user);
    	mm.setGoodsId(goodsId);
    	mqSender.sendMiaoshaMessage(mm);
    	return Result.success(0);

    	//判断库存
//    	GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);//10个商品，req1 req2
//    	int stock = goods.getStockCount();
//    	if(stock <= 0) {
//    		return Result.error(CodeMsg.MIAO_SHA_OVER);
//    	}
//    	//判断是否已经秒杀到了
//    	MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
//    	if(order != null) {
//    		return Result.error(CodeMsg.REPEATE_MIAOSHA);
//    	}
//    	//减库存 下订单 写入秒杀订单
//    	OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
//        return Result.success(orderInfo);
    }

    /*
    *系统初始化的时候做一些事情
    * */
	@Override
	public void afterPropertiesSet() throws Exception {
		List<GoodsVo> goodList = goodsService.listGoodsVo();
		if(goodList == null){
			return;
		}
		for(GoodsVo goods:goodList){
			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), goods.getStockCount());
			localOverMap.put(goods.getId(), false);
		}
	}


	/*
	*
	* 1：秒杀成功
	* 0：排队中
	* */

	@RequestMapping(value = "/result",method = RequestMethod.GET)
	@ResponseBody
	public Result<Long> miaoshaResult(Model model,MiaoshaUser user,@RequestParam("goodsId")long goodsId){
		model.addAttribute("user", user);
		if(user == null){
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		long result = miaoshaService.getMiasoshaResult(user.getId(),goodsId);
		return Result.success(result);
	}


	@AccessLimit(seconds=5, maxCount=5, needLogin=true)
	@RequestMapping(value="/path", method=RequestMethod.GET)
	@ResponseBody
	public Result<String> getMiaoshaPath(HttpServletRequest request, MiaoshaUser user,
										 @RequestParam("goodsId")long goodsId,
										 @RequestParam(value="verifyCode", defaultValue="0")int verifyCode
	) {
		if(user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		boolean check = miaoshaService.checkVerifyCode(user, goodsId, verifyCode);
		if(!check) {
			return Result.error(CodeMsg.REQUEST_ILLEGAL);
		}
		String path  =miaoshaService.createMiaoshaPath(user, goodsId);
		return Result.success(path);
	}


	@RequestMapping(value="/verifyCode", method=RequestMethod.GET)
	@ResponseBody
	public Result<String> getMiaoshaVerifyCod(HttpServletResponse response, MiaoshaUser user,
											  @RequestParam("goodsId")long goodsId) {
		if(user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		try {
			BufferedImage image  = miaoshaService.createVerifyCode(user, goodsId);
			OutputStream out = response.getOutputStream();
			ImageIO.write(image, "JPEG", out);
			out.flush();
			out.close();
			return null;
		}catch(Exception e) {
			e.printStackTrace();
			return Result.error(CodeMsg.MIAOSHA_FAIL);
		}
	}
}
