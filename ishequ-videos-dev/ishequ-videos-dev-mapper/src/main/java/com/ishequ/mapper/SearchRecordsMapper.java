package com.ishequ.mapper;

import java.util.List;

import com.ishequ.pojo.SearchRecords;
import com.ishequ.utils.MyMapper;

public interface SearchRecordsMapper extends MyMapper<SearchRecords> {
	
	public List<String> getHotwords();
}