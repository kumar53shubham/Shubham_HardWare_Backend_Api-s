package com.shubham.hardware.services;

import com.shubham.hardware.dtos.PageableResponse;
import com.shubham.hardware.dtos.ProductDto;

import java.util.List;

public interface ProductService {

    //    create
    ProductDto create(ProductDto productDto);

    //    update
    ProductDto update(ProductDto productDto,String productId);

    //    delete
    void delete(String productId);

    //    get one
    ProductDto getProductById(String productId);

    //    get all
    PageableResponse<ProductDto> getAllProduct(int pageNumber, int pageSize, String sortBy, String sortDir);

    //    get all : Live
    PageableResponse<ProductDto> getAllLive(int pageNumber, int pageSize, String sortBy, String sortDir);

    //    search
    PageableResponse<ProductDto> getAllProductByTitle(String keyword,int pageNumber, int pageSize, String sortBy, String sortDir);
}
