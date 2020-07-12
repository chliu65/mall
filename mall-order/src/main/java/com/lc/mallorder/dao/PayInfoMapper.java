package com.lc.mallorder.dao;


import com.lc.mallorder.entity.PayInfo;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface PayInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(PayInfo record);

    int insertSelective(PayInfo record);

    PayInfo selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(PayInfo record);

    int updateByPrimaryKey(PayInfo record);
}
