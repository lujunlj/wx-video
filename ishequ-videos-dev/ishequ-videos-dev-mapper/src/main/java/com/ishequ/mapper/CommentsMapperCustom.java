package com.ishequ.mapper;

import java.util.List;

import com.ishequ.pojo.Comments;
import com.ishequ.pojo.vo.CommentsVO;
import com.ishequ.utils.MyMapper;

public interface CommentsMapperCustom extends MyMapper<Comments> {
	
	public List<CommentsVO> queryComments(String videoId);
}