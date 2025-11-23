package com.ai.tutor.appointment.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class RedisUtils {

    private static StringRedisTemplate stringRedisTemplate;

    static {
        RedisUtils.stringRedisTemplate = SpringUtil.getBean(StringRedisTemplate.class);
    }


//    public static <T> List<T> mget(Collection<String> keys, Class<T> tClass) {
//        List<String> list = stringRedisTemplate.opsForValue().multiGet(keys);
//        if (Objects.isNull(list)) {
//            return new ArrayList<>();
//        }
//        return list.stream().map(o -> toBeanOrNull(o, tClass)).collect(Collectors.toList());
//    }

//    static <T> T toBeanOrNull(String json, Class<T> tClass) {
//        return json == null ? null : JsonUtils.toObj(json, tClass);
//    }

    private static final String LUA_INCR_EXPIRE =
            "local key,ttl=KEYS[1],ARGV[1] \n" +
                    " \n" +
                    "if redis.call('EXISTS',key)==0 then   \n" +
                    "  redis.call('SETEX',key,ttl,1) \n" +
                    "  return 1 \n" +
                    "else \n" +
                    "  return tonumber(redis.call('INCR',key)) \n" +
                    "end ";

    public static Long inc(String key, int time, TimeUnit unit) {
        RedisScript<Long> redisScript = new DefaultRedisScript<>(LUA_INCR_EXPIRE, Long.class);
        return stringRedisTemplate.execute(redisScript, Collections.singletonList(key), String.valueOf(unit.toSeconds(time)));
    }


}
