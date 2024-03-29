package com.trading.journal.authentication.userauthority.service.impl;

import com.trading.journal.authentication.authority.Authority;
import com.trading.journal.authentication.authority.AuthorityCategory;
import com.trading.journal.authentication.authority.service.AuthorityService;
import com.trading.journal.authentication.user.AuthoritiesChange;
import com.trading.journal.authentication.user.User;
import com.trading.journal.authentication.userauthority.UserAuthority;
import com.trading.journal.authentication.userauthority.UserAuthorityRepository;
import com.trading.journal.authentication.userauthority.service.UserAuthorityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAuthorityServiceImpl implements UserAuthorityService {

    private final UserAuthorityRepository userAuthorityRepository;

    private final AuthorityService authorityService;

    @Override
    public List<UserAuthority> saveCommonUserAuthorities(User user) {
        return authorityService.getAuthoritiesByCategory(AuthorityCategory.COMMON_USER)
                .stream()
                .map(authority -> new UserAuthority(user, authority))
                .map(userAuthorityRepository::save)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserAuthority> saveAdminUserAuthorities(User user) {
        return authorityService.getAll()
                .stream()
                .map(authority -> new UserAuthority(user, authority))
                .map(userAuthorityRepository::save)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserAuthority> saveOrganisationAdminUserAuthorities(User user) {
        return authorityService.getAuthoritiesByCategory(AuthorityCategory.ORGANISATION)
                .stream()
                .map(authority -> new UserAuthority(user, authority))
                .map(userAuthorityRepository::save)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserAuthority> addAuthorities(User user, AuthoritiesChange authoritiesChange) {
        List<Authority> authorities = authoritiesChange.authorities().stream()
                .map(authorityService::getByName)
                .filter(Optional::isPresent)
                .map(Optional::get).toList();

        List<UserAuthority> userAuthoritiesToAdd = authorities.stream()
                .filter(filterOutEqualAuthorities(user))
                .map(authority -> new UserAuthority(user, authority))
                .toList();

        userAuthoritiesToAdd.forEach(userAuthorityRepository::save);
        return userAuthorityRepository.findByUserId(user.getId());
    }

    @Override
    public List<UserAuthority> deleteAuthorities(User user, AuthoritiesChange authoritiesChange) {
        List<Authority> authorities = authoritiesChange.authorities().stream()
                .map(authorityService::getByName)
                .filter(Optional::isPresent)
                .map(Optional::get).toList();

        List<UserAuthority> userAuthoritiesToRemove = user.getAuthorities()
                .stream()
                .filter(filterUserRolesToRemove(authorities)).toList();

        userAuthoritiesToRemove.forEach(userAuthorityRepository::delete);
        return userAuthorityRepository.findByUserId(user.getId());
    }

    private Predicate<Authority> filterOutEqualAuthorities(User user) {
        return authority -> user
                .getAuthorities()
                .stream()
                .noneMatch(userAuthority -> userAuthority.getAuthority().getName().equals(authority.getName())
                        && Objects.equals(userAuthority.getAuthority().getId(), authority.getId()));
    }

    private Predicate<UserAuthority> filterUserRolesToRemove(List<Authority> authorities) {
        return userAuthority -> authorities.stream()
                .anyMatch(authority -> userAuthority.getAuthority().getName().equals(authority.getName())
                        && Objects.equals(userAuthority.getAuthority().getId(), authority.getId())
                );
    }
}
