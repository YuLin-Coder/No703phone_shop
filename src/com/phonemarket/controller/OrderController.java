package com.phonemarket.controller;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.pagehelper.PageInfo;
import com.phonemarket.entity.Address;
import com.phonemarket.entity.Cart;
import com.phonemarket.entity.Goods;
import com.phonemarket.entity.Guess;
import com.phonemarket.entity.Order;
import com.phonemarket.entity.OrderDetail;
import com.phonemarket.entity.Users;
import com.phonemarket.service.IAddressService;
import com.phonemarket.service.ICartService;
import com.phonemarket.service.IGoodsService;
import com.phonemarket.service.IGuessService;
import com.phonemarket.service.IOrderService;
import com.phonemarket.util.AlipayConfig;
import com.phonemarket.util.OrderSearchVO;
import com.phonemarket.util.OrderVO;

@Controller
@RequestMapping("/order")
public class OrderController {
	@Autowired
	private ICartService cartService;
	@Autowired
	private IGoodsService goodsService;
	@Autowired
	private IOrderService orderService;
	@Autowired
	private IAddressService addressService;
	@Autowired
	private IGuessService guessService;
	
	//下订单
	@RequestMapping("takeOrder")
	public String takeOrder(Integer[] goodslist,Integer addr,Model model,HttpServletRequest request){
		List<Cart> cartList=new ArrayList<Cart>();
		List<OrderDetail> detailList=new ArrayList<OrderDetail>();
		Double totalPrice=0D;
		for (Integer i : goodslist) {
			Cart cart = cartService.findCartById(i);
			totalPrice+=cart.getCartNum()*cart.getCartGoods().getGoodsPrice();
			cartList.add(cart);
			OrderDetail detail=new OrderDetail(cart.getCartGoods(), cart.getCartGoods().getGoodsPrice()*cart.getCartNum(), cart.getCartNum());
			detailList.add(detail);
		}
		HttpSession session = request.getSession();
		Users user=(Users) session.getAttribute("user");
		Date orderDate=new Date();
		Address address = addressService.findAddresById(addr);
		String a=address.getAddrProvince()+address.getAddrCity()+address.getAddrArea()+address.getAddrDetail();
		Order order=new Order(user, orderDate, totalPrice, 1, address.getAddrNickname(), address.getAddrPhone(), a);
		order.setDetailList(detailList);
		Order takeOrder = orderService.takeOrder(order);
		for (Cart c : cartList) {
			Goods goods = goodsService.findById(c.getCartGoods().getGoodsId());
			goods.setGoodsNum(goods.getGoodsNum()-c.getCartNum());
			goods.setGoodsVolume(goods.getGoodsVolume()+c.getCartNum());
			goodsService.update(goods);
			cartService.deleteCart(c.getCartId());
		}
		model.addAttribute("order",takeOrder);
		List<Guess> guessList = guessService.findGuessGoodsByUserId(user.getUserId(), 4);
		model.addAttribute("guessList", guessList);
		return "userview/takeorder";
	}
	
	@RequestMapping("findReadPayOrder")
	@ResponseBody
	public List<Order> findReadyPayOrder(HttpServletRequest request){
		HttpSession session = request.getSession();
		Users user=(Users) session.getAttribute("user");
		List<Order> orderList = orderService.findOrdersByUserIdAndState(user.getUserId(), 1);
		return orderList;
	}
	@RequestMapping("findReadyToDeliverOrder")
	@ResponseBody
	public List<Order> findReadyToDeliverOrder(HttpServletRequest request){
		HttpSession session = request.getSession();
		Users user = (Users) session.getAttribute("user");
		List<Order> orderList = orderService.findOrdersByUserIdAndState(user.getUserId(), 2);
		return orderList;
	}
	@RequestMapping("findReadyToReceiveOrder")
	@ResponseBody
	public List<Order> findReadToReceiveOrder(HttpServletRequest request){
		HttpSession session = request.getSession();
		Users user = (Users) session.getAttribute("user");
		List<Order> orderList = orderService.findOrdersByUserIdAndState(user.getUserId(), 3);
		return orderList;
	}
	@RequestMapping("findReadyToEvaluateOrder")
	@ResponseBody
	public List<Order> findReadyToEvaluateOrder(HttpServletRequest request){
		HttpSession session = request.getSession();
		Users user = (Users) session.getAttribute("user");
		List<Order> orderList = orderService.findOrdersByUserIdAndState(user.getUserId(), 4);
		return orderList;
	}
	@RequestMapping("findFinishOrder")
	@ResponseBody
	public List<Order> findFinishOrder(HttpServletRequest request){
		HttpSession session = request.getSession();
		Users user = (Users) session.getAttribute("user");
		List<Order> orderList = orderService.findOrdersByUserIdAndState(user.getUserId(), 5);
		return orderList;
		
	}
	
	@RequestMapping("receiveOrder")
	@ResponseBody
	public String receiveOrder(String orderId){
		Integer rs = orderService.receiveOrder(orderId);
		if(rs>0){
			return "success";
		}else{
			return "fail";
		}
	}
	@RequestMapping("deleteOrder")
	@ResponseBody
	public String deleteOrder(String orderId){
		Integer rs = orderService.deleteOrder(orderId);
		if(rs>0){
			return "success";
		}else{
			return "fail";
		}
	}
	@RequestMapping("findOrderBySplitPage")
	@ResponseBody
	public JSONObject findOrderBySplitPage(Integer page,Integer limit,OrderSearchVO vo){
		if(vo!=null){
			System.out.println(vo.getOrderState()+"========================");
		}
		PageInfo<Order> info = orderService.findOrdersBySplitPage(page, limit,vo);
		JSONObject obj=new JSONObject();
		obj.put("msg", "");
		obj.put("code", 0);
		obj.put("count", info.getTotal());
		obj.put("data", info.getList());
		return obj;
	}
	@RequestMapping("updateOrder")
	@ResponseBody
	public String updateOrder(Order order){
		Integer rs = orderService.updateOrder(order);
		if(rs>0){
			return "success";
		}else{
			return "fail";
		}
	}
	@RequestMapping("deliverOrder")
	@ResponseBody
	public String deliverOrder(String orderId,String expressNo){
		if(!expressNo.trim().equals("")){
			Integer rs = orderService.deliverOrder(orderId, expressNo);
			if(rs>0){
				return "success";
			}else{
				return "fail";
			}
		}else{
			return "fail";
		}
	}
	@RequestMapping("findOrderById")
	@ResponseBody
	public Order findOrderById(String orderId){
		Order order = orderService.findOrderById(orderId);
		return order;
	}
	@RequestMapping("findTotalOrder")
	@ResponseBody
	public JSONObject findOrderTotalMoney(){
		List<OrderVO> list = orderService.findTotalMoneyByMonth(6);
		String[] month=new String[6];
		Long[] total=new Long[6];
		Integer[] sheets=new Integer[6];
		for(int i=0;i<list.size();i++){
			month[i]=list.get(i).getOrderMonth();
			total[i]=list.get(i).getTotalMoney();
			sheets[i]=list.get(i).getSheets();
		}
		JSONObject obj=new JSONObject();
		obj.put("month", month);
		obj.put("total", total);
		obj.put("sheets", sheets);
		return obj;
	}
	@RequestMapping("findToDo")
	@ResponseBody
	public JSONObject findToDo(){
		Integer totalOrder = orderService.findTotalOrder();
		Integer totalDediver = orderService.findTotalDeliverOrder();
		JSONObject obj=new JSONObject();
		obj.put("total", totalOrder);
		obj.put("deliver", totalDediver);
		return obj;
	}
	
	@RequestMapping("toPay")
	public String toPayFor(String orderId,HttpServletResponse response){
		AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.gatewayUrl, AlipayConfig.app_id, AlipayConfig.merchant_private_key, "json", AlipayConfig.charset, AlipayConfig.alipay_public_key, AlipayConfig.sign_type);
		//设置请求参数
		AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
		alipayRequest.setReturnUrl(AlipayConfig.return_url);
		alipayRequest.setNotifyUrl(AlipayConfig.notify_url);
		Order order = orderService.findOrderById(orderId);
		//商户订单号，商户网站订单系统中唯一订单号，必填
		String out_trade_no=null;
		//付款金额，必填
		String total_amount=null;
		//订单名称，必填
		String subject=null;
		try {
			out_trade_no = new String(orderId.getBytes("ISO-8859-1"),"UTF-8");
			total_amount = new String((order.getOrderPrice()+"").getBytes("ISO-8859-1"),"UTF-8");
			subject = new String("手机商城");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		//商品描述，可空
		List<OrderDetail> detailList = order.getDetailList();
		String body = null;
		for (OrderDetail o : detailList) {
			body=body+o.getDetailGoods().getGoodsName()+",";
		}
		body=body.substring(0, body.length()-1)+"等商品";
		
		alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\"," 
				+ "\"total_amount\":\""+ total_amount +"\"," 
				+ "\"subject\":\""+ subject +"\"," 
				+ "\"body\":\""+ body +"\"," 
				+ "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
		//			String result = alipayClient.pageExecute(alipayRequest).getBody();
					Integer rs = orderService.payForOrder(out_trade_no);
					String result = "";
					if(rs>0){
						return "paysuccess";	
					}else {
						return null;
					}
					
	}
	@RequestMapping("notify_url")
	public void notifyUrl(HttpServletRequest request,HttpServletResponse response){
		Map<String,String> params = new HashMap<String,String>();
		Map<String,String[]> requestParams = request.getParameterMap();
		for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			String[] values = (String[]) requestParams.get(name);
			String valueStr = "";
			for (int i = 0; i < values.length; i++) {
				valueStr = (i == values.length - 1) ? valueStr + values[i]
						: valueStr + values[i] + ",";
			}
			//乱码解决，这段代码在出现乱码时使用
			try {
				valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			params.put(name, valueStr);
		}
		
		boolean signVerified=false;
		try {
			signVerified = AlipaySignature.rsaCheckV1(params, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
		} catch (AlipayApiException e1) {
			e1.printStackTrace();
		} //调用SDK验证签名

		//——请在这里编写您的程序（以下代码仅作参考）——
		
		/* 实际验证过程建议商户务必添加以下校验：
		1、需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
		2、判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
		3、校验通知中的seller_id（或者seller_email) 是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email）
		4、验证app_id是否为该商户本身。
		*/
		PrintWriter out=null;
		try {
			out = response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(signVerified) {//验证成功
			//商户订单号
			String out_trade_no="";
			//交易状态
			String trade_status="";
			try {
				out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"),"UTF-8");

				//支付宝交易号
				String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"),"UTF-8");

				trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"),"UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			if(trade_status.equals("TRADE_FINISHED")||trade_status.equals("TRADE_SUCCESS")){
				//判断该笔订单是否在商户网站中已经做过处理
				//如果没有做过处理，根据订单号（out_trade_no）在商户网站的订单系统中查到该笔订单的详细，并执行商户的业务程序
				//如果有做过处理，不执行商户的业务程序
				Integer rs = orderService.payForOrder(out_trade_no);
				if(rs>0){
					System.out.println("异步通知支付成功");
				}
				//注意：
				//付款完成后，支付宝系统发送该交易状态通知
			}
			
			out.println("success");
			
		}else {//验证失败
			out.println("fail");
		
			//调试用，写文本函数记录程序运行情况是否正常
			//String sWord = AlipaySignature.getSignCheckContentV1(params);
			//AlipayConfig.logResult(sWord);
		}
	}
	@RequestMapping("return_url")
	public String returnUrl(String out_trade_no){
		Integer rs = orderService.payForOrder(out_trade_no);
		if(rs>0){
			System.out.println("同步通知支付成功");
		}
		return "paysuccess";
	}
	
}
