package com.sky.mapper;

import com.sky.entity.AddressBook;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AddressBookMapper {

    /**
     * 新增地址
     * @param addressBook
     */
    void insert(AddressBook addressBook);

    /**
     * 根据用户id查询所有地址&查询用户的默认地址
     * @param addressBook
     * @return
     */
    List<AddressBook> list(AddressBook addressBook);

    /**
     * 修改地址
     */
    void update(AddressBook addressBook);

    /**
     * 根据id删除地址
     * @param id
     */
    //delete语句后面不用加*
    //@Delete("delete * from address_book where id = #{id}")
    @Delete("delete from address_book where id = #{id}")
    void delete(Long id);

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    @Select("select * from address_book where id = #{id}")
    AddressBook getById(Long id);

    /**
     * 设置默认地址
     * @param addressBook
     */
    @Update("update address_book set is_default = #{isDefault} where id = #{id}")
    void setDefaultAddress(AddressBook addressBook);

    /**
     * 将用户所有地址都设置为非默认地址
     * @param addressBook
     */
    @Update("update address_book set is_default = #{isDefault} where user_id = #{userId}")
    void setIsDefaultByuserId(AddressBook addressBook);
}
