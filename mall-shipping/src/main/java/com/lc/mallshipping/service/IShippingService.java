package com.lc.mallshipping.service;


import com.lc.mallshipping.common.resp.ServerResponse;
import com.lc.mallshipping.entity.Shipping;

public interface IShippingService {
    ServerResponse add(Integer userId, Shipping shipping);

    ServerResponse del(Integer userId, Integer shippingId);

    ServerResponse update(Integer userId, Shipping shipping);

    ServerResponse select(Integer userId, Integer shippingId);

    ServerResponse list(Integer userId, int pageNum, int pageSize);

    ServerResponse getShippingById(Integer userId, Integer shippingId);
}
