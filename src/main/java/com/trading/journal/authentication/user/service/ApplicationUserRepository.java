package com.trading.journal.authentication.user.service;

import com.trading.journal.authentication.user.ApplicationUser;
import com.trading.journal.authentication.user.UserInfo;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ApplicationUserRepository extends CrudRepository<ApplicationUser, Long> {

    Integer countByUserName(String userName);

    Integer countByEmail(String email);

    ApplicationUser findByEmail(String email);

    @Query("select id, userName, firstName, lastName, email, enabled, verified, createdAt from Users where userName = :userName")
    UserInfo findByUserName(String userName);

    @Query("SELECT COUNT(Users.id) FROM Users inner join UserAuthorities where Users.id = UserAuthorities.userId and UserAuthorities.name in (:roles)")
    Integer countAdmins(List<String> roles);
}