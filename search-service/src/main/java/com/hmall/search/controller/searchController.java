package com.hmall.search.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.dto.ItemDTO;
import com.hmall.search.domain.po.ItemDoc;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class searchController {


    private final RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(HttpHost.create("192.168.101.128:9200"))
    );

    @ApiOperation(value = "搜索商品")
    @GetMapping("/{id}")
    public ItemDTO search(@PathVariable("id") Long id) throws IOException {
        //准备request对象
        GetRequest request = new GetRequest("items").id(id.toString());
        //发请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        //解析返回对象
        String json = response.getSourceAsString();
        ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);

        return BeanUtil.copyProperties(itemDoc, ItemDTO.class);
    }
}
