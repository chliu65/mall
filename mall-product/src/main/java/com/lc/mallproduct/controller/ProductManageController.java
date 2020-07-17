package com.lc.mallproduct.controller;

import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;

import com.lc.mallproduct.common.exception.GlobalException;
import com.lc.mallproduct.common.keys.UserKey;
import com.lc.mallproduct.common.resp.ResponseEnum;
import com.lc.mallproduct.common.resp.ServerResponse;
import com.lc.mallproduct.common.utils.CookieUtil;
import com.lc.mallproduct.common.utils.JsonUtil;
import com.lc.mallproduct.common.utils.PropertiesUtil;
import com.lc.mallproduct.entity.Product;
import com.lc.mallproduct.entity.User;
import com.lc.mallproduct.service.IFileService;
import com.lc.mallproduct.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;


@RestController
@RequestMapping("/manage/product")
@Slf4j
public class ProductManageController {
    @Autowired
    private ProductService productService;
    @Autowired
    private IFileService fileService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 产品list
     */
    @RequestMapping("/list.do")
    public ServerResponse list(@RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                               @RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
        return productService.list(pageNum,pageSize);
    }

    /**
     * 产品搜索
     */
    @RequestMapping("search.do")
    public ServerResponse<PageInfo> search(String productName,
                                           Integer productId,
                                           @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                           @RequestParam(value = "pageSize",defaultValue = "10") int pageSize){

        return productService.search(productName,productId,pageNum,pageSize);
    }

    /**
     * 图片上传
     */
    @RequestMapping("upload.do")
    public ServerResponse upload(@RequestParam(value = "upload_file",required = false) MultipartFile file, HttpServletRequest request){
        String path = request.getSession().getServletContext().getRealPath("upload");
        String targetFileName = fileService.upload(file,path);
        String url = "http://img.oursnail.cn/"+targetFileName;

        log.info("【上传的图片路径为：{}】",url);

        Map fileMap = Maps.newHashMap();
        fileMap.put("uri",targetFileName);
        fileMap.put("url",url);
        log.info("【返回数据为:{}】",fileMap);
        return ServerResponse.createBySuccess(fileMap);
    }

    /**
     * 产品详情
     */
    @RequestMapping("detail.do")
    public ServerResponse detail(Integer productId){
        return productService.detail(productId);
    }

    /**
     * 产品上下架
     */
    @RequestMapping("set_sale_status.do")
    public ServerResponse<String> set_sale_status(Integer productId,Integer status){
        return productService.set_sale_status(productId,status);
    }

    /**
     * 新增OR更新产品
     */
    @RequestMapping("save.do")
    public ServerResponse<String> productSave(Product product){
        return productService.saveOrUpdateProduct(product);
    }

    /**
     * 富文本上传图片//???/未区分管理员
     * 由于这里如果没有管理员权限，需要回复特定形式的信息，所以校验单独放在这里，zuul过滤器对其直接放过
     */
    @RequestMapping("richtext_img_upload.do")
    public Map richtextImgUpload(@RequestParam(value = "upload_file",required = false) MultipartFile file, HttpServletRequest request, HttpServletResponse response) {
        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isEmpty(loginToken)){
            throw new GlobalException(ResponseEnum.LOGIN_EXPIRED);
        }
        //2.从redis中获取用户信息
        String userStr = stringRedisTemplate.opsForValue().get(new UserKey(loginToken).getPrefix());
        if(userStr == null){
            throw new GlobalException(ResponseEnum.LOGIN_EXPIRED);
        }

        User user = JsonUtil.Str2Obj(userStr,User.class);
        Map resultMap = Maps.newHashMap();
        if(user == null){
            resultMap.put("success",false);
            resultMap.put("msg","请登录管理员");
            return resultMap;
        }

        String path = request.getSession().getServletContext().getRealPath("upload");
        String targetFileName = fileService.upload(file, path);
        if (StringUtils.isBlank(targetFileName)) {
            resultMap.put("success", false);
            resultMap.put("msg", "上传失败");
            return resultMap;
        }
        String url = PropertiesUtil.getProperty("ftp.server.http.prefix","http://img.oursnail.cn/")+targetFileName;
        resultMap.put("success", true);
        resultMap.put("msg", "上传成功");
        resultMap.put("file_path", url);
        log.info("【返回数据为:{}】",resultMap);
        response.addHeader("Access-Control-Allow-Headers", "X-File-Name");
        return resultMap;
    }

}
