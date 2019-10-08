package com.ishequ.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ishequ.service.UserService;
import com.ishequ.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.ishequ.pojo.Users;
import com.ishequ.pojo.vo.UsersVO;

import javax.servlet.http.HttpServletRequest;

@RestController
public class WXLoginController extends BasicController {

	@Autowired
	private UserService userService;
	
	public UsersVO setUserRedisSessionToken(Users userModel) {
		String uniqueToken = UUID.randomUUID().toString();
//		redis.set(USER_REDIS_SESSION + ":" + userModel.getId(), uniqueToken, 1000 * 60 * 30);
		redisTemplate.boundValueOps(USER_REDIS_SESSION + ":" + userModel.getId()).set(uniqueToken,1000 * 60 * 30);
		UsersVO userVO = new UsersVO();
		BeanUtils.copyProperties(userModel, userVO);
		userVO.setUserToken(uniqueToken);
		return userVO;
	}
	
	@PostMapping("/wxLogin")
	public ISheQuJSONResult wxLogin(@RequestParam(value = "code") String code,@RequestParam(value = "avatarurl")String avatarurl,@RequestParam(value = "nickname")String nickname) throws Exception {

		System.out.println(code);
		
//		https://api.weixin.qq.com/sns/jscode2session?appid=APPID&secret=SECRET&js_code=JSCODE&grant_type=authorization_code
		
		String url = "https://api.weixin.qq.com/sns/jscode2session";
		Map<String, String> param = new HashMap<>();
		param.put("appid", "wx5ee7115a0e83198b");
		param.put("secret", "5eb799946720c96b13e59cd622bcb93e");
		param.put("js_code", code);
		param.put("grant_type", "authorization_code");
		
		String wxResult = HttpClientUtil.doGet(url, param);
		System.out.println(wxResult);
		
		WXSessionModel model = JsonUtils.jsonToPojo(wxResult, WXSessionModel.class);
			

		String openid = model.getOpenid();
		if(StringUtils.isNotBlank(openid)){
			Users users = userService.queryUserInfoByOpenId(openid);
			if (users != null){
				UsersVO userVO = new UsersVO();
				BeanUtils.copyProperties(users, userVO);
				userVO.setUserToken(model.getSession_key());
				redisTemplate.boundValueOps(USER_REDIS_SESSION + ":" + users.getId()).set(model.getSession_key(),1000 * 60 * 30);
				return ISheQuJSONResult.ok(userVO);
			}else{//第一微信登录，为绑定账号
				if (StringUtils.isBlank(openid)) {
					return ISheQuJSONResult.errorMsg("用户openid不能为空...");
				}
				// 文件保存的命名空间
				String fileSpace = FILE_SPACE;
				// 保存到数据库中的相对路径
				String uploadPathDB = "/" + openid + "/face";
				String fileName = UUID.randomUUID().toString();
				String dirpath =  fileSpace + uploadPathDB;
				File file = new File(dirpath);
				if(!file.exists()){file.mkdirs();};
				// 文件上传的最终保存路径
				String finalFacePath = dirpath + "/" + fileName;
				// 设置数据库保存的路径
				uploadPathDB += ("/" + fileName);
				FileUtils.downLoadUrlImageAndSave(avatarurl,finalFacePath);
				Users user = new Users();
				user.setNickname(nickname);
				user.setFansCounts(0);
				user.setReceiveLikeCounts(0);
				user.setFollowCounts(0);
				user.setOpenid(openid);
				user.setFaceImage(uploadPathDB);
				userService.saveUser(user);
				UsersVO userVO = new UsersVO();
				BeanUtils.copyProperties(user, userVO);
				userVO.setUserToken(model.getSession_key());
				redisTemplate.boundValueOps(USER_REDIS_SESSION + ":" + user.getId()).set(model.getSession_key(),1000 * 60 * 30);
				return ISheQuJSONResult.ok(userVO);
			}
		}
		return ISheQuJSONResult.errorMsg("微信登录失败!");

	}



}
