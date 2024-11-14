package com.github.ralgond.rls.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RuleMapper {

    @Select("SELECT id,key_type,method,path_pattern,burst,token_count,token_time_unit FROM rules WHERE deleted=false")
    List<Rule> getAllRules();
}
