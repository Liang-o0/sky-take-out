package com.sky.service;

import com.sky.entity.AddressBook;

import java.util.List;

public interface AddressBookService {

    /**
     * 新增地址
     * @param addressBook
     */
    void addNewAddress(AddressBook addressBook);


    /**
     * 查询登录用户所有地址
     * @return
     */
    List<AddressBook> list();

    /**
     * 查询登录用户的默认地址
     * @return
     */
    List<AddressBook> getDefaultAddress();

    /**
     * 修改地址
     * @param addressBook
     */
    void update(AddressBook addressBook);

    /**
     * 根据id删除地址
     * @param id
     */
    void delete(Long id);

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    AddressBook getById(Long id);

    /**
     * 设置默认地址
     * @param addressBook
     */
    void setDefault(AddressBook addressBook);
}
