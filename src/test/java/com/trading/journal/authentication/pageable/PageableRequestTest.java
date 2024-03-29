package com.trading.journal.authentication.pageable;

import com.trading.journal.authentication.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

class PageableRequestTest {

    @DisplayName("Load pageable request")
    @Test
    void pageable() {
        String[] sort = new String[]{"name", "asc", "id", "desc", "age", "asc"};
        PageableRequest pageableRequest = new PageableRequest(1, 20, sort, "something");

        Pageable pageable = pageableRequest.pageable();

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().get()).hasSize(3);
        assertThat(Objects.requireNonNull(pageable.getSort().getOrderFor("name")).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(Objects.requireNonNull(pageable.getSort().getOrderFor("id")).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(Objects.requireNonNull(pageable.getSort().getOrderFor("age")).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(pageableRequest.getFilter()).isEqualTo("something");
    }

    @DisplayName("Load pageable request even with extra spaces in sort")
    @Test
    void pageableSort() {
        String[] sort = new String[]{" name", "  asc", "id  ", "desc  ", "age ", " asc"};
        PageableRequest pageableRequest = new PageableRequest(1, 20, sort, "something");

        Pageable pageable = pageableRequest.pageable();

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().get()).hasSize(3);
        assertThat(Objects.requireNonNull(pageable.getSort().getOrderFor("name")).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(Objects.requireNonNull(pageable.getSort().getOrderFor("id")).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(Objects.requireNonNull(pageable.getSort().getOrderFor("age")).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(pageableRequest.getFilter()).isEqualTo("something");
    }

    @DisplayName("Load pageable throws exception when sort array is not odd")
    @Test
    void pageableSortException() {
        String[] sort = new String[]{"name", "desc", "age"};
        PageableRequest pageableRequest = new PageableRequest(1, 20, sort, "something");

        ApplicationException exception = assertThrows(ApplicationException.class, pageableRequest::pageable);
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getStatusText()).isEqualTo("Sort is invalid. It must be a pair of column and direction");
    }

    @DisplayName("Load pageable throws exception when direction is not valid (asc or desc)")
    @Test
    void pageableDirectionException() {
        String[] sort = new String[]{"name", "asc", "id", "abc", "age", "asc"};
        PageableRequest pageableRequest = new PageableRequest(1, 20, sort, "something");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, pageableRequest::pageable);
        assertThat(exception.getMessage()).contains("Invalid value 'abc' for orders given; Has to be either 'desc' or 'asc' (case insensitive)");
    }
}