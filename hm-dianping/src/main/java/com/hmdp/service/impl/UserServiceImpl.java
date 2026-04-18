package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
@Autowired
private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendcode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)) {
           return Result.fail("手机号格式错误，发送短信验证码失败");
        }else{
            String s = RandomUtil.randomNumbers(6);
            stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, s, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
            log.debug("发送短信验证码成功，验证码：{}", s);
        }
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        Object code = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + loginForm.getPhone());
        String code1 = loginForm.getCode();
        if(code ==null||!code.equals(code1)){
            return Result.fail("验证码错误，登录失败");
        }
        User user  = query().eq("phone", loginForm.getPhone()).one();
        String phone = loginForm.getPhone();
        if(user == null){
            user =  createUserWithPhone(phone);
            }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        String token  = UUID.randomUUID().toString();
        Map<String, Object> map = BeanUtil.beanToMap(
                userDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString())
        );

        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token, map);

        return Result.ok(token);
    }

public User createUserWithPhone(String phone){
User user = new User();
user.setPhone(phone);
user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX +RandomUtil.randomString(10));
save(user);
return user;
}
}