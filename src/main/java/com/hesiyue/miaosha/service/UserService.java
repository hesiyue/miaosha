package com.hesiyue.miaosha.service;

import com.hesiyue.miaosha.dao.UserDao;
import com.hesiyue.miaosha.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    UserDao userDao;

    public User getById(int id){
        return userDao.geyById(id);
    }

    @Transactional
   public boolean tx(){
       User user = new User();
       user.setName("hexinyi");
       user.setId(2);
       userDao.insert(user);
       User use2 = new User();
       use2.setId(1);
       use2.setName("dengzhiyang");
       userDao.insert(use2);

       return true;
   }
}
