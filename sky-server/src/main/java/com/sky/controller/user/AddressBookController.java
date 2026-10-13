package com.sky.controller.user;

import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/user/addressBook")
@Api("地址簿相关接口")
public class AddressBookController {
    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增地址
     * @param addressBook
     * @return
     */
    @PostMapping
    @ApiOperation("新增地址接口")
    public Result add(@RequestBody AddressBook addressBook){

        addressBookService.addNewAddress(addressBook);
        return Result.success();
    }

    /**
     * 查询登录用户所有地址
     * @return
     */
    @ApiOperation("查询登录用户所有地址")
    @GetMapping("/list")
    public Result<List<AddressBook>> list(){

        List<AddressBook> List = addressBookService.list();
        return Result.success(List);

    }

    /**
     * 查询登录用户的默认地址
     * @return
     */
    @ApiOperation("查询登录用户的默认地址")
    @GetMapping("/default")
    public Result<AddressBook> defaultList(){
        List<AddressBook> list = addressBookService.getDefaultAddress();
        //如果没有查到的清空
        if (list != null && list.size() > 0){
            //一个用户只有一个默认地址，函数返回不是一个List集合
            return Result.success(list.get(0));
        }
        return Result.error("没有找到默认地址");
    }

    /**
     * 修改地址
     * @param addressBook
     * @return
     */
    @ApiOperation("修改地址接口")
    @PutMapping
    public Result update(@RequestBody AddressBook addressBook){
        addressBookService.update(addressBook);
        return Result.success();
    }

    /**
     * 根据id删除地址
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("根据id删除地址")
    public Result delete(Long id){
        addressBookService.delete(id);

        return Result.success();
    }

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    @ApiOperation("根据id查询地址")
    @GetMapping("/{id}")
    public Result<AddressBook> getAddressById(@PathVariable Long id ){
        AddressBook addressBook = addressBookService.getById(id);

        return Result.success(addressBook);
    }

    /**
     * 设置默认地址
     * @return
     */
    @ApiOperation("设置默认地址")
    @PutMapping("/default")
    public Result setDefault(@RequestBody AddressBook addressBook){
        addressBookService.setDefault(addressBook);

        return Result.success();
    }
}
