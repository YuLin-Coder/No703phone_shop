package com.phonemarket.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.phonemarket.entity.Evaluate;
import com.phonemarket.entity.Goods;
import com.phonemarket.entity.Guess;
import com.phonemarket.entity.Users;
import com.phonemarket.service.IEvaluateService;
import com.phonemarket.service.IGoodsService;
import com.phonemarket.service.IGuessService;
import com.phonemarket.service.impl.CartServiceImpl;
import com.phonemarket.util.UUIDUtil;

@Controller
@RequestMapping("/goods")
public class GoodsController {
	@Autowired
	private IGoodsService goodsService;
	@Autowired
	private IGuessService guessService;
	@Autowired
	private IEvaluateService evaluateService;
	
	@RequestMapping("findAll")
	public String findAllGoods(Model model){
		List<Goods> list = goodsService.findAll();
		model.addAttribute("goodsList", list);
		return "list";
	}
	
	@RequestMapping("{goodsId}")
	public String findAllGoods(@PathVariable("goodsId")Integer goodsId,Model model){
		Goods goods = goodsService.findById(goodsId);
		List<Evaluate> evaList = evaluateService.findEvaluateByGoodsId(goodsId);
		model.addAttribute("goods", goods);
		model.addAttribute("evaList", evaList);
		return "detail";
	}
	
	@RequestMapping("preUpdate")
	public String preUpdate(Integer goodsId,Model model){
		Goods goods = goodsService.findById(goodsId);
		model.addAttribute("goods", goods);
		return "update";
	}
	
	@RequestMapping("findBySplitPage")
	@ResponseBody
	public JSONObject findBySplitPage(Integer page,Integer limit,String keyword){
		PageInfo<Goods> info = goodsService.findBySplitPage(page, limit,keyword);
		JSONObject obj=new JSONObject();
		obj.put("msg", "");
		obj.put("code", 0);
		obj.put("count", info.getTotal());
		obj.put("data", info.getList());
		return obj;
	}
	
	@RequestMapping("findGoodsByType")
	@ResponseBody
	public List<Goods> findGoodsByType(Integer typeId){
		 List<Goods> list = goodsService.findGoodsByType(typeId);
		 return list;
	}
	@RequestMapping("detail")
	public String findGoodsDetail(Integer goodsId,Model model,HttpServletRequest request){
		Goods goods = goodsService.findById(goodsId);
		model.addAttribute("goods",goods);
		model.addAttribute("evaList", goods.getEvaList());
		HttpSession session = request.getSession();
		Users user=(Users) session.getAttribute("user");
		if(user!=null){
			Guess guess = guessService.findGuessByUserId(user.getUserId(), goods.getGoodsId());
			if(guess!=null){
				guess.setGuessNum(guess.getGuessNum()+1);
				guessService.updateGuess(guess);
			}else{
				Guess g=new Guess(goods, 1, -1, user);
				guessService.addGuess(g);
			}
		}
		return "userview/product_detail";
	}
	@RequestMapping("findHotGoods")
	@ResponseBody
	public List<Goods> findHotGoods(){
		return goodsService.findHotGoods(4);
	}
	@RequestMapping("search")
	public ModelAndView searchGoodsByName(String keyWord){
		ModelAndView mv=new ModelAndView();
		List<Goods> list = goodsService.findGoodsLikeName(keyWord);
		mv.addObject("searchList",list);
		mv.setViewName("userview/search");
		return mv;
	}
	@RequestMapping("searchPre")
	@ResponseBody
	public List<Goods> searchPreGoods(String keyword){
		List<Goods> list = goodsService.findGoodsLikeName(keyword);
		return list;
	}
	@RequestMapping("delete")
	@ResponseBody
	public String deleteGoods(Integer goodsId){
		Integer rs = goodsService.deleteGoods(goodsId);
		if(rs>0){
			return "success";
		}else{
			return "fail";
		}
	}
	@RequestMapping("updateGoods")
	@ResponseBody
	public String updateGoods(Goods goods){
		Integer rs = goodsService.update(goods);
		if(rs>0){
			return "success";
		}else{
			return "fail";
		}
	}
	@RequestMapping("batchDelete")
	@ResponseBody
	public String batchDelete(String batchId){
		Integer rs=0;
		String[] id = batchId.split(",");
		for (String s : id) {
			Integer goodsId = Integer.valueOf(s);
			rs = goodsService.deleteGoods(goodsId);
		}
		if(rs>0){
			return "success";
		}else{
			return "fail";
		}
	}
	@RequestMapping(value="uploadImg",method={RequestMethod.POST})
	@ResponseBody
	public Object uploadGoodsImg(@PathVariable(value="file")MultipartFile file,HttpServletRequest request){
		String str = file.getOriginalFilename();
		String name=UUIDUtil.getUUID()+str.substring(str.lastIndexOf("."));
		String path=request.getSession().getServletContext().getRealPath("/upload")+"/"+name;
		try {
			file.transferTo(new File(path));
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
		}
		JSONObject obj=new JSONObject();
		obj.put("src", name);
		return obj;
	}
	@RequestMapping("addGoods")
	@ResponseBody
	public String addGoods(Goods goods){
		Integer rs = goodsService.addGoods(goods);
		if(rs>0){
			return "success";
		}else{
			return "fail";
		}
	}
	@RequestMapping("findGoodsByVolume")
	@ResponseBody
	public JSONObject findGoodsByVolume(){
		List<Goods> goodsList = goodsService.findGoodsByVolume(5);
		String[] name=new String[5];
		Integer[] volume=new Integer[5];
		for(int i=0;i<goodsList.size();i++){
			name[i]=goodsList.get(i).getGoodsName();
			volume[i]=goodsList.get(i).getGoodsVolume();
		}
		JSONObject obj=new JSONObject();
		obj.put("name", name);
		obj.put("volume", volume);
		return obj;
	}
}
