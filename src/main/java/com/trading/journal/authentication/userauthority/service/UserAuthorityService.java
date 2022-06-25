package com.trading.journal.authentication.userauthority.service;

import com.trading.journal.authentication.user.ApplicationUser;
import com.trading.journal.authentication.user.AuthoritiesChange;
import com.trading.journal.authentication.userauthority.UserAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public interface UserAuthorityService {

    List<UserAuthority> saveCommonUserAuthorities(ApplicationUser applicationUser);

    List<UserAuthority> saveAdminUserAuthorities(ApplicationUser applicationUser);

    List<UserAuthority> addAuthorities(ApplicationUser applicationUser, AuthoritiesChange authorities);

    List<UserAuthority> deleteAuthorities(ApplicationUser applicationUser, AuthoritiesChange authorities);

    List<UserAuthority> getByUserId(Long userId);

    List<SimpleGrantedAuthority> loadListAsSimpleGrantedAuthority(ApplicationUser applicationUser);
}