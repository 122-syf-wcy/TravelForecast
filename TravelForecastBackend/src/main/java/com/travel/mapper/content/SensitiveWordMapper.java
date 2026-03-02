package com.travel.mapper.content;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.entity.content.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 敏感词Mapper
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
    
    /**
     * 获取所有启用的敏感词
     */
    @Select("SELECT word, action, replacement FROM sensitive_words WHERE status = 'active' ORDER BY level DESC, LENGTH(word) DESC")
    List<SensitiveWord> getAllActiveSensitiveWords();
}

