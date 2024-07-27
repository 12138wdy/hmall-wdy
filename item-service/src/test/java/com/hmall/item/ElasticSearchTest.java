package com.hmall.item;


import cn.hutool.json.JSONUtil;
import com.hmall.item.domain.po.ItemDoc;
import org.apache.http.HttpHost;
import org.apache.lucene.search.TotalHits;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import java.util.Map;


//@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticSearchTest {

    private RestHighLevelClient client;


    @Test
    void testMatchAll() throws IOException {

        //1.准备request对象
        SearchRequest request = new SearchRequest("items");
        //2.构建查询条件
        request.source()
                .query(QueryBuilders.matchAllQuery());
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        parseResponse(response);

    }


    @Test
    void testSearch() throws IOException {

        //1.准备request对象
        SearchRequest request = new SearchRequest("items");
        //2.构建查询条件
        request.source()
                .query(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name","脱脂牛奶"))
                        .filter(QueryBuilders.termQuery("brand","德亚"))
                        .filter(QueryBuilders.rangeQuery("price").lt(20000))
                );
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        parseResponse(response);

    }


    @Test
    void testSortAndPage() throws IOException {

        int pageNo = 2,pageSize = 5;

        //1.准备request对象
        SearchRequest request = new SearchRequest("items");
        //2.构建查询条件
        request.source().query(QueryBuilders.matchAllQuery());
        //2.1.构造分页条件
        request.source().from((pageNo - 1) * pageSize).size(pageSize);
        //2.2.构造排序条件
        request.source().sort("sold", SortOrder.DESC);
        request.source().sort("price", SortOrder.ASC);

        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        parseResponse(response);
    }


    @Test
    void testHighLighter() throws IOException {

        //1.准备request对象
        SearchRequest request = new SearchRequest("items");
        //2.构建查询条件
        request.source().query(QueryBuilders.matchQuery("name","脱脂牛奶"));
        request.source().highlighter(SearchSourceBuilder.highlight().field("name"));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        parseResponse(response);

    }

    @Test
    void testAgg() throws IOException {

        //1.准备request对象
        SearchRequest request = new SearchRequest("items");
        //2.构建查询条件
        //2.1.分页
        request.source().size(0);
        //2.2.桶聚合
        String brandAggName = "brand_agg";
        request.source().aggregation(
                AggregationBuilders
                        .terms(brandAggName)
                        .field("brand")
                        .size(10)
        );
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        Aggregations aggregations = response.getAggregations();
        Terms termsBucket = aggregations.get(brandAggName);
        List<? extends Terms.Bucket> buckets = termsBucket.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            System.out.println("bucket.getKeyAsString() = " + bucket.getKeyAsString());
            System.out.println("bucket.getDocCount() = " + bucket.getDocCount());
        }
    }



    private static void parseResponse(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        //4.1.总条数
        TotalHits totalHits = searchHits.getTotalHits();
        System.out.println("totalHits = " + totalHits);

        //4.2.结果数组
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String source = hit.getSourceAsString();
            ItemDoc doc = JSONUtil.toBean(source, ItemDoc.class);

            //4.3.高亮结果处理
            Map<String, HighlightField> fieldMap = hit.getHighlightFields();
            if (fieldMap != null && !fieldMap.isEmpty()){
                HighlightField hf = fieldMap.get("name");
                String hfName = hf.getFragments()[0].string();
                doc.setName(hfName);
            }
            System.out.println("doc = " + doc);
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
