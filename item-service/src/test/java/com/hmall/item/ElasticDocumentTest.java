package com.hmall.item;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.apache.ibatis.annotations.Update;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;


//@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticDocumentTest {

    private RestHighLevelClient client;
    //@Autowired
    private IItemService itemService;

    @Test
    void testCreateDocIndex() throws IOException {

        Item item = itemService.getById(317578L);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);

        itemDoc.setPrice(29900);

        //1.准备request对象
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        //2.准备请求参数
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        //3.发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetDocIndex() throws IOException {

        //1.准备request对象
        GetRequest request = new GetRequest("items", "317578");
        //2.发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        //3.处理返回的结果
        String json = response.getSourceAsString();
        ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
        System.out.println("itemDoc = " + itemDoc);
    }


    @Test
    void testDeleteDocIndex() throws IOException {

        //1.准备request对象
        DeleteRequest request = new DeleteRequest("items", "317578");
        //2.发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void testUpdateDocIndex() throws IOException {

        //1.准备request对象
        UpdateRequest request = new UpdateRequest("items", "317578");
        //2.准备数据
        request.doc(
                "price", 28900
        );
        //3.发送请求
        client.update(request, RequestOptions.DEFAULT);
    }


    @Test
    void testBulkDocIndex() throws IOException {

        int pageNo = 1;
        int pageSize = 1000;

        //1.从数据库中查询数据
        while (true) {
            Page<Item> page = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(Page.of(pageNo, pageSize));

            List<Item> items = page.getRecords();

            if (items == null || items.isEmpty()) {
                return;
            }

            //2.准备request对象
            BulkRequest request = new BulkRequest();
            //3.准备数据
            for (Item item : items) {
                request.add(new IndexRequest("items")
                        .id(item.getId().toString())
                        .source(JSONUtil.toJsonStr(BeanUtil.copyProperties(item,ItemDoc.class)), XContentType.JSON));
            }
            //4.发送请求
            client.bulk(request, RequestOptions.DEFAULT);
            pageNo++;
        }
    }


    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(
                RestClient.builder(
                        HttpHost.create("http://192.168.101.128:9200")
                ));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
    }

}
