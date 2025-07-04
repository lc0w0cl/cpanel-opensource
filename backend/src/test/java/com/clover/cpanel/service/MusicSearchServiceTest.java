package com.clover.cpanel.service;

import com.clover.cpanel.dto.MusicSearchRequestDTO;
import com.clover.cpanel.dto.MusicSearchResultDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 音乐搜索服务测试
 */
@SpringBootTest
public class MusicSearchServiceTest {

    @Test
    public void testParseBilibiliVideoItem() {
        // 模拟您提供的HTML结构
        String html = """
            <div class="bili-video-card" data-v-2c40a088 data-v-37adbc26>
                <div class="bili-video-card__wrap" data-v-37adbc26>
                    <a href="//www.bilibili.com/video/BV1aiT7zTEWD/" class="" target="_blank" data-v-37adbc26>
                        <div class="bili-video-card__image" data-v-37adbc26>
                            <div class="bili-video-card__image--wrap" data-v-37adbc26>
                                <picture class="v-img bili-video-card__cover" data-v-37adbc26>
                                    <img src="//i2.hdslb.com/bfs/archive/b3858b087e18557ac83e27fcbff1d91355e1a92c.jpg@672w_378h_1c_!web-search-common-cover" alt="《稻香》周杰伦 完整版无损音质！" loading="lazy">
                                </picture>
                            </div>
                            <div class="bili-video-card__mask" data-v-37adbc26>
                                <div class="bili-video-card__stats" data-v-37adbc26>
                                    <div class="bili-video-card__stats--left" data-v-37adbc26>
                                        <span class="bili-video-card__stats--item" data-v-37adbc26>
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="#ffffff" class="bili-video-card__stats--icon" data-v-37adbc26></svg>
                                            <span data-v-37adbc26>5.8万</span>
                                        </span>
                                        <span class="bili-video-card__stats--item" data-v-37adbc26>
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" class="bili-video-card__stats--icon" data-v-37adbc26></svg>
                                            <span data-v-37adbc26>120</span>
                                        </span>
                                    </div>
                                    <span class="bili-video-card__stats__duration" data-v-37adbc26>03:46</span>
                                </div>
                            </div>
                        </div>
                    </a>
                    <div class="bili-video-card__info" data-v-37adbc26>
                        <div class="bili-video-card__info--right" data-v-37adbc26>
                            <a href="//www.bilibili.com/video/BV1aiT7zTEWD/" target="_blank" data-v-37adbc26>
                                <h3 class="bili-video-card__info--tit" title="《稻香》周杰伦 完整版无损音质！" data-v-37adbc26>《<em class="keyword">稻香</em>》周杰伦 完整版无损音质！</h3>
                            </a>
                            <div class="bili-video-card__info--bottom" data-v-37adbc26>
                                <a class="bili-video-card__info--owner" href="//space.bilibili.com/3493131953637653" target="_blank" data-v-37adbc26>
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" class="bili-video-card__info--author-ico mr_2" data-v-37adbc26></svg>
                                    <span class="bili-video-card__info--author" data-v-37adbc26>芒果味杨枝甘露</span>
                                    <span class="bili-video-card__info--date" data-v-37adbc26> · 06-04</span>
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            """;

        Document doc = Jsoup.parse(html);
        Element videoCard = doc.selectFirst(".bili-video-card");

        // 测试解析逻辑
        if (videoCard != null) {
            // 测试标题提取
            Element titleElement = videoCard.selectFirst(".bili-video-card__info--tit");
            System.out.println("标题: " + (titleElement != null ? titleElement.text() : "未找到"));

            // 测试链接提取
            Element linkElement = videoCard.selectFirst("a[href*='/video/']");
            System.out.println("链接: " + (linkElement != null ? linkElement.attr("href") : "未找到"));

            // 测试UP主提取
            Element uploaderElement = videoCard.selectFirst(".bili-video-card__info--author");
            System.out.println("UP主: " + (uploaderElement != null ? uploaderElement.text() : "未找到"));

            // 测试时长提取
            Element durationElement = videoCard.selectFirst(".bili-video-card__stats__duration");
            System.out.println("时长: " + (durationElement != null ? durationElement.text() : "未找到"));

            // 测试缩略图提取
            Element imgElement = videoCard.selectFirst(".bili-video-card__cover img");
            System.out.println("缩略图: " + (imgElement != null ? imgElement.attr("src") : "未找到"));

            // 测试播放量提取
            Element playElement = videoCard.selectFirst(".bili-video-card__stats--item span:last-child");
            System.out.println("播放量: " + (playElement != null ? playElement.text() : "未找到"));

            // 测试发布时间提取
            Element timeElement = videoCard.selectFirst(".bili-video-card__info--date");
            System.out.println("发布时间: " + (timeElement != null ? timeElement.text() : "未找到"));
        }
    }

    @Test
    public void testSearchMusic() {
        MusicSearchService service = new MusicSearchService();
        
        MusicSearchRequestDTO request = new MusicSearchRequestDTO();
        request.setQuery("稻香");
        request.setSearchType("keyword");
        request.setPlatform("bilibili");
        request.setPage(1);
        request.setPageSize(5);

        try {
            List<MusicSearchResultDTO> results = service.searchMusic(request);
            System.out.println("搜索结果数量: " + results.size());
            
            for (MusicSearchResultDTO result : results) {
                System.out.println("标题: " + result.getTitle());
                System.out.println("UP主: " + result.getArtist());
                System.out.println("时长: " + result.getDuration());
                System.out.println("链接: " + result.getUrl());
                System.out.println("---");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
