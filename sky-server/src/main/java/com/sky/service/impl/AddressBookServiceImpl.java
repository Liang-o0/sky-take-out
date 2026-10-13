package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressBookServiceImpl implements AddressBookService {
    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 新增地址
     * @param addressBook
     */
    public void addNewAddress(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        //将当前新增地址设置为不是默认地址，设置默认地址通过前端按钮去选择
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);

    }


    /**
     * 查询登录用户所有地址
     * @return
     */
    public List<AddressBook> list() {
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());
        List<AddressBook> list = addressBookMapper.list(addressBook);

        return list ;
    }

    /**
     * 查询登录用户的默认地址
     * @return
     */
    public List<AddressBook> getDefaultAddress() {
        AddressBook addressBook = new AddressBook();
        addressBook.setIsDefault(1);
        addressBook.setUserId(BaseContext.getCurrentId());
        List<AddressBook> list = addressBookMapper.list(addressBook);
        return list;
    }

    /**
     * 修改地址
     * @param addressBook
     */
    public void update(AddressBook addressBook) {

        addressBookMapper.update(addressBook);

    }

    /**
     * 根据id删除地址
     * @param id
     */
    public void delete(Long id) {
        addressBookMapper.delete(id);
    }

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    public AddressBook getById(Long id) {
        AddressBook addressBook = addressBookMapper.getById(id);
        return addressBook;
    }

    /**
     * 设置默认地址
     * @param addressBook
     */
    public void setDefault(AddressBook addressBook) {
       //先把所有的地址都设置为非默认地址
        addressBook.setIsDefault(0);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.setIsDefaultByuserId(addressBook);
        //根据id设置默认地址,将当前地址修改为默认地址
        addressBook.setIsDefault(1);
        addressBookMapper.setDefaultAddress(addressBook);
    }
}
